package org.openrndr.math

import kotlin.math.max
import kotlin.math.pow

/**
 * Computes a normalization factor based on the maximum exponent of the input values.
 *
 * @param a The first input value.
 * @param b The second input value.
 * @param c The third input value.
 * @param d The fourth input value.
 * @return A normalization factor that is either 1.0 or a power of 2, determined by the input values.
 */
fun normalizationFactor(a: Double, b: Double, c: Double, d: Double): Double {
    val exponent = max(max(a, b), max(c, d)).asExponent.toDouble()
    return if (exponent < -8 || exponent > 8) 2.0.pow(-exponent) else 1.0
}

/**
 * Computes a normalization factor based on the input values `a`, `b`, and `c`.
 * The factor is calculated as a power of 2, inversely proportional to the largest exponent
 * of the input values, if it falls outside the range of [-8, 8]. Otherwise, the factor defaults to 1.0.
 *
 * @param a the first input value
 * @param b the second input value
 * @param c the third input value
 * @return the computed normalization factor
 */
fun normalizationFactor(a: Double, b: Double, c: Double): Double {
    val exponent = max(max(a, b), c).asExponent.toDouble()
    return if (exponent < -8 || exponent > 8) 2.0.pow(-exponent) else 1.0
}

fun normalizationFactor(a: Double, b: Double): Double {
    val exponent = max(a, b).asExponent.toDouble()
    return if (exponent < -8 || exponent > 8) 2.0.pow(-exponent) else 1.0
}
