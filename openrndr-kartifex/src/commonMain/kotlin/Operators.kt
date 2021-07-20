package org.openrndr.kartifex

typealias DoubleUnaryOperator = (Double) -> Double
typealias DoublePredicate = (Double) -> Boolean
typealias DoubleBinaryOperator = (Double, Double) -> Double

fun signum(d: Double): Double {
    return if (d == 0.0 || d.isNaN()) d else if (d < 0) -1.0 else 1.0
}