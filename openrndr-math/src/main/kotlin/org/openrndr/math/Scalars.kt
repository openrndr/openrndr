package org.openrndr.math

import kotlin.math.max
import kotlin.math.pow

fun normalizationFactor(a: Double, b: Double, c: Double, d: Double): Double {
    val exponent = max(max(a, b), max(c, d)).asExponent.toDouble()
    return if (exponent < -8 || exponent > 8) 2.0.pow(-exponent) else 1.0
}

fun normalizationFactor(a: Double, b: Double, c: Double): Double {
    val exponent = max(max(a, b), c).asExponent.toDouble()
    return if (exponent < -8 || exponent > 8) 2.0.pow(-exponent) else 1.0
}

fun normalizationFactor(a: Double, b: Double): Double {
    val exponent = max(a, b).asExponent.toDouble()
    return if (exponent < -8 || exponent > 8) 2.0.pow(-exponent) else 1.0
}
