package org.openrndr.color

import kotlinx.serialization.Serializable
import org.openrndr.math.Vector4
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmRecord

/**
 * The Yxy color space, also known as the
 * [xyY color space](https://en.wikipedia.org/wiki/CIE_1931_color_space#CIE_xy_chromaticity_diagram_and_the_CIE_xyY_color_space).
 * @param yy luminance of the color, in a range of 0.0 (darkest) to 100.0 (brightest)
 * @param x first chromaticity coordinate, in a range of 0.0 to 1.0
 * @param y second chromaticity coordinate, in a range of 0.0 to 1.0
 * @param alpha alpha as a percentage between 0.0 and 1.0
 */
@Suppress("LocalVariableName")
@Serializable
@JvmRecord
data class ColorYxya(
    val yy: Double,
    val x: Double,
    val y: Double,
    override val alpha: Double = 1.0
) : ColorModel<ColorYxya> {
    companion object {
        fun fromXYZa(xyza: ColorXYZa): ColorYxya {
            val s = xyza.x + xyza.y + xyza.z
            val yy = xyza.y
            val x = if (s > 0) xyza.x / s else 0.0
            val y = if (s > 0) xyza.y / s else 0.0
            return ColorYxya(yy, x, y, xyza.alpha)
        }
    }

    fun toXYZa(): ColorXYZa {
        val X = (yy / y) * x
        val Y = yy
        val Z = if (yy > 0) ((1.0 - x - y) * yy) / y else 0.0
        return ColorXYZa(X, Y, Z, alpha)
    }

    override fun toRGBa(): ColorRGBa = toXYZa().toRGBa()
    override fun opacify(factor: Double): ColorYxya = copy(alpha = alpha * factor)
    override fun toVector4(): Vector4 = Vector4(yy, x, y, alpha)
}

fun mix(a: ColorYxya, b: ColorYxya, x: Double): ColorYxya {
    return ColorYxya(
        a.yy * (1.0 - x) + b.yy * x,
        a.x * (1.0 - x) + b.x * x,
        a.y * (1.0 - x) + b.y * x,
        a.alpha * (1.0 - x) + b.alpha * x
    )
}