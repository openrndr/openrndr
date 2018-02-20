package org.openrndr.color

@Suppress("unused", "UNUSED_PARAMETER")
data class ColorLUVa(val l: Double, val u: Double, val v: Double, val alpha: Double = 1.0, val ref: ColorXYZa) {
    companion object {
        fun fromXYZa(xyz: ColorXYZa, ref: ColorXYZa): ColorLUVa {
            val y = xyz.y / ref.y
            val l = if (y <=  Math.pow(6.0/29.0, 3.0)) Math.pow(29.0/3.0,3.0) * y else
                116.0 * Math.pow(y, 1.0/3.0) - 16.0


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


        val up = u / ( 13* l) + ur
        val vp = v / (13 * l) + vr

        val y = if (l<= 8) ref.y  * l * Math.pow(3.0/29.0, 3.0) else ref.y * Math.pow((l+16)/116.0,3.0)
        val x = y * ((9*up)/(4*vp))
        val z = y * ( (12 - 3*up - 20*vp) / (4*vp))
        return ColorXYZa(x,y, z, alpha)
    }


    fun toRGBa(ref: ColorXYZa = this.ref): ColorRGBa = toXYZa().toRGBa()
    fun toHSVa(ref: ColorXYZa = this.ref): ColorHSVa = toXYZa().toRGBa().toHSVa()
    fun toHSLa(ref: ColorXYZa = this.ref): ColorHSLa = toXYZa().toRGBa().toHSLa()
    fun toLABa(ref: ColorXYZa = this.ref): ColorLABa = toXYZa().toLABa(ref)

    fun toLCHUVa(): ColorLCHUVa = ColorLCHUVa.fromLUVa(this)

}