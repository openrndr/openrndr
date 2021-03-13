package org.openrndr.color

import kotlin.math.pow

data class ColorLABa(
    val l: Double,
    val a: Double,
    val b: Double,
    val alpha: Double = 1.0,
    val ref: ColorXYZa = ColorXYZa.NEUTRAL
) :
    ConvertibleToColorRGBa,
    OpacifiableColor<ColorLABa>,
    ShadableColor<ColorLABa>,
    AlgebraicColor<ColorLABa> {

    companion object {
        fun fromXYZa(xyz: ColorXYZa, ref: ColorXYZa): ColorLABa {
            val x = xyz.x / ref.x
            val y = xyz.y / ref.y
            val z = xyz.z / ref.z

            val l = 116 * f(y) - 16.0
            val a = 500 * (f(x) - f(y))
            val b = 200 * (f(y) - f(z))

            return ColorLABa(l, a, b, xyz.a, ref)
        }

        fun fromRGBa(rgba: ColorRGBa, ref: ColorXYZa = ColorXYZa.NEUTRAL) =
            fromXYZa(ColorXYZa.fromRGBa(rgba), ref)
    }

    fun toXYZa(): ColorXYZa {
        var x: Double
        var y: Double
        var z: Double

        val lab = this

        val fy = (lab.l + 16.0) / 116.0
        val fx = lab.a / 500.0 + fy
        val fz = fy - lab.b / 200.0

        x = if (fx * fx * fx > 0.008856) {
            fx * fx * fx
        } else {
            (116 * fx - 16) / 903.3
        }

        y = if (lab.l > 903.3 * 0.008856) {
            ((lab.l + 16) / 116.0).pow(3.0)
        } else {
            lab.l / 903.3
        }

        z = if (fz * fz * fz > 0.008856) {
            fz * fz * fz
        } else {
            (116.0 * fz - 16.0) / 903.3
        }

        x *= ref.x
        y *= ref.y
        z *= ref.z
        return ColorXYZa(x, y, z, alpha)
    }

    fun toLCHABa() = ColorLCHABa.fromLABa(this)
    fun toLSHABa() = toLCHABa().toLSHABa()
    fun toLUVa() = toXYZa().toLUVa(ref)
    override fun toRGBa() = toXYZa().toRGBa()
    fun toHSVa() = toXYZa().toRGBa().toHSVa()
    fun toHSLa() = toXYZa().toRGBa().toHSLa()

    override fun opacify(factor: Double) = copy(alpha = alpha * factor)
    override fun shade(factor: Double) = copy(l = l * factor)

    override fun plus(right: ColorLABa) =
        copy(l = l + right.l, a = a + right.a, b = b + right.b, alpha = alpha + right.alpha)

    override fun minus(right: ColorLABa) =
        copy(l = l - right.l, a = a - right.a, b = b - right.b, alpha = alpha - right.alpha)

    override fun times(scale: Double): ColorLABa =
        copy(l = l * scale, a = a * scale, b = b * scale, alpha = alpha * scale)
}

private fun f(t: Double): Double {
    return if (t > 0.008856) {
        t.pow(1.0 / 3.0)
    } else {
        (903.3 * t + 16.0) / 116.0
    }
}
