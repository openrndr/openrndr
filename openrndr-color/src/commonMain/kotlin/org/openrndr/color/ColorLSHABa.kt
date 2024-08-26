package org.openrndr.color

import kotlinx.serialization.Serializable
import org.openrndr.math.Vector4
import org.openrndr.math.mixAngle

/**
 * Based on [ColorLCHABa], but
 * instead tries to use a normalized chroma.
 *
 * @see ColorLCHABa
 */
@Serializable
data class ColorLSHABa(
    val l: Double,
    val s: Double,
    val h: Double,
    override val alpha: Double = 1.0,
    override val ref: ColorXYZa = ColorXYZa.NEUTRAL
) : ColorModel<ColorLSHABa>, ReferenceWhitePoint, HueShiftableColor<ColorLSHABa>, SaturatableColor<ColorLSHABa>,
    LuminosityColor<ColorLSHABa>,
    AlgebraicColor<ColorLSHABa> {
    companion object {
        fun fromLCHABa(lcha: ColorLCHABa): ColorLSHABa {
            val maxC = ColorLCHABa.findMaxChroma(lcha.l, lcha.h, lcha.ref)
            return ColorLSHABa(lcha.l, lcha.c / maxC, lcha.h, lcha.alpha, lcha.ref)
        }
    }

    fun toLCHABa(): ColorLCHABa {
        val maxC = ColorLCHABa.findMaxChroma(l, h, ref)
        return ColorLCHABa(l, s * maxC, h, alpha, ref)
    }

    override fun toRGBa() = toLCHABa().toRGBa()

    override fun opacify(factor: Double): ColorLSHABa = copy(alpha = alpha * factor)

    override fun toVector4(): Vector4 = Vector4(l, s, h, alpha)
    override fun withHue(hue: Double): ColorLSHABa = copy(h = hue)
    override val hue: Double
        get() = h

    override fun withSaturation(saturation: Double): ColorLSHABa = copy(s = saturation)

    override val saturation: Double
        get() = s

    override fun plus(right: ColorLSHABa): ColorLSHABa =
        copy(l = l + right.l, s = s + right.s, h = h + right.h, alpha = alpha + right.alpha)

    override fun minus(right: ColorLSHABa): ColorLSHABa =
        copy(l = l - right.l, s = s - right.s, h = h - right.h, alpha = alpha - right.alpha)

    override fun times(scale: Double): ColorLSHABa =
        copy(l = l * scale, s = s * scale, h = h * scale, alpha = alpha * scale)

    override fun mix(other: ColorLSHABa, factor: Double): ColorLSHABa {
        val sx = factor.coerceIn(0.0, 1.0)
        return ColorLSHABa(
            (1.0 - sx) * l + sx * other.l,
            (1.0 - sx) * s + sx * other.s,
            mixAngle(h, other.h, sx),
            (1.0 - sx) * alpha + sx * other.alpha
        )
    }

    override fun withLuminosity(luminosity: Double): ColorLSHABa = copy(l = luminosity)

    override val luminosity: Double
        get() = l
}