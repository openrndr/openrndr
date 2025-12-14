package org.openrndr.kartifex.utils

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow

/**
 * @author ztellman
 */
const val SIGNIFICAND_WIDTH = 53
const val MIN_EXPONENT = -1022
const val MAX_EXPONENT = 1023
const val EXP_BIT_MASK = 0x7FF0000000000000L
const val EXP_BIAS = 1023


fun getExponent(d: Double): Int {
    /*
     * Bitwise convert d to long, mask out exponent bits, shift
     * to the right and then subtract out double's bias adjust to
     * get true exponent value.
     */
    return ((d.toRawBits() and EXP_BIT_MASK shr SIGNIFICAND_WIDTH - 1) - EXP_BIAS).toInt()
}

fun powerOfTwoD(exp: Double): Double {
    return 2.0.pow(exp)
}

/**
 * Computes the size of the unit in the last place (ULP) of the provided double-precision floating-point value.
 * The ULP of a nonzero floating-point value is the positive distance between the given value and the next larger representable value of the same precision.
 *
 * @param d the double value for which the ULP is to be computed
 * @return the size of the unit in the last place of the provided value
 */
fun ulp(d: Double): Double {
    var exp: Int = getExponent(d)
    return when (exp) {
        MAX_EXPONENT + 1 -> abs(d)
        MIN_EXPONENT - 1 -> Double.MIN_VALUE
        else -> {
            //assert(exp <= java.lang.Double.MAX_EXPONENT && exp >= java.lang.Double.MIN_EXPONENT)

            // ulp(x) is usually 2^(SIGNIFICAND_WIDTH-1)*(2^ilogb(x))
            exp -= (SIGNIFICAND_WIDTH - 1)
            if (exp >= MIN_EXPONENT) {
                powerOfTwoD(exp.toDouble())
            } else {
                // return a subnormal result; left shift integer
                // representation of Double.MIN_VALUE appropriate
                // number of positions

                Double.fromBits(
                    1L shl exp - (MIN_EXPONENT - (SIGNIFICAND_WIDTH - 1))
                )
            }
        }
    }
}

val MACHINE_EPSILON: Double = ulp(1.0)
const val SCALAR_EPSILON = 1e-14
fun equals(a: Double, b: Double, epsilon: Double): Boolean {
    return abs(a - b) < epsilon
}

/**
 * Compares two angles, taking into account wrapping around at 2π, to determine if they are approximately equal within a given tolerance.
 *
 * @param t0 the first angle in radians
 * @param t1 the second angle in radians
 * @param epsilon the tolerance within which the angles are considered equal
 * @return `true` if the angles are approximately equal within the given tolerance, `false` otherwise
 */
@Suppress("NAME_SHADOWING")
fun angleEquals(t0: Double, t1: Double, epsilon: Double): Boolean {
    var t0 = t0
    var t1 = t1
    if (t1 < t0) {
        val tmp = t1
        t1 = t0
        t0 = tmp
    }
    var result = t1 - t0 < epsilon
    if (!result) {
        t1 -= PI * 2
        result = t0 - t1 < epsilon
    }
    return result
}

fun normalize(a: Double, b: Double, n: Double): Double = (n - a) / (b - a)

fun lerp(a: Double, b: Double, t: Double): Double = a + (b - a) * t

fun inside(min: Double, n: Double, max: Double): Boolean = min < n && n < max

fun clamp(min: Double, n: Double, max: Double): Double = if (n <= min) {
    min
} else if (n >= max) {
    max
} else {
    n
}

/**
 * Computes a normalization factor for a given set of four double values.
 * The normalization ensures values are scaled properly based on their maximum magnitude.
 *
 * @param a The first value.
 * @param b The second value.
 * @param c The third value.
 * @param d The fourth value.
 * @return A double representing the normalization factor.
 */
fun normalizationFactor(a: Double, b: Double, c: Double, d: Double): Double {
    val maxValue = max(max(a, b), max(c, d))
    return if (maxValue == 0.0) {
        1.0
    } else {
        val exponent: Double = getExponent(maxValue).toDouble()
        if (exponent < -8 || exponent > 8) 2.0.pow(-exponent) else 1.0
    }
}

/**
 * Calculates a normalization factor based on the maximum value among the input parameters
 * and their exponent. It ensures the result lies within a reasonable range for numerical stability.
 *
 * @param a the first input value
 * @param b the second input value
 * @param c the third input value
 * @return the computed normalization factor
 */
fun normalizationFactor(a: Double, b: Double, c: Double): Double {
    val maxValue = max(a, b, c)
    return if (maxValue == 0.0) {
        1.0
    } else {
        val exponent = getExponent(maxValue).toDouble()
        if (exponent < -8 || exponent > 8) 2.0.pow(-exponent) else 1.0
    }
}

/**
 * Calculates a normalization factor based on the maximum value between two given numbers
 * and the exponent of that maximum value. The normalization factor ensures that values
 * within a certain range are normalized appropriately.
 *
 * @param a the first double value
 * @param b the second double value
 * @return the normalization factor based on the input values
 */
fun normalizationFactor(a: Double, b: Double): Double {
    val maxValue = max(a, b)
    return if (maxValue == 0.0) {
        1.0
    } else {
        val exponent: Double = getExponent(maxValue).toDouble()
        if (exponent < -8 || exponent > 8) 2.0.pow(-exponent) else 1.0
    }
}

fun max(a: Double, b: Double): Double = if (a < b) b else a

fun max(a: Double, b: Double, c: Double): Double = max(a, max(b, c))

fun min(a: Double, b: Double): Double = if (a > b) b else a

fun min(a: Double, b: Double, c: Double): Double = min(a, min(b, c))
