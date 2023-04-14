package org.openrndr.shape

import org.openrndr.math.EuclideanVector
import org.openrndr.math.LinearType
import org.openrndr.utils.tuples.Quadruple

import kotlin.jvm.JvmName
import kotlin.math.roundToInt

@JvmName("proportionalizeWithoutT")
fun <T : EuclideanVector<T>> proportionalize(input: List<T>): List<Pair<T, Double>> {
    return proportionalize(input.map { Pair(it, 0.0) })
}

fun <T : EuclideanVector<T>> proportionalize(input: List<Pair<T, Double>>): List<Pair<T, Double>> {
    val lengths = mutableListOf<Double>()
    var sum = 0.0
    for (i in input.indices) {
        if (i > 0) {
            sum += input[i].first.distanceTo(input[i - 1].first)
        }
        lengths.add(sum)
    }
    for (i in lengths.indices) {
        lengths[i] /= sum
    }

    return (lengths.indices).map { i ->
        Pair(input[i].first, lengths[i])
    }
}

@JvmName("resampleList")
fun <T : LinearType<T>> resample(input: List<Pair<T, Double>>, ts: List<Double>): List<Pair<T, Double>> {
    val result = mutableListOf<Pair<T, Double>>()
    var index = 0
    for (t in ts) {
        while (index < input.size && input[index].second < t) {
            index++
        }
        val i1 = index.coerceIn(input.indices)
        val i0 = (index - 1).coerceIn(input.indices)

        val v: T = if (i0 == i1) {
            input[i0].first
        } else {
            val t0 = t - input[i0].second
            val dt = input[i1].second - input[i0].second
            val f = t0 / dt
            val v0 = input[i0].first
            val v1 = input[i1].first
            v0 * (1.0 - f) + v1 * f
        }
        result.add(Pair(v, t))
    }
    return result
}

private fun gatherTs(vararg input: List<Pair<LinearType<*>, Double>>): List<Double> {
    return input.flatMap { l -> l.map { it.second } }.distinct().sorted()
}

fun <T1 : LinearType<T1>, T2 : LinearType<T2>> resample(
    input1: List<Pair<T1, Double>>,
    input2: List<Pair<T2, Double>>
): List<Triple<T1, T2, Double>> {
    val ts = gatherTs(input1, input2)

    val resampled1 = resample(input1, ts)
    val resampled2 = resample(input2, ts)

    val result = mutableListOf<Triple<T1, T2, Double>>()
    for (i in ts.indices) {
        result.add(Triple(resampled1[i].first, resampled2[i].first, ts[i]))
    }
    return result
}

fun <T1 : LinearType<T1>, T2 : LinearType<T2>, T3 : LinearType<T3>> resample(
    input1: List<Pair<T1, Double>>,
    input2: List<Pair<T2, Double>>,
    input3: List<Pair<T3, Double>>
): List<Quadruple<T1, T2, T3, Double>> {
    val ts = gatherTs(input1, input2, input3)

    val resampled1 = resample(input1, ts)
    val resampled2 = resample(input2, ts)
    val resampled3 = resample(input3, ts)

    val result = mutableListOf<Quadruple<T1, T2, T3, Double>>()
    for (i in ts.indices) {
        result.add(Quadruple(resampled1[i].first, resampled2[i].first, resampled3[i].first, ts[i]))
    }
    return result
}


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

/** Returns specified amount of points of equal distance from each other. */
fun <T> sampleEquidistantWithT(
    segments: List<Pair<T, Double>>,
    count: Int
): List<Pair<T, Double>> where T : LinearType<T>, T : EuclideanVector<T> {
    val result = mutableListOf<Pair<T, Double>>()
    if (segments.isEmpty()) {
        return result
    }
    var totalLength = 0.0

    for (i in 0 until segments.size - 1) {
        totalLength += (segments[i].first - segments[i + 1].first).length
    }

    val spacing = totalLength / (count - 1)

    var remaining = 0.0
    var cursor: T
    if (count > 0) {
        result.add(segments[0])
    }

    for (i in 0 until segments.size - 1) {
        val direction = segments[i + 1].first - segments[i].first
        val deltaT = segments[i + 1].second - segments[i].second

        val segmentLength = direction.length
        if (segmentLength + remaining < spacing) {
            remaining += segmentLength
        } else {
            val skipLength = (spacing - remaining).coerceAtLeast(0.0)
            val pointsFromSegment = 1 + ((segmentLength - skipLength) / spacing).roundToInt()

            // note: sometimes pointsFromSegments overestimates (due to roundToInt) the number of points
            // that should be sampled from the current segment by 1 at most.

            val skipL = skipLength / segmentLength
            val spaceL = spacing / segmentLength
            val start = segments[i]
            var l = skipL
            for (n in 0 until pointsFromSegment) {
                if (l < 1.0 + 1.0E-6) {
                    cursor = start.first + direction * l

                    result.add(cursor to start.second + deltaT * l)
                    l += spaceL
                }
            }
            remaining = (1.0 - (l - spaceL)) * segmentLength
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