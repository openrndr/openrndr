package org.openrndr.color




class ColorLABa(val l:Double, val a:Double, val b:Double, val alpha:Double=1.0, val ref:ColorXYZa= ColorXYZa.NEUTRAL) {

    companion object {
        fun fromXYZa(xyz: ColorXYZa, ref:ColorXYZa) : ColorLABa {
            val x = xyz.x / ref.x
            val y = xyz.y / ref.y
            val z = xyz.z / ref.z

            val l = 116 * f(y) - 16.0
            val a = 500 * (f(x) - f(y))
            val b = 200 * (f(y) - f(z))

            return ColorLABa(l, a, b, xyz.a, ref)
        }

        fun fromRGBa(rgba: ColorRGBa, ref:ColorXYZa = ColorXYZa.NEUTRAL) =
                fromXYZa(ColorXYZa.fromRGBa(rgba), ref)
    }

    fun toXYZa():ColorXYZa {
        var x: Double
        var y: Double
        var z: Double

        val lab = this

        val fy = (lab.l + 16.0) / 116.0
        val fx = lab.a / 500.0 + fy
        val fz = fy - lab.b / 200.0

        if (fx * fx * fx > 0.008856) {
            x = fx * fx * fx
        } else {
            x = (116 * fx - 16) / 903.3
        }

        if (lab.l > 903.3 * 0.008856) {
            y = Math.pow((lab.l + 16) / 116.0, 3.0)
        } else {
            y = lab.l / 903.3
        }
        if (fz * fz * fz > 0.008856) {
            z = fz * fz * fz
        } else {
            z = (116.0 * fz - 16.0) / 903.3
        }

        x = x * ref.x
        y = y * ref.y
        z = z * ref.z
        return ColorXYZa(x, y, z, alpha)
    }
    fun toLCHABa() = ColorLCHABa.fromLABa(this)
    fun toLSHABa() = toLCHABa().toLSHABa()
    fun toLUVa() = toXYZa().toLUVa(ref)
    fun toRGBa() = toXYZa().toRGBa()
    fun toHSVa() = toXYZa().toRGBa().toHSVa()
    fun toHSLa() = toXYZa().toRGBa().toHSLa()

}

private fun f(t: Double): Double {
    return if (t > 0.008856) {
        Math.pow(t, 1.0 / 3.0)
    } else {
        (903.3 * t + 16.0) / 116.0 // (1.0/3.0) * Math.pow(29.0 / 6.0, 2.0) * t + 4.0/29.0;
    }
}
