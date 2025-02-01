package org.openrndr.color

import org.openrndr.math.map

/**
 * A list of pairs representing ranges of hues and their corresponding value ranges.
 *
 * Each pair maps a range of hue values (in degrees) to a range of associated values.
 * These pairs can be used to classify or map color hues to specific value intervals.
 */
private val hueRanges = listOf(
    0.0..<35.0 to 0.0..60.0,
    35.0..<60.0 to 60.0..120.0,
    60.0..<135.0 to 120.0..180.0,
    135.0..<225.0 to 180.0..240.0,
    225.0..<275.0 to 240.0..300.0,
    275.0..<360.0 to 300.0..360.0
)
/**
 * A list of pairs representing ranges representing the inverse of [hueRanges]
 */
private val xueRanges = listOf(
    0.0..<60.0 to 0.0..35.0,
    60.0..<120.0 to 35.0..60.0,
    120.0..<180.0 to 60.0..135.0,
    180.0..<240.0 to 135.0..225.0,
    240.0..<300.0 to 225.0..275.0,
    300.0..<360.0 to 275.0..360.0
)

/**
 * Maps a given value `t` based on a specified list of input and output ranges.
 * The method determines the appropriate input range and corresponding output range
 * from the list, and then performs a linear mapping of the given value within
 * those ranges. The input value is treated as a hue and wrapped within 0 to 360 degrees.
 *
 * @param ranges a list of pairs where each pair contains an open-ended input range
 *               and a corresponding closed floating-point output range
 * @param t the input value to be mapped
 * @return the mapped value in the output range corresponding to the input value
 */
private fun mapRange(ranges: List<Pair<OpenEndRange<Double>, ClosedFloatingPointRange<Double>>>, t: Double): Double {
    val hue = t.mod(360.0)
    val range = ranges.first { (range, _) -> hue in range }
    val (inputRange, outputRange) = range
    return map(inputRange.start..inputRange.endExclusive, outputRange, hue)
}

internal fun hueToXue(hue: Double): Double = mapRange(hueRanges, hue)
internal fun xueToHue(xue: Double): Double = mapRange(xueRanges, xue)
