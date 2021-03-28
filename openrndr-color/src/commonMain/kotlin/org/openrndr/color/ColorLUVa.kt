package org.openrndr.color

import org.openrndr.math.CastableToVector4
import org.openrndr.math.Vector4
import kotlin.math.pow

@Suppress("unused", "UNUSED_PARAMETER")
data class ColorLUVa(val l: Double, val u: Double, val v: Double, val alpha: Double = 1.0, val ref: ColorXYZa) :
    ConvertibleToColorRGBa,
    CastableToVector4,
    OpacifiableColor<ColorLUVa>,
    ShadableColor<ColorLUVa>,
    AlgebraicColor<ColorLUVa> {

    companion object {
        fun fromXYZa(xyz: ColorXYZa, ref: ColorXYZa): ColorLUVa {
            val y = xyz.y / ref.y
            val l = if (y <= (6.0 / 29.0).pow(3.0)) (29.0 / 3.0).pow(3.0) * y else
                116.0 * y.pow(1.0 / 3.0) - 16.0


            val up = (xyz.x * 4.0) / (xyz.x + xyz.y * 15.0 + xyz.z * 3.0)
            val vp = (xyz.y * 9.0) / (xyz.x + xyz.y * 15.0 + xyz.z * 3.0)

            val ur = (ref.x * 4.0) / (ref.x + ref.y * 15 + ref.z * 3.0)
            val vr = (ref.y * 9.0) / (ref.x + ref.y * 15 + ref.z * 3.0)

            val u = 13.0 * l * (up - ur)
            val v = 13.0 * l * (vp - vr)

            return ColorLUVa(l, u, v, xyz.a, ref)
        }

        fun fromRGBa(rgba: ColorRGBa, ref: ColorXYZa = ColorXYZa.NEUTRAL): ColorLUVa {
            return fromXYZa(ColorXYZa.fromRGBa(rgba), ref)
        }
    }

    fun toXYZa(): ColorXYZa {

        val ur = (ref.x * 4.0) / (ref.x + ref.y * 15 + ref.z * 3.0)
        val vr = (ref.y * 9.0) / (ref.x + ref.y * 15 + ref.z * 3.0)


        val up = u / (13 * l) + ur
        val vp = v / (13 * l) + vr

        val y = if (l <= 8) ref.y * l * (3.0 / 29.0).pow(3.0) else ref.y * ((l + 16) / 116.0).pow(3.0)
        val x = y * ((9 * up) / (4 * vp))
        val z = y * ((12 - 3 * up - 20 * vp) / (4 * vp))
        return ColorXYZa(x, y, z, alpha)
    }


    override fun toRGBa() = toXYZa().toRGBa()
    fun toRGBa(ref: ColorXYZa = this.ref): ColorRGBa = toXYZa().toRGBa()
    fun toHSVa(ref: ColorXYZa = this.ref): ColorHSVa = toXYZa().toRGBa().toHSVa()
    fun toHSLa(ref: ColorXYZa = this.ref): ColorHSLa = toXYZa().toRGBa().toHSLa()
    fun toLABa(ref: ColorXYZa = this.ref): ColorLABa = toXYZa().toLABa(ref)

    fun toLCHUVa(): ColorLCHUVa = ColorLCHUVa.fromLUVa(this)

    override fun opacify(factor: Double) = copy(alpha = alpha * factor)
    override fun shade(factor: Double) = copy(l = l * factor)

    override fun plus(right: ColorLUVa) =
        copy(l = l + right.l, u = u + right.u, v = v + right.v, alpha = alpha + right.alpha)

    override fun minus(right: ColorLUVa) =
        copy(l = l - right.l, u = u - right.u, v = v - right.v, alpha = alpha - right.alpha)

    override fun times(scale: Double) = copy(l = l * scale, u = u * scale, v = v * scale, alpha = alpha * scale)

    override fun toVector4() = Vector4(l, u, v, alpha)
}