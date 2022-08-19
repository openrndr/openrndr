package org.openrndr.shape

import org.openrndr.math.EuclideanVector
import org.openrndr.math.LinearType
import kotlin.math.roundToInt

/** Returns specified amount of points of equal distance from each other. */
fun <T> sampleEquidistant(
    segments: List<T>,
    count: Int
): List<T> where T : LinearType<T>, T : EuclideanVector<T> {
    val result = mutableListOf<T>()
    if (segments.isEmpty()) {
        return result
    }
    var totalLength = 0.0

    for (i in 0 until segments.size - 1) {
        totalLength += segments[i].minus(segments[i + 1]).length
    }

    val spacing = totalLength / (count - 1)

    var remaining = 0.0
    var cursor: T
    if (count > 0) {
        result.add(segments[0])
    }

    for (i in 0 until segments.size - 1) {
        val direction = segments[i + 1] - segments[i]
        val segmentLength = direction.length
        if (segmentLength + remaining < spacing) {
            remaining += segmentLength
        } else {
            val skipLength = (spacing - remaining).coerceAtLeast(0.0)
            val pointsFromSegment = 1 + ((segmentLength - skipLength) / spacing).roundToInt()

            // note: sometimes pointsFromSegments overestimates (due to roundToInt) the number of points
            // that should be sampled from the current segment by 1 at most.

            val skipT = skipLength / segmentLength
            val spaceT = spacing / segmentLength
            val start = segments[i]
            var t = skipT
            for (n in 0 until pointsFromSegment) {
                if (t < 1.0 + 1.0E-6) {
                    cursor = start + direction * t
                    t += spaceT
                    result.add(cursor)
                }
            }
            remaining = (1.0 - (t - spaceT)) * segmentLength
        }
    }
    if (count >= 2) {
        if (result.size == count) {
            result[result.lastIndex] = segments.last()
        } else {
            result.add(segments.last())
        }
    }
    return result
}