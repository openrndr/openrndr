package org.openrndr.color

import org.openrndr.math.mixAngle

/**
 * color in HSL space
 * @param h hue in degrees (0 .. 360)
 * @param s saturation (0.0 .. 1.0)
 * @param l lightness (0.0 .. 1.0)
 * @param a alpha (0.0. .. 1.0)
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
            val min = if (rgb.r <= rgb.b && rgb.r <= rgb.g) {
                rgb.r
            } else if (rgb.g <= rgb.b && rgb.g <= rgb.r) {
                rgb.g
            } else if (rgb.b <= rgb.r && rgb.b <= rgb.g) {
                rgb.b
            } else {
                0.0
            }

            val max: Double
            val maxArg: ColorRGBa.Component

            if (rgb.r >= rgb.b && rgb.r >= rgb.g) {
                maxArg = ColorRGBa.Component.R
                max = rgb.r
            } else if (rgb.g >= rgb.b && rgb.g >= rgb.r) {
                maxArg = ColorRGBa.Component.G
                max = rgb.g
            } else {
                maxArg = ColorRGBa.Component.B
                max = rgb.b
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
                    ColorRGBa.Component.R -> 60.0 * ((rgb.g - rgb.b) / d + if (rgb.g < rgb.b) 6 else 0)
                    ColorRGBa.Component.G -> 60.0 * ((rgb.b - rgb.r) / d + 2.0)
                    ColorRGBa.Component.B -> 60.0 * ((rgb.r - rgb.g) / d + 4.0)
                    ColorRGBa.Component.a -> 0.0
                }
            }
            return ColorHSLa(h, s, l, rgb.a)
        }
    }

    override fun shiftHue(shiftInDegrees: Double): ColorHSLa = copy(h = (h + shiftInDegrees))
    override fun saturate(factor: Double) = copy(s = s * factor)
    override fun shade(factor: Double) = copy(l = l * factor)

    override fun mix(other: ColorHSLa, x: Double) = mix(this, other, x)

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
    override fun plus(other: ColorHSLa) = copy(h = h + other.h, s = s + other.s, l = l + other.l, a = a + other.a)
    override fun minus(other: ColorHSLa) = copy(h = h - other.h, s = s - other.s, l = l - other.l, a = a - other.a)
    override fun times(factor: Double) = copy(h = h * factor, s = s * factor, l = l * factor, a = a * factor)

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
 * Mixes two colors in HSLa space
 * @param left the left hand ColorHSLa color
 * @param right the right hand ColorHSLa
 * @param x the mix amount
 * @return a mix of [left] and [right], x == 0.0 corresponds with left, x == 1.0 corresponds with right
 */
fun mix(left: ColorHSLa, right: ColorHSLa, x: Double): ColorHSLa {
    val sx = x.coerceIn(0.0, 1.0)
    return ColorHSLa(
            mixAngle(left.h, right.h, sx),
            (1.0 - sx) * left.s + sx * right.s,
            (1.0 - sx) * left.l + sx * right.l,
            (1.0 - sx) * left.a + sx * right.a)
}
