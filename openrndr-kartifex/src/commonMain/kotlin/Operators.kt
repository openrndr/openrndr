package org.openrndr.kartifex

typealias DoubleUnaryOperator = (Double) -> Double
typealias DoublePredicate = (Double) -> Boolean
typealias DoubleBinaryOperator = (Double, Double) -> Double

/**
 * Determines the sign of the given double value. Returns -1.0 if the value is negative,
 * 1.0 if the value is positive, and the value itself if it is either 0.0 or NaN.
 *
 * @param d the double value whose sign is to be determined
 * @return -1.0 if d is negative, 1.0 if d is positive, or d itself if it is 0.0 or NaN
 */
fun signum(d: Double): Double {
    return if (d == 0.0 || d.isNaN()) d else if (d < 0) -1.0 else 1.0
}