package org.openrndr.color

import kotlinx.serialization.Serializable
import org.openrndr.math.Vector4
import org.openrndr.math.mixAngle
import kotlin.jvm.JvmOverloads

/**
 * Based on [ColorLCHUVa], but
 * instead tries to use a normalized chroma.
 *
 * @see ColorLCHUVa
 */
@Serializable
data class ColorLSHUVa @JvmOverloads constructor(
    val l: Double,
    val s: Double,
    val h: Double,
    override val alpha: Double = 1.0,
    override val ref: ColorXYZa = ColorXYZa.NEUTRAL
) : ColorModel<ColorLSHUVa>, ReferenceWhitePoint,
    HueShiftableColor<ColorLSHUVa>,
    SaturatableColor<ColorLSHUVa>,
    LuminosityColor<ColorLSHUVa>,
    AlgebraicColor<ColorLSHUVa> {
    companion object {
        fun fromLCHUVa(lcha: ColorLCHUVa): ColorLSHUVa {

            val maxC = ColorLCHUVa.findMaxChroma(lcha.l, lcha.h, lcha.ref)
            return ColorLSHUVa(lcha.l, lcha.c / maxC, lcha.h, lcha.alpha, lcha.ref)
        }
    }

    fun toLCHUVa(): ColorLCHUVa {
        val maxC = ColorLCHUVa.findMaxChroma(l, h, ref)
        return ColorLCHUVa(l, s * maxC, h, alpha, ref)
    }

    override fun toRGBa() = toLCHUVa().toRGBa()

    override fun opacify(factor: Double): ColorLSHUVa = copy(alpha = alpha * factor)

    override fun toVector4(): Vector4 = Vector4(l, s, h, alpha)
    override fun withHue(hue: Double): ColorLSHUVa = copy(h = hue)

    override val hue: Double
        get() = h
    override fun withSaturation(saturation: Double): ColorLSHUVa = copy(s = saturation)

    override val saturation
        get() = s
    override fun plus(right: ColorLSHUVa): ColorLSHUVa =
        copy(l = l + right.l, s = s + right.s, h = h + right.h, alpha = alpha + right.alpha)

    override fun minus(right: ColorLSHUVa): ColorLSHUVa =
        copy(l = l - right.l, s = s - right.s, h = h - right.h, alpha = alpha - right.alpha)

    override fun times(scale: Double): ColorLSHUVa =
        copy(l = l * scale, s = s * scale, h = h * scale, alpha = alpha * scale)

    override fun mix(other: ColorLSHUVa, factor: Double): ColorLSHUVa {
        val sx = factor.coerceIn(0.0, 1.0)
            return ColorLSHUVa(
                (1.0 - sx) * l + sx * other.l,
                (1.0 - sx) * s + sx * other.s,
                mixAngle(h, other.h, sx),
                (1.0 - sx) * alpha + sx * other.alpha
            )
        }

    override fun withLuminosity(luminosity: Double): ColorLSHUVa = copy(l = luminosity)

    override val luminosity: Double
        get() = l
}
