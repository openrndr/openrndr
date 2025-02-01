package org.openrndr.color

import kotlinx.serialization.Serializable
import org.openrndr.math.Vector4
import org.openrndr.math.mixAngle

/**
 * Practically identical to [ColorHSLa], but
 * for mapping colors to classical painter's scheme
 * of complementary colors.
 *
 * @see ColorHSLa
 * @see ColorXSVa
 */
@Serializable
data class ColorXSLa(val x: Double, val s: Double, val l: Double, override val alpha: Double = 1.0) :
    ColorModel<ColorXSLa>,
    ShadableColor<ColorXSLa>,
    HueShiftableColor<ColorXSLa>,
    SaturatableColor<ColorXSLa>,
    AlgebraicColor<ColorXSLa> {

    companion object {
        /**
         * Converts a `ColorHSLa` instance to a `ColorXSLa` instance.
         *
         * @param hsla The source color in the HSLa color space.
         * @return A new `ColorXSLa` instance representing the converted color.
         */
        fun fromHSLa(hsla: ColorHSLa): ColorXSLa = ColorXSLa(hueToXue(hsla.h), hsla.s, hsla.l, hsla.alpha)
    }

    @Deprecated("Legacy alpha parameter name", ReplaceWith("alpha"))
    val a = alpha

    /**
     * Converts the current `ColorXSLa` instance to the `ColorHSLa` representation (Hue, Saturation, Lightness, Alpha).
     *
     * @return A new `ColorHSLa` instance with the hue calculated based on the `x` value, and the other components retained as is.
     */
    fun toHSLa(): ColorHSLa = ColorHSLa(xueToHue(x), s, l, alpha)

    override fun toRGBa() = toHSLa().toRGBa()

    override fun shiftHue(shiftInDegrees: Double) = copy(x = (x + shiftInDegrees))
    override fun withHue(hue: Double) = copy(x = hue)

    override val hue: Double
        get() = x

    override fun withSaturation(saturation: Double): ColorXSLa = copy(s = saturation)

    override val saturation: Double
        get() = s

    override fun shade(factor: Double) = copy(l = l * factor)
    override fun opacify(factor: Double) = copy(alpha = alpha * factor)

    override fun plus(right: ColorXSLa) = copy(
        x = x + right.x,
        s = s + right.s,
        l = l + right.l,
        alpha = alpha + right.alpha
    )

    override fun minus(right: ColorXSLa) = copy(
        x = x - right.x,
        s = s - right.s,
        l = l - right.l,
        alpha = alpha - right.alpha
    )

    override fun times(scale: Double) = copy(x = x * scale, s = s * scale, l = l * scale, alpha = alpha * scale)

    override fun mix(other: ColorXSLa, factor: Double) = mix(this, other, factor)

    override fun toVector4(): Vector4 = Vector4(x, s, l, alpha)
}

/**
 * Weighted mix between two colors in the XSL color space.
 *
 * @param x the weighting of colors, a value 0.0 is equivalent to [left],
 * 1.0 is equivalent to [right] and at 0.5 both colors contribute to the result equally
 * @return a mix of [left] and [right] weighted by [x]
 */
fun mix(left: ColorXSLa, right: ColorXSLa, x: Double): ColorXSLa {
    val sx = x.coerceIn(0.0, 1.0)
    return ColorXSLa(
        mixAngle(left.x, right.x, sx),
        (1.0 - sx) * left.s + sx * right.s,
        (1.0 - sx) * left.l + sx * right.l,
        (1.0 - sx) * left.alpha + sx * right.alpha
    )
}
