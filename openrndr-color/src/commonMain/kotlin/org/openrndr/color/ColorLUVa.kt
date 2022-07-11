package org.openrndr.color

import org.openrndr.math.Vector4
import kotlin.jvm.JvmOverloads
import kotlin.math.pow

/**
 * The [CIELUV color space](https://en.wikipedia.org/wiki/CIELUV)
 * @param l luminance, in a range of 0.0 (darkest) to 100.0 (brightest)
 * @param u unbounded chromaticity coordinate U
 * @param v unbounded chromaticity coordinate V
 * @param alpha alpha as a percentage between 0.0 and 1.0
 * @param ref reference white against which the color values are calculated
 */
@Suppress("unused", "UNUSED_PARAMETER")
data class ColorLUVa @JvmOverloads constructor (val l: Double, val u: Double, val v: Double, override val alpha: Double = 1.0, val ref: ColorXYZa) :
    ColorModel<ColorLUVa>,
    ShadableColor<ColorLUVa>,
    AlgebraicColor<ColorLUVa> {

    companion object {
        fun fromXYZa(xyz: ColorXYZa, ref: ColorXYZa): ColorLUVa {
            val y = if (ref.y != 0.0) xyz.y / ref.y else 0.0
            val l = if (y <= (6.0 / 29.0).pow(3.0)) (29.0 / 3.0).pow(3.0) * y else
                116.0 * y.pow(1.0 / 3.0) - 16.0


            val div0 = (xyz.x + xyz.y * 15.0 + xyz.z * 3.0)

            val up = if (div0 ==0.0) 0.0 else (xyz.x * 4.0) / div0
            val vp = if (div0 == 0.0) 0.0 else (xyz.y * 9.0) / div0

            val div1 =  (ref.x + ref.y * 15 + ref.z * 3.0)
            val ur = if (div1 == 0.0) 0.0 else (ref.x * 4.0) / div1
            val vr = if (div1 == 0.0) 0.0 else (ref.y * 9.0) / div1

            val u = 13.0 * l * (up - ur)
            val v = 13.0 * l * (vp - vr)

            return ColorLUVa(l, u, v, xyz.alpha, ref)
        }

        fun fromRGBa(rgba: ColorRGBa, ref: ColorXYZa = ColorXYZa.NEUTRAL): ColorLUVa {
            return fromXYZa(ColorXYZa.fromRGBa(rgba), ref)
        }
    }

    fun toXYZa(): ColorXYZa {
        val divr = (ref.x + ref.y * 15 + ref.z * 3.0)
        val ur = if (divr == 0.0) 0.0 else (ref.x * 4.0) / divr
        val vr = if (divr == 0.0) 0.0 else (ref.y * 9.0) / divr

        val divp = 13 * l
        val up = if (divp == 0.0) 0.0 else u / divp + ur
        val vp = if (divp == 0.0) 0.0 else v / divp + vr

        val y = if (l <= 8) ref.y * l * (3.0 / 29.0).pow(3.0) else ref.y * ((l + 16) / 116.0).pow(3.0)
        val x = if (vp == 0.0) 0.0 else  y * ((9 * up) / (4 * vp))
        val z = if (vp == 0.0) 0.0 else y * ((12 - 3 * up - 20 * vp) / (4 * vp))
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