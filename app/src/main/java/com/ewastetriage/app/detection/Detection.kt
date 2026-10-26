package com.ewastetriage.app.detection

data class Detection(
    val classIndex: Int,
    val className: String,
    val confidence: Float,
    val x1: Float,  // normalized 0-1
    val y1: Float,
    val x2: Float,
    val y2: Float
)
