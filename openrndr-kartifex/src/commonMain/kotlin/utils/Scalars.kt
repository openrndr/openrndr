package org.openrndr.kartifex.utils

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow

/**
 * @author ztellman
 */
object Scalars {
    const val SIGNIFICAND_WIDTH = 53
    const val MIN_EXPONENT = -1022
    const val MAX_EXPONENT = 1023
    const val EXP_BIT_MASK = 0x7FF0000000000000L
    const val EXP_BIAS        = 1023


    fun getExponent(d: Double): Int {
        /*
         * Bitwise convert d to long, mask out exponent bits, shift
         * to the right and then subtract out double's bias adjust to
         * get true exponent value.
         */
        return ((d.toRawBits() and EXP_BIT_MASK shr SIGNIFICAND_WIDTH - 1) - EXP_BIAS).toInt()
    }

    fun powerOfTwoD(exp:Double) : Double {
        return 2.0.pow(exp)
    }

    fun ulp(d: Double): Double {
        var exp: Int = getExponent(d)
        return when (exp) {
            MAX_EXPONENT + 1 -> abs(d)
            MIN_EXPONENT - 1 -> Double.MIN_VALUE
            else -> {
                //assert(exp <= java.lang.Double.MAX_EXPONENT && exp >= java.lang.Double.MIN_EXPONENT)

                // ulp(x) is usually 2^(SIGNIFICAND_WIDTH-1)*(2^ilogb(x))
                exp = exp - (SIGNIFICAND_WIDTH - 1)
                if (exp >= MIN_EXPONENT) {
                    powerOfTwoD(exp.toDouble())
                } else {
                    // return a subnormal result; left shift integer
                    // representation of Double.MIN_VALUE appropriate
                    // number of positions

                    Double.fromBits(
                        1L shl
                                exp - (MIN_EXPONENT - (SIGNIFICAND_WIDTH - 1))
                    )
                }
            }
        }
    }

    val MACHINE_EPSILON: Double = ulp(1.0)
    const val EPSILON = 1e-14
    fun equals(a: Double, b: Double, epsilon: Double): Boolean {
        return abs(a - b) < epsilon
    }

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
        val maxValue = max(max(a, b), max(c, d))
        return if (maxValue == 0.0) {
            1.0
        } else {
            val exponent: Double = getExponent(maxValue).toDouble()
            if (exponent < -8 || exponent > 8) 2.0.pow(-exponent) else 1.0
        }
    }

    fun normalizationFactor(a: Double, b: Double, c: Double): Double {
        val maxValue = max(a, b, c)
        return if (maxValue == 0.0) {
            1.0
        } else {
            val exponent = getExponent(maxValue).toDouble()
            if (exponent < -8 || exponent > 8) 2.0.pow(-exponent) else 1.0
        }
    }

    fun normalizationFactor(a: Double, b: Double): Double {
        val maxValue = max(a,b)
        return if (maxValue == 0.0) {
            1.0
        } else {
            val exponent: Double = getExponent(maxValue).toDouble()
            if (exponent < -8 || exponent > 8) 2.0.pow(-exponent) else 1.0
        }
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
}