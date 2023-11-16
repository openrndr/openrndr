package org.openrndr.color

import kotlinx.serialization.Serializable
import org.openrndr.math.Vector4
import kotlin.jvm.JvmOverloads

/**
 * Based on [ColorLCHABa], but
 * instead tries to use a normalized chroma.
 *
 * @see ColorLCHABa
 */
@Serializable
data class ColorLSHABa @JvmOverloads constructor (
    val l: Double,
    val s: Double,
    val h: Double,
    override val alpha: Double = 1.0,
    override val ref: ColorXYZa = ColorXYZa.NEUTRAL
) : ColorModel<ColorLSHABa>, ReferenceWhitePoint {
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
    fun scaleHue(scale: Double) = copy(h = h * scale)
    fun shiftHue(shift: Double) = copy(h = h + shift)
    fun scaleSaturation(scale: Double) = copy(s = s * scale)
    fun shiftSaturation(shift: Double) = copy(s = s + shift)
    fun scaleLuminosity(scale: Double) = copy(l = l * scale)
    fun shiftLuminosity(shift: Double) = copy(l = l + shift)

    val saturated get() = copy(s = s.coerceIn(0.0, 1.0))

    override fun toVector4(): Vector4 = Vector4(l, s, h, alpha)
}