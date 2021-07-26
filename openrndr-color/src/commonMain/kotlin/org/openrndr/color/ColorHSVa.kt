@file:Suppress("unused")

package org.openrndr.color

import org.openrndr.math.mixAngle
import org.openrndr.math.mod
import kotlin.math.floor


/**
 * The [HSV color space](https://en.wikipedia.org/wiki/HSL_and_HSV).
 *
 * @see ColorHSLa
 *
 * @param h hue in degrees, where a full rotation is 360.0 degrees
 * @param s saturation as a percentage between 0.0 and 1.0
 * @param v value/brightness as a percentage between 0.0 and 1.0
 * @param a alpha as a percentage between 0.0 and 1.0
 */
@Suppress("unused")
data class ColorHSVa(val h: Double, val s: Double, val v: Double, val a: Double = 1.0) :
        ConvertibleToColorRGBa,
        ShadableColor<ColorHSVa>,
        HueShiftableColor<ColorHSVa>,
        SaturatableColor<ColorHSVa>,
        AlgebraicColor<ColorHSVa> {

    operator fun invoke(h: Double = this.h, s: Double = this.s, v: Double = this.v, a: Double = this.a) =
            ColorHSVa(h, s, v, a)

    companion object {
        fun fromRGBa(rgb: ColorRGBa): ColorHSVa {
            val srgb = rgb.toSRGB()
            var min = Double.POSITIVE_INFINITY
            var max = Double.NEGATIVE_INFINITY

            var h: Double
            var maxArg: ColorRGBa.Component? = null

            if (srgb.r <= srgb.b && srgb.r <= srgb.g) {
                min = srgb.r
            }
            if (srgb.g <= srgb.b && srgb.g <= srgb.r) {
                min = srgb.g
            }
            if (srgb.b <= srgb.r && srgb.b <= srgb.g) {
                min = srgb.b
            }

            if (srgb.r >= srgb.b && srgb.r >= srgb.g) {
                maxArg = ColorRGBa.Component.R
                max = srgb.r
            }
            if (srgb.g >= srgb.b && srgb.g >= srgb.r) {
                maxArg = ColorRGBa.Component.G
                max = srgb.g
            }
            if (srgb.b >= srgb.r && srgb.b >= srgb.g) {
                maxArg = ColorRGBa.Component.B
                max = srgb.b
            }

            val s: Double
            val v = max
            val delta = max - min
            if (max != 0.0) {
                s = delta / max
            } else {
                // r = g = b = 0		// s = 0, v is undefined
                s = 0.0
                h = 0.0
                return ColorHSVa(h, s, v, srgb.a)
            }
            if (maxArg == ColorRGBa.Component.R) {
                h = (srgb.g - srgb.b) / delta        // between yellow & magenta
            } else if (maxArg == ColorRGBa.Component.G) {
                h = 2 + (srgb.b - srgb.r) / delta    // between cyan & yellow
            } else {
                h = 4 + (srgb.r - srgb.g) / delta    // between magenta & cyan
            }
            h *= 60.0                // degrees
            if (h < 0) {
                h += 360.0
            }
            return ColorHSVa(h, s, v, srgb.a)
        }
    }


    override fun shiftHue(shiftInDegrees: Double) = copy(h = (h + shiftInDegrees))
    override fun saturate(factor: Double) = copy(s = s * factor)
    override fun shade(factor: Double): ColorHSVa = copy(v = v * factor)

    override fun mix(other: ColorHSVa, factor: Double) = mix(this, other, factor)

    /**
     * a unit presentation of this ColorHSVa, essentially brings the hue back in [0, 360)
     * @return a copy with the hue value in [0, 360)
     */
    val unit get() = copy(h = ((h % 360) + 360) % 360)

    override fun toRGBa(): ColorRGBa {
        val i: Int
        val f: Double

        val r: Double
        val g: Double
        val b: Double
        val hsv = this

        val sh = mod(hsv.h, 360.0) / 60            // sector 0 to 5
        i = floor(sh).toInt()
        f = sh - i            // factorial part of h
        val p = hsv.v * (1 - hsv.s)
        val q = hsv.v * (1 - hsv.s * f)
        val t = hsv.v * (1 - hsv.s * (1 - f))
        if (s > 0.00001) {
            when (i) {
                0 -> {
                    r = hsv.v
                    g = t
                    b = p
                }
                1 -> {
                    r = q
                    g = hsv.v
                    b = p
                }
                2 -> {
                    r = p
                    g = hsv.v
                    b = t
                }
                3 -> {
                    r = p
                    g = q
                    b = hsv.v
                }
                4 -> {
                    r = t
                    g = p
                    b = hsv.v
                }
                else        // case 5:
                -> {
                    r = hsv.v
                    g = p
                    b = q
                }
            }
        } else {
            r = hsv.v
            g = hsv.v
            b = hsv.v
        }
        return ColorRGBa(r, g, b, hsv.a, Linearity.SRGB)

    }

    override fun plus(right: ColorHSVa) = copy(h = h + right.h, s = s + right.s, v = v + right.v, a = a + right.a)
    override fun minus(right: ColorHSVa) = copy(h = h - right.h, s = s - right.s, v = v - right.v, a = a - right.a)
    override fun times(scale: Double) = copy(h = h * scale, s = s * scale, v = v * scale, a = a * scale)

    fun toXSVa(): ColorXSVa {
        return ColorXSVa.fromHSVa(this)
    }
}

fun hsv(h: Double, s: Double, v: Double) = ColorHSVa(h, s, v)
fun hsva(h: Double, s: Double, v: Double, a: Double) = ColorHSVa(h, s, v, a)

/**
 * Weighted mix between two colors in the HSVa color space.
 * @param left the left-hand ColorHSVa to mix
 * @param right the right-hand ColorHSVa to mix
 * @param x the weighting of colors, a value 0.0 is equivalent to [left],
 * 1.0 is equivalent to [right] and at 0.5 both colors contribute to the result equally
 * @return a mix of [left] and [right] weighted by [x]
 */
fun mix(left: ColorHSVa, right: ColorHSVa, x: Double): ColorHSVa {
    val sx = x.coerceIn(0.0, 1.0)
    return ColorHSVa(
            mixAngle(left.h, right.h, sx),
            (1.0 - sx) * left.s + sx * right.s,
            (1.0 - sx) * left.v + sx * right.v,
            (1.0 - sx) * left.a + sx * right.a)
}
