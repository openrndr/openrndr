package org.openrndr.color

import org.openrndr.math.CastableToVector4
import org.openrndr.math.Vector4
import kotlin.jvm.JvmOverloads

/**
 * The Yxy color space, also known as the
 * [xyY color space](https://en.wikipedia.org/wiki/CIE_1931_color_space#CIE_xy_chromaticity_diagram_and_the_CIE_xyY_color_space).
 * @param yy luminance of the color, in a range of 0.0 (darkest) to 100.0 (brightest)
 * @param x first chromaticity coordinate, in a range of 0.0 to 1.0
 * @param y second chromaticity coordinate, in a range of 0.0 to 1.0
 */
@Suppress("LocalVariableName")
data class ColorYxya @JvmOverloads constructor (
    val yy: Double,
    val x: Double,
    val y: Double,
    val a: Double = 1.0
) : CastableToVector4 {
    companion object {
        fun fromXYZa(xyza: ColorXYZa): ColorYxya {
            val s = xyza.x + xyza.y + xyza.z
            val yy = xyza.y
            val x = if (s > 0) xyza.x / s else 0.0
            val y = if (s > 0) xyza.y / s else 0.0
            return ColorYxya(yy, x, y, xyza.a)
        }
    }

    fun toXYZa(): ColorXYZa {
        val X = (yy / y) * x
        val Y = yy
        val Z = if (yy > 0) ((1.0 - x - y) * yy) / y else 0.0
        return ColorXYZa(X, Y, Z, a)
    }

    override fun toVector4(): Vector4 = Vector4(yy, x, y, a)
}

fun mix(a: ColorYxya, b: ColorYxya, x: Double): ColorYxya {
    return ColorYxya(
        a.yy * (1.0 - x) + b.yy * x,
        a.x * (1.0 - x) + b.x * x,
        a.y * (1.0 - x) + b.y * x,
        a.a * (1.0 - x) + b.a * x
    )
}