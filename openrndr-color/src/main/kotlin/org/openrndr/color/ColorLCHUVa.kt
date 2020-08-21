package org.openrndr.color

import org.openrndr.math.mixAngle
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class ColorLCHUVa(val l: Double, val c: Double, val h: Double, val alpha: Double = 1.0, val ref: ColorXYZa = ColorXYZa.NEUTRAL) :
        ConvertibleToColorRGBa,
        ShadableColor<ColorLCHUVa>,
        OpacifiableColor<ColorLCHUVa>,
        HueShiftableColor<ColorLCHUVa>,
        AlgebraicColor<ColorLCHUVa> {

    companion object {
        fun fromLUVa(luva: ColorLUVa): ColorLCHUVa {
            val l = luva.l
            val c = sqrt(luva.u * luva.u + luva.v * luva.v)
            var h = atan2(luva.v, luva.u)

            if (h < 0) {
                h += Math.PI * 2
            }
            h = Math.toDegrees(h)
            return ColorLCHUVa(l, c, h, luva.alpha, luva.ref)
        }

        fun findMaxChroma(l: Double, h: Double, ref: ColorXYZa): Double {
            var left = 0.0
            var right = 2000.0
            var bestGuess = left
            while (true) {

                if (right - left < 0.0001) {
                    return bestGuess
                }

                val leftTry = ColorLCHUVa(l, left, h, 1.0, ref)
                val rightTry = ColorLCHUVa(l, right, h, 1.0, ref)
                val middle = (left + right) / 2
                val middleTry = ColorLCHUVa(l, middle, h, 1.0, ref)

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
    }


    fun toLUVa(): ColorLUVa {
        val u = c * cos(Math.toRadians(h))
        val v = c * sin(Math.toRadians(h))
        return ColorLUVa(l, u, v, alpha, ref)
    }

    fun toLSHUVa() = ColorLSHUVa.fromLCHUVa(this)
    override fun toRGBa() = toLUVa().toRGBa()

    override fun opacify(factor: Double) = copy(alpha = alpha * factor)
    override fun shade(factor: Double) = copy(l = l * factor)
    override fun shiftHue(shiftInDegrees: Double) = copy(h = h + shiftInDegrees)

    override fun plus(other: ColorLCHUVa) = copy(l = l + other.l, c = c + other.c, h = h + other.h, alpha = alpha + other.alpha)
    override fun minus(other: ColorLCHUVa) = copy(l = l - other.l, c = c - other.c, h = h - other.h, alpha = alpha - other.alpha)
    override fun times(factor: Double) = copy(l = l * factor, c = c * factor, h = h * factor, alpha = alpha * factor)
    override fun mix(other: ColorLCHUVa, factor: Double) = mix(this, other, factor)
}


fun mix(left: ColorLCHUVa, right: ColorLCHUVa, x: Double): ColorLCHUVa {
    val sx = x.coerceIn(0.0, 1.0)
    return ColorLCHUVa(
            (1.0 - sx) * left.l + sx * right.l,
            (1.0 - sx) * left.c + sx * right.c,
            mixAngle(left.h, right.h, sx),
            (1.0 - sx) * left.alpha + sx * right.alpha)
}
