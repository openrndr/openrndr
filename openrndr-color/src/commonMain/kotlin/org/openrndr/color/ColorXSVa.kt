package org.openrndr.color

import kotlinx.serialization.Serializable
import org.openrndr.math.Vector4
import org.openrndr.math.mixAngle

/**
 * Practically identical to [ColorHSVa], but
 * for mapping colors to classical painter's scheme
 * of complementary colors.
 *
 * @see ColorHSVa
 * @see ColorXSLa
 */
@Serializable
data class ColorXSVa(val x: Double, val s: Double, val v: Double, override val alpha: Double = 1.0) :
    ColorModel<ColorXSVa>,
    ShadableColor<ColorXSVa>,
    HueShiftableColor<ColorXSVa>,
    SaturatableColor<ColorXSVa>,
    AlgebraicColor<ColorXSVa> {

    companion object {
        fun fromHSVa(hsva: ColorHSVa): ColorXSVa {
            val h = ((hsva.h % 360.0) + 360.0) % 360.0
            val x = if (0 <= h && h < 35) {
                map(h, 0.0, 35.0, 0.0, 60.0)
            } else if (35 <= h && h < 60) {
                map(h, 35.0, 60.0, 60.0, 120.0)
            } else if (60 <= h && h < 135.0) {
                map(h, 60.0, 135.0, 120.0, 180.0)
            } else if (135.0 <= h && h < 225.0) {
                map(h, 135.0, 225.0, 180.0, 240.0)
            } else if (225.0 <= h && h < 275.0) {
                map(h, 225.0, 275.0, 240.0, 300.0)
            } else {
                map(h, 275.0, 360.0, 300.0, 360.0)
            }
            return ColorXSVa(x, hsva.s, hsva.v, hsva.alpha)
        }
    }

    @Deprecated("Legacy alpha parameter name", ReplaceWith("alpha"))
    val a = alpha

    fun toHSVa(): ColorHSVa {
        val x = this.x % 360.0
        val h = if (0.0 <= x && x < 60.0) {
            map(x, 0.0, 60.0, 0.0, 35.0)
        } else if (60.0 <= x && x < 120.0) {
            map(x, 60.0, 120.0, 35.0, 60.0)
        } else if (120.0 <= x && x < 180.0) {
            map(x, 120.0, 180.0, 60.0, 135.0)
        } else if (180.0 <= x && x < 240.0) {
            map(x, 180.0, 240.0, 135.0, 225.0)
        } else if (240.0 <= x && x < 300.0) {
            map(x, 240.0, 300.0, 225.0, 275.0)
        } else {
            map(x, 300.0, 360.0, 275.0, 360.0)
        }
        return ColorHSVa(h, s, v, alpha)
    }

    override fun toRGBa() = toHSVa().toRGBa()

    override fun shiftHue(shiftInDegrees: Double) = copy(x = (x + shiftInDegrees))
    override fun withHue(hue: Double): ColorXSVa = copy(x = hue)

    override val hue: Double
        get() = x

    override fun withSaturation(saturation: Double): ColorXSVa = copy(s = saturation)

    override val saturation: Double
        get() = s

    override fun shade(factor: Double) = copy(v = v * factor)
    override fun opacify(factor: Double): ColorXSVa = copy(alpha = alpha * factor)

    override fun plus(right: ColorXSVa) = copy(
        x = x + right.x,
        s = s + right.s,
        v = v + right.v,
        alpha = alpha + right.alpha
    )
    override fun minus(right: ColorXSVa) = copy(
        x = x - right.x,
        s = s - right.s,
        v = v - right.v,
        alpha = alpha - right.alpha
    )
    override fun times(scale: Double) = copy(x = x * scale, s = s * scale, v = v * scale, alpha = alpha * scale)

    override fun mix(other: ColorXSVa, factor: Double) = mix(this, other, factor)

    override fun toVector4(): Vector4 = Vector4(x, s, v, alpha)
}

private fun map(x: Double, a: Double, b: Double, c: Double, d: Double): Double {
    return ((x - a) / (b - a)) * (d - c) + c
}

/**
 * Weighted mix between two colors in the XSV color space.
 *
 * @param x the weighting of colors, a value 0.0 is equivalent to [left],
 * 1.0 is equivalent to [right] and at 0.5 both colors contribute to the result equally
 * @return a mix of [left] and [right] weighted by [x]
 */
fun mix(left: ColorXSVa, right: ColorXSVa, x: Double): ColorXSVa {
    val sx = x.coerceIn(0.0, 1.0)
    return ColorXSVa(
        mixAngle(left.x, right.x, sx),
        (1.0 - sx) * left.s + sx * right.s,
        (1.0 - sx) * left.v + sx * right.v,
        (1.0 - sx) * left.alpha + sx * right.alpha
    )
}
