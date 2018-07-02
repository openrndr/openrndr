package org.openrndr.color

data class ColorXSLa(val x: Double, val s: Double, val l: Double, val alpha: Double) {

    companion object {
        fun fromHSLa(hsla: ColorHSLa): ColorXSLa {
            val h = ((hsla.h % 360.0) + 360.0) % 360.0
            val x = if (0 <= h && h < 35) {
                map(h, 0.0, 35.0, 0.0, 60.0)
            } else if (35 <= h && h < 60) {
                map(h, 35.0, 60.0, 60.0, 120.0)
            } else if (60 <= h && h < 135.0) {
                map(h, 60.0, 135.0, 120.0, 180.0)
            } else if (135.0 <= h && h < 225.0) {
                map(h, 135.0, 225.0, 180.0, 240.0)
            } else if (225.0 <= h && h < 275.0) {
                map(h, 225.0, 275.0, 240.0, 300.0)
            } else {
                map(h, 276.0, 360.0, 300.0, 360.0)
            }
            return ColorXSLa(x, hsla.s, hsla.l, hsla.a)
        }
    }

    fun toHSLa(): ColorHSLa {
        val x = this.x % 360.0
        val h = if (0.0 <= x && x < 60.0) {
            map(x, 0.0, 60.0, 0.0, 35.0)
        } else if (60.0 <= x && x < 120.0) {
            map(x, 60.0, 120.0, 35.0, 60.0)
        } else if (120.0 <= x && x < 180.0) {
            map(x, 120.0, 180.0, 60.0, 135.0)
        } else if (180.0 <= x && x < 240.0) {
            map(x, 180.0, 240.0, 135.0, 225.0)
        } else if (240.0 <= x && x < 300.0) {
            map(x, 240.0, 300.0, 225.0, 275.0)
        } else {
            map(x, 300.0, 360.0, 276.0, 360.0)
        }
        return ColorHSLa(h, s, l, alpha)
    }

    fun toRGBa() = toHSLa().toRGBa()
}

private fun map(x: Double, a: Double, b: Double, c: Double, d: Double): Double {
    return ((x - a) / (b - a)) * (d - c) + c
}