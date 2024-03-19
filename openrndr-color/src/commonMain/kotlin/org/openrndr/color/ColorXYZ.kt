package org.openrndr.color

import kotlinx.serialization.Serializable
import org.openrndr.math.Vector4
import kotlin.jvm.JvmOverloads
import kotlin.math.min

/**
 * The [CIE XYZ color space](https://en.wikipedia.org/wiki/CIE_1931_color_space#Definition_of_the_CIE_XYZ_color_space).
 *
 * @param x first chromaticity coordinate, mix of the three CIE RGB curves chosen to be nonnegative
 * @param y luminance of the color, in a range of 0.0 (darkest) to 1.0 (brightest)
 * @param z second chromaticity coordinate, quasi-equal to blue
 * @param alpha alpha as a percentage between 0.0 and 1.0
 */
@Serializable
data class ColorXYZa(val x: Double, val y: Double, val z: Double, override val alpha: Double = 1.0) :
    ColorModel<ColorXYZa>,
    AlgebraicColor<ColorXYZa> {

    @Suppress("unused")
    companion object {
        // White points of standard illuminants as per the CIE 1931 2° Standard Observer
        // Also see: https://en.wikipedia.org/wiki/CIE_1931_color_space#CIE_standard_observer,
        // https://en.wikipedia.org/wiki/Standard_illuminant#White_points_of_standard_illuminants
        val SO2_A = ColorXYZa(109.83 / 100.0, 1.0, 35.55 / 100.0)
        val SO2_C = ColorXYZa(98.04 / 100.0, 1.0, 118.11 / 100.0)
        val SO2_F2 = ColorXYZa(98.09 / 100.0, 1.0, 67.53 / 100.0)
        val SO2_TL4 = ColorXYZa(101.40 / 100.0, 1.0, 65.90 / 100.0)
        val SO2_UL3000 = ColorXYZa(107.99 / 100.0, 1.0, 33.91 / 100.0)
        val SO2_D50 = ColorXYZa(107.99 / 100.0, 1.0, 82.45 / 100.0)
        val SO2_D60 = ColorXYZa(107.99 / 100.0, 1.0, 100.86 / 100.0)
        val SO2_D65 = ColorXYZa(95.02 / 100.0, 1.0, 108.82 / 100.0)
        val SO2_D75 = ColorXYZa(107.99 / 100.0, 1.0, 122.53 / 100.0)

        // White points of standard illuminants as per the CIE 1963 10° Standard Observer
        val SO10_A = ColorXYZa(111.16 / 100.0, 1.0, 35.19 / 100.0)
        val SO10_C = ColorXYZa(97.30 / 100.0, 1.0, 116.14 / 100.0)
        val SO10_F2 = ColorXYZa(102.12 / 100.0, 1.0, 69.37 / 100.0)
        val SO10_TL4 = ColorXYZa(103.82 / 100.0, 1.0, 66.90 / 100.0)
        val SO10_UL3000 = ColorXYZa(111.12 / 100.0, 1.0, 35.21 / 100.0)
        val SO10_D50 = ColorXYZa(96.72 / 100.0, 1.0, 81.45 / 100.0)
        val SO10_D60 = ColorXYZa(95.21 / 100.0, 1.0, 99.60 / 100.0)
        val SO10_D65 = ColorXYZa(94.83 / 100.0, 1.0, 107.38 / 100.0)
        val SO10_D75 = ColorXYZa(94.45 / 100.0, 1.0, 120.70 / 100.0)

        val NEUTRAL = fromRGBa(ColorRGBa(1.0, 1.0, 1.0, linearity = Linearity.LINEAR))

        fun fromRGBa(rgba: ColorRGBa): ColorXYZa {
            val linear = rgba.toLinear()
            val x = 0.4124 * linear.r + 0.3576 * linear.g + 0.1805 * linear.b
            val y = 0.2126 * linear.r + 0.7152 * linear.g + 0.0722 * linear.b
            val z = 0.0193 * linear.r + 0.1192 * linear.g + 0.9505 * linear.b
            return ColorXYZa(x, y, z, linear.alpha)
        }
    }

    @Deprecated("Legacy alpha parameter name", ReplaceWith("alpha"))
    val a = alpha

    val minValue get() = min(min(x, y), z)

    fun toLABa(ref: ColorXYZa) = ColorLABa.fromXYZa(this, ref)
    fun toLUVa(ref: ColorXYZa) = ColorLUVa.fromXYZa(this, ref)
    override fun toRGBa(): ColorRGBa {
        val r = 3.2406 * x - 1.5372 * y - 0.4986 * z
        val g = -0.9689 * x + 1.8758 * y + 0.0415 * z
        val b = 0.0557 * x - 0.2040 * y + 1.0570 * z
        return ColorRGBa(r, g, b, alpha, Linearity.LINEAR)
    }

    fun toHSVa(): ColorHSVa = toRGBa().toHSVa()
    fun toHSLa(): ColorHSLa = toRGBa().toHSLa()
    override fun plus(right: ColorXYZa) = copy(
        x = x + right.x,
        y = y + right.y,
        z = z + right.z,
        alpha = alpha + right.alpha
    )
    override fun minus(right: ColorXYZa) = copy(
        x = x - right.x,
        y = y - right.y,
        z = z - right.z,
        alpha = alpha - right.alpha
    )
    override fun times(scale: Double): ColorXYZa = copy(
        x = x * scale,
        y = y * scale,
        z = z * scale,
        alpha = alpha * scale
    )
    override fun opacify(factor: Double) = copy(alpha = alpha * factor)

    override fun toVector4() = Vector4(x, y, z, alpha)
}