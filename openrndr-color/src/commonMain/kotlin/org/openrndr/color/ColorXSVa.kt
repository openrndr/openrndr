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
        /**
         * Converts a color from the HSVa (Hue, Saturation, Value, Alpha) color space to the XSVa
         * (custom-defined X, Saturation, Value, Alpha) color space.
         *
         * The conversion maps the hue value from [0, 360) degrees in the HSVa color space to a custom-defined
         * "X" scale based on specific intervals.
         *
         * @param hsva The input color in the HSVa color space, represented as a [ColorHSVa] object.
         * @return The converted color in the XSVa color space, represented as a [ColorXSVa] object.
         */
        fun fromHSVa(hsva: ColorHSVa): ColorXSVa = ColorXSVa(hueToXue(hsva.hue), hsva.s, hsva.v, hsva.alpha)
    }

    @Deprecated("Legacy alpha parameter name", ReplaceWith("alpha"))
    val a = alpha

    /**
     * Converts this `ColorXSVa` instance to a `ColorHSVa` representation.
     *
     * The conversion maps the hue (`x`) from the `ColorXSVa` space to the equivalent hue in the
     * `ColorHSVa` space while keeping the saturation (`s`), value (`v`), and alpha (`alpha`) unchanged.
     *
     * @return A `ColorHSVa` representing the corresponding color in the HSV color model.
     */
    fun toHSVa(): ColorHSVa = ColorHSVa(xueToHue(x), s, v, alpha)

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
