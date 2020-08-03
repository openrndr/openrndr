package org.openrndr.math

import java.lang.Math.getExponent
import kotlin.math.max
import kotlin.math.pow

fun normalizationFactor(a: Double, b: Double, c: Double, d: Double): Double {
    val exponent = getExponent(max(max(a, b), max(c, d))).toDouble()
    return if (exponent < -8 || exponent > 8) 2.0.pow(-exponent) else 1.0
}

fun normalizationFactor(a: Double, b: Double, c: Double): Double {
    val exponent = getExponent(max(max(a, b), c)).toDouble()
    return if (exponent < -8 || exponent > 8) 2.0.pow(-exponent) else 1.0
}

fun normalizationFactor(a: Double, b: Double): Double {
    val exponent = getExponent(max(a, b)).toDouble()
    return if (exponent < -8 || exponent > 8) 2.0.pow(-exponent) else 1.0
}
