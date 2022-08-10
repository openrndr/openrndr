@file:Suppress("unused")

package org.openrndr.color

import org.openrndr.math.Vector4
import org.openrndr.math.mixAngle
import org.openrndr.math.mod
import kotlin.jvm.JvmOverloads
import kotlin.math.floor


/**
 * The [HSV color space](https://en.wikipedia.org/wiki/HSL_and_HSV).
 *
 * @see ColorHSLa
 *
 * @param h hue in degrees, where a full rotation is 360.0 degrees
 * @param s saturation as a percentage between 0.0 and 1.0
 * @param v value/brightness as a percentage between 0.0 and 1.0
 * @param alpha alpha as a percentage between 0.0 and 1.0
 */
@Suppress("unused")
data class ColorHSVa @JvmOverloads constructor (val h: Double, val s: Double, val v: Double, override val alpha: Double = 1.0) :
        ColorModel<ColorHSVa>,
        ShadableColor<ColorHSVa>,
        HueShiftableColor<ColorHSVa>,
        SaturatableColor<ColorHSVa>,
        AlgebraicColor<ColorHSVa> {

    companion object {
        fun fromRGBa(rgb: ColorRGBa): ColorHSVa {
            val srgb = rgb.toSRGB()
            val min = srgb.minValue

            val max: Double
            val maxArg: ColorRGBa.Component

            if (srgb.r >= srgb.b && srgb.r >= srgb.g) {
                maxArg = ColorRGBa.Component.R
                max = srgb.r
            } else if (srgb.g >= srgb.b && srgb.g >= srgb.r) {
                maxArg = ColorRGBa.Component.G
                max = srgb.g
            } else {
                maxArg = ColorRGBa.Component.B
                max = srgb.b
            }

            val v = max
            // In the case r == g == b
            if (min == max) {
                return ColorHSVa(0.0, 0.0, v, srgb.alpha)
            }
            val delta = max - min
            val s = delta / max
            var h = 60 * when (maxArg) {
                ColorRGBa.Component.R -> (srgb.g - srgb.b) / delta // between yellow & magenta
                ColorRGBa.Component.G -> (srgb.b - srgb.r) / delta + 2.0 // between cyan & yellow
                ColorRGBa.Component.B -> (srgb.r - srgb.g) / delta + 4.0 // between magenta & cyan
            }
            if (h < 0) {
                h += 360.0
            }
            return ColorHSVa(h, s, v, srgb.alpha)
        }
    }

    @Deprecated("Legacy alpha parameter name", ReplaceWith("alpha"))
    val a = alpha

    override fun opacify(factor: Double): ColorHSVa = copy(alpha = alpha * factor)
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
        return ColorRGBa(r, g, b, hsv.alpha, Linearity.SRGB)

    }

    override fun plus(right: ColorHSVa) = copy(
        h = h + right.h,
        s = s + right.s,
        v = v + right.v,
        alpha = alpha + right.alpha
    )
    override fun minus(right: ColorHSVa) = copy(
        h = h - right.h,
        s = s - right.s,
        v = v - right.v,
        alpha = alpha - right.alpha
    )
    override fun times(scale: Double) = copy(h = h * scale, s = s * scale, v = v * scale, alpha = alpha * scale)

    override fun toVector4(): Vector4 = Vector4(h, s, v, alpha)

    fun toXSVa(): ColorXSVa {
        return ColorXSVa.fromHSVa(this)
    }
}

fun hsv(h: Double, s: Double, v: Double, a: Double = 1.0) = ColorHSVa(h, s, v, a)

@Deprecated("Use hsv(h, s, v, a)", ReplaceWith("hsv(h, s, v, a)"), DeprecationLevel.WARNING)
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
        (1.0 - sx) * left.alpha + sx * right.alpha
    )
}
