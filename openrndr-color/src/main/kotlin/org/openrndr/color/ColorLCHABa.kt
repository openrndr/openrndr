package org.openrndr.color

import org.openrndr.math.mixAngle
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class ColorLCHABa(val l: Double, val c: Double, val h: Double, val alpha: Double = 1.0, val ref: ColorXYZa = ColorXYZa.NEUTRAL):
    ConvertibleToColorRGBa,
    ShadableColor<ColorLCHABa>,
    OpacifiableColor<ColorLCHABa>,
    HueShiftableColor<ColorLCHABa>,
    AlgebraicColor<ColorLCHABa>

{
    companion object {
        fun findMaxChroma(l: Double, h: Double, ref: ColorXYZa): Double {
            var left = 0.0
            var right = 2000.0
            var bestGuess = left
            while (true) {

                if (right - left < 0.0001) {
                    return bestGuess
                }

                val leftTry = ColorLCHABa(l, left, h, 1.0, ref)
                val rightTry = ColorLCHABa(l, right, h, 1.0, ref)
                val middle = (left + right) / 2
                val middleTry = ColorLCHABa(l, middle, h, 1.0, ref)

                val leftValid = leftTry.toRGBa().let { it.minValue >= 0 && it.maxValue <= 1.0 }
                val rightValid = rightTry.toRGBa().let { it.minValue >= 0 && it.maxValue <= 1.0 }
                val middleValid = middleTry.toRGBa().let { it.minValue >= 0 && it.maxValue <= 1.0 }

                if (leftValid && middleValid && !rightValid) {
                    val newLeft = middle
                    val newRight = right
                    bestGuess = middle
                    left = newLeft
                    right = newRight
                }

                if (leftValid && !middleValid && !rightValid) {
                    val newLeft = left
                    val newRight = middle
                    left = newLeft
                    right = newRight
                }

                if (leftValid == middleValid && middleValid == rightValid) {
                    return bestGuess
                }
            }
        }


        fun fromLABa(laba: ColorLABa): ColorLCHABa {
            val l = laba.l
            val c = sqrt(laba.a * laba.a + laba.b * laba.b)
            var h = atan2(laba.b, laba.a)

            if (h < 0) {
                h += Math.PI * 2
            }

            h = Math.toDegrees(h)

            return ColorLCHABa(l, c, h, laba.alpha, laba.ref)
        }
    }



    fun toLABa(): ColorLABa {
        val a = c * cos(Math.toRadians(h))
        val b = c * sin(Math.toRadians(h))
        return ColorLABa(l, a, b, alpha, ref)
    }

    fun toXYZa(): ColorXYZa = toLABa().toXYZa()

    override fun toRGBa(): ColorRGBa = toLABa().toXYZa().toRGBa()

    fun toLSHABa() = ColorLSHABa.fromLCHABa(this)

    override fun opacify(factor: Double) = copy(alpha = alpha * factor)
    override fun shade(factor: Double) = copy(l = l * factor)
    override fun shiftHue(shiftInDegrees: Double) = copy(h = h + shiftInDegrees)

    override fun plus(other: ColorLCHABa) = copy(l = l + other.l, c = c + other.c, h = h + other.h, alpha = alpha + other.alpha)
    override fun minus(other: ColorLCHABa) = copy(l = l - other.l, c = c  -other.c, h = h - other.h, alpha = alpha - other.alpha)
    override fun times(factor: Double)= copy(l = l * factor, c = c * factor, h = h * factor, alpha = alpha * factor)
    override fun mix(other: ColorLCHABa, factor: Double) = mix(this, other, factor)
}

fun mix(left: ColorLCHABa, right: ColorLCHABa, x: Double): ColorLCHABa {
    val sx = x.coerceIn(0.0, 1.0)
    return ColorLCHABa(
            (1.0 - sx) * left.l + sx * right.l,
            (1.0 - sx) * left.c + sx * right.c,
            mixAngle(left.h, right.h, sx),
            (1.0 - sx) * left.alpha + sx * right.alpha)
}
