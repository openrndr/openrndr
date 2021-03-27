package org.openrndr.math.test

// code copied from openrndr-adopted-artifex and transformed to pure kotlin by IntelliJ

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.ulp

object Scalars {
    val MACHINE_EPSILON = 1.0.ulp
    const val EPSILON = 1e-14
    fun equals(a: Double, b: Double, epsilon: Double): Boolean {
        return abs(a - b) < epsilon
    }

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

    fun normalize(a: Double, b: Double, n: Double): Double {
        return (n - a) / (b - a)
    }

    fun lerp(a: Double, b: Double, t: Double): Double {
        return a + (b - a) * t
    }

    fun inside(min: Double, n: Double, max: Double): Boolean {
        return min < n && n < max
    }

    fun clamp(min: Double, n: Double, max: Double): Double {
        return if (n <= min) {
            min
        } else if (n >= max) {
            max
        } else {
            n
        }
    }

    fun normalizationFactor(a: Double, b: Double, c: Double, d: Double): Double {
        val exponent = getExponent(max(max(a, b), max(c, d))).toDouble()
        return if (exponent < -8 || exponent > 8) 2.0.pow(-exponent) else 1.0
    }

    fun normalizationFactor(a: Double, b: Double, c: Double): Double {
        var maxValue = max(a, b, c)
        if (maxValue == 0.0) {
            maxValue = min(a, b, c)
        }
        val exponent = getExponent(maxValue).toDouble()
        return if (exponent < -8 || exponent > 8) 2.0.pow(-exponent) else 1.0
    }

    fun normalizationFactor(a: Double, b: Double): Double {
        val exponent = getExponent(max(a, b)).toDouble()
        return if (exponent < -8 || exponent > 8) 2.0.pow(-exponent) else 1.0
    }

    fun max(a: Double, b: Double): Double {
        return if (a < b) b else a
    }

    fun max(a: Double, b: Double, c: Double): Double {
        return max(a, max(b, c))
    }

    fun min(a: Double, b: Double): Double {
        return if (a > b) b else a
    }

    fun min(a: Double, b: Double, c: Double): Double {
        return min(a, min(b, c))
    }

    // this was the only method missing in kotlin.math, so it was copied from transformed from JVM sources
    fun getExponent(d: Double): Int {
        /*
             * Bitwise convert d to long, mask out exponent bits, shift
             * to the right and then subtract out double's bias adjust to
             * get true exponent value.
             */
        return ((d.toRawBits() and EXP_BIT_MASK shr
                SIGNIFICAND_WIDTH - 1) - EXP_BIAS).toInt()
    }

    private const val EXP_BIT_MASK = 0x7FF0000000000000L
    private const val SIGNIFICAND_WIDTH = 53
    private const val EXP_BIAS = 1023

}
