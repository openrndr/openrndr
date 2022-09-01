package org.openrndr.shape

import kotlin.math.min

class Path1D(val segments: List<Segment1D>, val closed: Boolean) {
    fun position(ut: Double): Double {
        val t = ut.coerceIn(0.0, 1.0)
        val segment = (t * segments.size).toInt()
        val segmentOffset = (t * segments.size) - segment
        return segments[min(segments.size - 1, segment)].position(segmentOffset)
    }

    fun adaptivePositions(distanceTolerance: Double = 25.0): List<Double> {
        val adaptivePoints = mutableListOf<Double>()
        var last: Double? = null
        for (segment in this.segments) {
            val samples = segment.adaptivePositions(distanceTolerance)
            if (samples.isNotEmpty()) {
                val r = samples[0]
                if (last == null || last.minus(r) > 0.001) {
                    adaptivePoints.add(r)
                }
                for (i in 1 until samples.size) {
                    adaptivePoints.add(samples[i])
                    last = samples[i]
                }
            }
        }
        return adaptivePoints
    }

    fun adaptivePositionsWithT(distanceTolerance: Double = 25.0): List<Pair<Double, Double>> {
        val adaptivePoints = mutableListOf<Pair<Double, Double>>()
        var last: Double? = null
        for ((index, segment) in this.segments.withIndex()) {
            val samples = segment.adaptivePositionsWithT(distanceTolerance)
            if (samples.isNotEmpty()) {
                val r = samples[0]
                adaptivePoints.add(r)
                for (i in 1 until samples.size) {
                    adaptivePoints.add(samples[i])
                }
            }
        }
        return adaptivePoints
    }
}