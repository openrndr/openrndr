package org.openrndr.color


data class ColorLSHUVa(val l: Double, val s: Double, val h: Double, val alpha: Double = 1.0, val ref: ColorXYZa = ColorXYZa.NEUTRAL) {

    companion object {
        fun fromLCHUVa(lcha: ColorLCHUVa): ColorLSHUVa {

            val maxC = ColorLCHUVa.findMaxChroma(lcha.l, lcha.h, lcha.ref)
            return ColorLSHUVa(lcha.l, lcha.c / maxC, lcha.h, lcha.alpha, lcha.ref)
        }
    }

    fun toLCHUVa(): ColorLCHUVa {
        val maxC = ColorLCHUVa.findMaxChroma(l, h, ref)
        return ColorLCHUVa(l, s * maxC, h, alpha, ref)
    }

    fun toRGBa() = toLCHUVa().toRGBa()

    fun scaleHue(scale: Double) = copy(h = h * scale)
    fun shiftHue(shift: Double) = copy(h = h + shift)
    fun scaleSaturation(scale: Double) = copy(s = s * scale)
    fun shiftSaturation(shift: Double) = copy(s = s + shift)
    fun scaleLuminosity(scale: Double) = copy(l = l * scale)
    fun shiftLuminosity(shift: Double) = copy(l = l + shift)

    val saturated get() = copy(s = s.coerceIn(0.0, 1.0))
}
