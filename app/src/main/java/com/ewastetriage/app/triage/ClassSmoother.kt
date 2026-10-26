package com.ewastetriage.app.triage

import java.util.ArrayDeque

/**
 * Port of Python smoothing.py.
 * A class is confirmed only if it appeared in at least [needed] of the last [window] frames.
 * Its smoothed confidence is the average over those frames.
 */
class ClassSmoother(private val window: Int = 5, private val needed: Int = 3) {
    private val history = ArrayDeque<Map<String, Float>>()

    fun update(bestConfByClass: Map<String, Float>) {
        history.addLast(bestConfByClass)
        while (history.size > window) history.pollFirst()
    }

    fun confirmed(): Map<String, Float> {
        val allNames = history.flatMap { it.keys }.toSet()
        val result = mutableMapOf<String, Float>()
        for (name in allNames) {
            val confs = history.mapNotNull { it[name] }
            if (confs.size >= needed) {
                result[name] = confs.average().toFloat()
            }
        }
        return result
    }

    fun reset() = history.clear()
}
