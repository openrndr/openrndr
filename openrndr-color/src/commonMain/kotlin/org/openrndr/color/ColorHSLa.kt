package org.openrndr.color

import org.openrndr.math.mixAngle

/**
 * The [HSL color space](https://en.wikipedia.org/wiki/HSL_and_HSV).
 *
 * @see ColorHSVa
 *
 * @param h hue in degrees, where a full rotation is 360.0 degrees
 * @param s saturation as a percentage between 0.0 and 1.0
 * @param l lightness/luminance as a percentage between 0.0 and 1.0
 * @param a alpha as a percentage between 0.0 and 1.0
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
data class ColorHSLa(val h: Double, val s: Double, val l: Double, val a: Double = 1.0) :
        ConvertibleToColorRGBa,
        ShadableColor<ColorHSLa>,
        HueShiftableColor<ColorHSLa>,
        SaturatableColor<ColorHSLa>,
        AlgebraicColor<ColorHSLa> {

    operator fun invoke(h: Double = this.h, s: Double = this.s, l: Double = this.l, a: Double = this.a) =
            ColorHSLa(h, s, l, a)

    override fun toString(): String {
        return "ColorHSL{" +
                "h=" + h +
                ", s=" + s +
                ", l=" + l +
                '}'
    }

    companion object {
        fun fromRGBa(rgb: ColorRGBa): ColorHSLa {
            val srgb = rgb.toSRGB()
            val min = if (srgb.r <= srgb.b && srgb.r <= srgb.g) {
                srgb.r
            } else if (srgb.g <= srgb.b && srgb.g <= srgb.r) {
                srgb.g
            } else if (srgb.b <= srgb.r && srgb.b <= srgb.g) {
                srgb.b
            } else {
                0.0
            }

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

            val l = (max + min) / 2.0
            val s: Double
            val h: Double
            if (max == min) {
                s = 0.0
                h = s
            } else {
                val d = max - min
                s = if (l > 0.5) d / (2.0 - max - min) else d / (max + min)
                h = when (maxArg) {
                    ColorRGBa.Component.R -> 60.0 * ((srgb.g - srgb.b) / d + if (srgb.g < srgb.b) 6 else 0)
                    ColorRGBa.Component.G -> 60.0 * ((srgb.b - srgb.r) / d + 2.0)
                    ColorRGBa.Component.B -> 60.0 * ((srgb.r - srgb.g) / d + 4.0)
                    ColorRGBa.Component.a -> 0.0
                }
            }
            return ColorHSLa(h, s, l, srgb.a)
        }
    }

    override fun shiftHue(shiftInDegrees: Double): ColorHSLa = copy(h = (h + shiftInDegrees))
    override fun saturate(factor: Double) = copy(s = s * factor)
    override fun shade(factor: Double) = copy(l = l * factor)

    override fun mix(other: ColorHSLa, factor: Double) = mix(this, other, factor)

    val unit get() = copy(h = ((h % 360) + 360) % 360)

    override fun toRGBa(): ColorRGBa {
        return if (s == 0.0) {
            ColorRGBa(l, l, l, a)
        } else {
            val q = if (l < 0.5) l * (1 + s) else l + s - l * s
            val p = 2 * l - q
            val r = hue2rgb(p, q, h / 360.0 + 1.0 / 3)
            val g = hue2rgb(p, q, h / 360.0)
            val b = hue2rgb(p, q, h / 360.0 - 1.0 / 3)
            ColorRGBa(r, g, b, a, Linearity.SRGB)
        }
    }

    fun toHSVa(): ColorHSVa = toRGBa().toHSVa()
    fun toXYZa(): ColorXYZa = toRGBa().toXYZa()
    fun toLABa(ref: ColorXYZa = ColorXYZa.NEUTRAL) = toRGBa().toXYZa().toLABa(ref)
    fun toLUVa(ref: ColorXYZa = ColorXYZa.NEUTRAL) = toRGBa().toXYZa().toLUVa(ref)
    fun toLCHABa(ref: ColorXYZa = ColorXYZa.NEUTRAL) = toLABa(ref).toLCHABa()
    fun toLCHUVa(ref: ColorXYZa = ColorXYZa.NEUTRAL) = toLUVa(ref).toLCHUVa()

    /**
     * convert to [ColorXSLa]
     */
    fun toXSLa() = ColorXSLa.fromHSLa(this)
    override fun plus(right: ColorHSLa) = copy(h = h + right.h, s = s + right.s, l = l + right.l, a = a + right.a)
    override fun minus(right: ColorHSLa) = copy(h = h - right.h, s = s - right.s, l = l - right.l, a = a - right.a)
    override fun times(scale: Double) = copy(h = h * scale, s = s * scale, l = l * scale, a = a * scale)

}

internal fun hue2rgb(p: Double, q: Double, ut: Double): Double {
    var t = ut
    while (t < 0) t += 1.0
    while (t > 1) t -= 1.0
    if (t < 1.0 / 6.0) return p + (q - p) * 6.0 * t
    if (t < 1.0 / 2.0) return q
    return if (t < 2.0 / 3.0) p + (q - p) * (2.0 / 3.0 - t) * 6.0 else p
}

fun hsl(h: Double, s: Double, l: Double) = ColorHSLa(h, s, l)
fun hsla(h: Double, s: Double, l: Double, a: Double) = ColorHSLa(h, s, l, a)

/**
 * Weighted mix between two colors in the HSL color space.
 * @param left the left-hand ColorHSLa to mix
 * @param right the right-hand ColorHSLa to mix
 * @param x the weighting of colors, a value 0.0 is equivalent to [left],
 * 1.0 is equivalent to [right] and at 0.5 both colors contribute to the result equally
 * @return a mix of [left] and [right] weighted by [x]
 */
fun mix(left: ColorHSLa, right: ColorHSLa, x: Double): ColorHSLa {
    val sx = x.coerceIn(0.0, 1.0)
    return ColorHSLa(
            mixAngle(left.h, right.h, sx),
            (1.0 - sx) * left.s + sx * right.s,
            (1.0 - sx) * left.l + sx * right.l,
            (1.0 - sx) * left.a + sx * right.a)
}
