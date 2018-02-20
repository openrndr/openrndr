package org.openrndr.color


data class ColorLSHABa(val l: Double, val s: Double, val h: Double, val alpha: Double = 1.0, val ref: ColorXYZa = ColorXYZa.NEUTRAL) {

    companion object {
        fun fromLCHABa(lcha: ColorLCHABa):ColorLSHABa {

           val maxC = ColorLCHABa.findMaxChroma(lcha.l, lcha.h, lcha.ref)
           return ColorLSHABa(lcha.l, lcha.c/maxC, lcha.h, lcha.alpha, lcha.ref)
        }
    }

    fun toLCHABa() : ColorLCHABa {
        val maxC = ColorLCHABa.findMaxChroma(l, h, ref)
        return ColorLCHABa(l, s * maxC, h, alpha, ref)
    }
    fun toRGBa() = toLCHABa().toRGBa()

    fun scaleHue(scale:Double) = copy(h = h * scale)
    fun shiftHue(shift:Double) = copy(h = h + shift)
    fun scaleSaturation(scale:Double) = copy(s = s * scale)
    fun shiftSaturation(shift:Double) = copy(s = s + shift)
    fun scaleLuminosity(scale:Double) = copy(l = l * scale)
    fun shiftLuminosity(shift:Double) = copy(l = l +shift)

    val saturated get() = copy(s = s.coerceIn(0.0, 1.0))
}

fun main(args: Array<String>) {
    val c = ColorLCHABa.findMaxChroma(60.3, 2.0, ColorXYZa.NEUTRAL)
    println(ColorLCHABa(60.3, c, 2.0, 1.0, ColorXYZa.NEUTRAL).toRGBa())

}