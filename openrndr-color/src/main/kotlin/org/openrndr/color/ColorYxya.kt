package org.openrndr.color

data class ColorYxya(val yy: Double, val x: Double, val y: Double, val a: Double = 1.0) {

    companion object {
        fun fromXYZa(xyza: ColorXYZa): ColorYxya {
            val s = xyza.x + xyza.y + xyza.z
            val yy = xyza.y
            val x = if (s>0) xyza.x / s else 0.0
            val y = if (s>0) xyza.y / s else 0.0
            return ColorYxya(yy, x, y, xyza.a)
        }
    }

    fun toXYZa() : ColorXYZa {
        val X = (yy/y) * x
        val Y = yy
        val Z = if (yy >0) ((1.0-x-y)* yy) / y else 0.0
        return ColorXYZa(X, Y, Z, a)
    }
}

fun mix(a: ColorYxya, b:ColorYxya, x:Double) : ColorYxya{
    return ColorYxya(a.yy * (1.0-x) + b.yy * x,
            a.x * (1.0-x) + b.x * x,
            a.y * (1.0-x) + b.y * x,
            a.a * (1.0-x) + b.a * x)
}