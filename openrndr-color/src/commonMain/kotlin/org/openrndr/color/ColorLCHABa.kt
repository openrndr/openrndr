package org.openrndr.color

import org.openrndr.math.asDegrees
import org.openrndr.math.asRadians
import org.openrndr.math.mixAngle
import kotlin.math.*

data class ColorLCHABa(
    val l: Double,
    val c: Double,
    val h: Double,
    val alpha: Double = 1.0,
    val ref: ColorXYZa = ColorXYZa.NEUTRAL
) :
    ConvertibleToColorRGBa,
    ShadableColor<ColorLCHABa>,
    OpacifiableColor<ColorLCHABa>,
    HueShiftableColor<ColorLCHABa>,
    AlgebraicColor<ColorLCHABa> {
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
                h += PI * 2
            }

            h = h.asDegrees

            return ColorLCHABa(l, c, h, laba.alpha, laba.ref)
        }
    }


    fun toLABa(): ColorLABa {
        val a = c * cos(h.asRadians)
        val b = c * sin(h.asRadians)
        return ColorLABa(l, a, b, alpha, ref)
    }

    fun toXYZa(): ColorXYZa = toLABa().toXYZa()

    override fun toRGBa(): ColorRGBa = toLABa().toXYZa().toRGBa()

    fun toLSHABa() = ColorLSHABa.fromLCHABa(this)

    override fun opacify(factor: Double) = copy(alpha = alpha * factor)
    override fun shade(factor: Double) = copy(l = l * factor)
    override fun shiftHue(shiftInDegrees: Double) = copy(h = h + shiftInDegrees)

    override fun plus(right: ColorLCHABa) =
        copy(l = l + right.l, c = c + right.c, h = h + right.h, alpha = alpha + right.alpha)

    override fun minus(right: ColorLCHABa) =
        copy(l = l - right.l, c = c - right.c, h = h - right.h, alpha = alpha - right.alpha)

    override fun times(scale: Double) = copy(l = l * scale, c = c * scale, h = h * scale, alpha = alpha * scale)
    override fun mix(other: ColorLCHABa, factor: Double) = mix(this, other, factor)
}

fun mix(left: ColorLCHABa, right: ColorLCHABa, x: Double): ColorLCHABa {
    val sx = x.coerceIn(0.0, 1.0)
    return ColorLCHABa(
        (1.0 - sx) * left.l + sx * right.l,
        (1.0 - sx) * left.c + sx * right.c,
        mixAngle(left.h, right.h, sx),
        (1.0 - sx) * left.alpha + sx * right.alpha
    )
}
