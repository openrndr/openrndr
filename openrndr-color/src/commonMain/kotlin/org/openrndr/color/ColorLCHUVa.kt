package org.openrndr.color

import org.openrndr.math.asDegrees
import org.openrndr.math.asRadians
import org.openrndr.math.mixAngle
import kotlin.math.*

/**
 * The [CIELChUV color space](https://en.wikipedia.org/wiki/CIELUV#Cylindrical_representation_(CIELCh))
 * is the cylindrical representation of the CIELUV color space.
 *
 * @param l luminance, in a range of 0.0 (darkest) to 100.0 (brightest)
 * @param c chroma
 * @param h hue in degrees, where a full rotation is 360.0 degrees
 * @param alpha alpha as a percentage between 0.0 and 1.0
 * @param ref reference white against which the color values are calculated
 *
 * @see ColorLUVa
 */
data class ColorLCHUVa(
    val l: Double,
    val c: Double,
    val h: Double,
    val alpha: Double = 1.0,
    val ref: ColorXYZa = ColorXYZa.NEUTRAL
) :
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
                h += PI * 2
            }
            h = h.asDegrees
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
        val u = c * cos(h.asRadians)
        val v = c * sin(h.asRadians)
        return ColorLUVa(l, u, v, alpha, ref)
    }

    fun toLSHUVa() = ColorLSHUVa.fromLCHUVa(this)
    override fun toRGBa() = toLUVa().toRGBa()

    override fun opacify(factor: Double) = copy(alpha = alpha * factor)
    override fun shade(factor: Double) = copy(l = l * factor)
    override fun shiftHue(shiftInDegrees: Double) = copy(h = h + shiftInDegrees)

    override fun plus(right: ColorLCHUVa) =
        copy(l = l + right.l, c = c + right.c, h = h + right.h, alpha = alpha + right.alpha)

    override fun minus(right: ColorLCHUVa) =
        copy(l = l - right.l, c = c - right.c, h = h - right.h, alpha = alpha - right.alpha)

    override fun times(scale: Double) = copy(l = l * scale, c = c * scale, h = h * scale, alpha = alpha * scale)
    override fun mix(other: ColorLCHUVa, factor: Double) = mix(this, other, factor)
}

/**
 * Weighted mix between two colors in the LChUV color space.
 *
 * @param x the weighting of colors, a value 0.0 is equivalent to [left],
 * 1.0 is equivalent to [right] and at 0.5 both colors contribute to the result equally
 * @return a mix of [left] and [right] weighted by [x]
 */
fun mix(left: ColorLCHUVa, right: ColorLCHUVa, x: Double): ColorLCHUVa {
    val sx = x.coerceIn(0.0, 1.0)
    return ColorLCHUVa(
        (1.0 - sx) * left.l + sx * right.l,
        (1.0 - sx) * left.c + sx * right.c,
        mixAngle(left.h, right.h, sx),
        (1.0 - sx) * left.alpha + sx * right.alpha
    )
}
