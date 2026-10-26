package com.ewastetriage.app.detection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class Detector(context: Context, modelFileName: String, classesFileName: String) {
    private val interpreter: Interpreter
    private val classNames: List<String>
    private val postprocessor: Postprocessor
    private val isTransposed: Boolean // true if shape is [1, 8400, 10], false if [1, 10, 8400]

    private val inputSize = 640

    init {
        // Load class names
        classNames = context.assets.open(classesFileName)
            .bufferedReader()
            .readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        // Load model
        val modelBuffer = loadModelBuffer(context, modelFileName)
        val options = Interpreter.Options().apply {
            setNumThreads(4)
        }
        interpreter = Interpreter(modelBuffer, options)

        val outShape = interpreter.getOutputTensor(0).shape()
        Log.i("Detector", "Output tensor shape: ${outShape.contentToString()}")
        isTransposed = (outShape.size >= 3 && outShape[1] == 8400 && outShape[2] == 10)

        postprocessor = Postprocessor(classNames)
    }

    private fun loadModelBuffer(context: Context, modelFileName: String): ByteBuffer {
        return try {
            val afd = context.assets.openFd(modelFileName)
            val fis = FileInputStream(afd.fileDescriptor)
            fis.channel.map(FileChannel.MapMode.READ_ONLY, afd.startOffset, afd.declaredLength)
        } catch (e: Exception) {
            Log.w("Detector", "openFd failed, falling back to direct byte read: ${e.message}")
            val bytes = context.assets.open(modelFileName).use { it.readBytes() }
            val buffer = ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder())
            buffer.put(bytes)
            buffer.rewind()
            buffer
        }
    }

    /**
     * Run inference on a bitmap. The bitmap will be scaled/letterboxed to 640x640.
     * Returns a list of detections in normalized coordinates (0-1).
     */
    fun detect(bitmap: Bitmap): List<Detection> {
        val letterboxed = letterbox(bitmap)
        val inputBuffer = bitmapToBuffer(letterboxed)

        // Flatten to (10 * 8400) in feature-major order: flat[f * 8400 + a]
        val flat = FloatArray(10 * 8400)

        if (isTransposed) {
            // Model shape is (1, 8400, 10)
            val outputArray = Array(1) { Array(8400) { FloatArray(10) } }
            interpreter.run(inputBuffer, outputArray)
            for (a in 0 until 8400) {
                for (f in 0 until 10) {
                    flat[f * 8400 + a] = outputArray[0][a][f]
                }
            }
        } else {
            // Model shape is (1, 10, 8400)
            val outputArray = Array(1) { Array(10) { FloatArray(8400) } }
            interpreter.run(inputBuffer, outputArray)
            for (f in 0 until 10) {
                for (a in 0 until 8400) {
                    flat[f * 8400 + a] = outputArray[0][f][a]
                }
            }
        }

        return postprocessor.process(flat)
    }

    /**
     * Letterbox: scale the bitmap to fit 640x640 with gray padding (value 114/255).
     * Returns a 640x640 bitmap.
     */
    private fun letterbox(src: Bitmap): Bitmap {
        val scale = minOf(inputSize.toFloat() / src.width, inputSize.toFloat() / src.height)
        val newW = (src.width * scale).toInt()
        val newH = (src.height * scale).toInt()

        val result = Bitmap.createBitmap(inputSize, inputSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        // Gray background (114, 114, 114) matching Ultralytics default
        canvas.drawColor(Color.rgb(114, 114, 114))
        val scaled = Bitmap.createScaledBitmap(src, newW, newH, true)
        val left = (inputSize - newW) / 2f
        val top = (inputSize - newH) / 2f
        canvas.drawBitmap(scaled, left, top, null)
        return result
    }

    /**
     * Convert bitmap to channel-first float32 ByteBuffer (1, 3, 640, 640), values 0-1.
     */
    private fun bitmapToBuffer(bitmap: Bitmap): ByteBuffer {
        val pixels = IntArray(inputSize * inputSize)
        bitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)

        // Channel-first: (1, 3, H, W)
        val buffer = ByteBuffer.allocateDirect(1 * 3 * inputSize * inputSize * 4)
        buffer.order(ByteOrder.nativeOrder())

        // Red channel
        for (p in pixels) buffer.putFloat(((p shr 16) and 0xFF) / 255f)
        // Green channel
        for (p in pixels) buffer.putFloat(((p shr 8) and 0xFF) / 255f)
        // Blue channel
        for (p in pixels) buffer.putFloat((p and 0xFF) / 255f)

        buffer.rewind()
        return buffer
    }

    fun close() {
        interpreter.close()
    }
}
