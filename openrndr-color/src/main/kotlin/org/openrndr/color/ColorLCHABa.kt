package org.openrndr.color


data class ColorLCHABa(val l: Double, val c: Double, val h: Double, val alpha: Double = 1.0, val ref: ColorXYZa = ColorXYZa.NEUTRAL) {

    companion object {
        fun findMaxChroma(l: Double, h: Double, ref: ColorXYZa): Double {
            var left = 0.0
            var right = 2000.0
            var bestGuess = left
            while (true) {

                if (right - left < 0.0001) {
                    return bestGuess
                }

                val leftTry = ColorLCHABa(l, left, h, 1.0, ref)
                val rightTry = ColorLCHABa(l, right, h, 1.0, ref)
                var middle = (left + right) / 2
                var middleTry = ColorLCHABa(l, middle, h, 1.0, ref)

                val leftValid = leftTry.toRGBa().let { it.minValue >= 0 && it.maxValue <= 1.0 }
                val rightValid = rightTry.toRGBa().let { it.minValue >= 0 && it.maxValue <= 1.0 }
                val middleValid = middleTry.toRGBa().let { it.minValue >= 0 && it.maxValue <= 1.0 }

                if (leftValid && middleValid && !rightValid) {
                    var newLeft = middle
                    var newRight = right
                    bestGuess = middle
                    left = newLeft
                    right = newRight
                }

                if (leftValid && !middleValid && !rightValid) {
                    val newLeft = left
                    var newRight = middle
                    left = newLeft
                    right = newRight
                }

                if (leftValid == middleValid && middleValid == rightValid) {
                    return bestGuess
                }
            }
        }


        fun fromLABa(laba: ColorLABa): ColorLCHABa {
            val l = laba.l
            val c = Math.sqrt(laba.a * laba.a + laba.b * laba.b)
            var h = Math.atan2(laba.b, laba.a)

            if (h < 0) {
                h += Math.PI * 2
            }

            h = Math.toDegrees(h)

            return ColorLCHABa(l, c, h, laba.alpha, laba.ref)
        }
    }

    fun scaleHue(scale: Double): ColorLCHABa = copy(h = (h * scale))
    fun shiftHue(shift: Double): ColorLCHABa = copy(h = (h + shift))

    fun scaleLuminosity(scale: Double): ColorLCHABa = copy(l = l * scale)
    fun shiftLuminosity(shift: Double): ColorLCHABa = copy(l = l + shift)

    fun shiftChroma(shift: Double): ColorLCHABa = copy(c = c + shift)
    fun scaleChroma(scale: Double): ColorLCHABa = copy(c = c * scale)


    fun toLABa(): ColorLABa {
        val a = c * Math.cos(Math.toRadians(h))
        val b = c * Math.sin(Math.toRadians(h))
        return ColorLABa(l, a, b, alpha, ref)
    }

    fun toXYZa(): ColorXYZa = toLABa().toXYZa()

    fun toRGBa(): ColorRGBa = toLABa().toXYZa().toRGBa()

    fun toLSHABa() = ColorLSHABa.fromLCHABa(this)
}

fun main(args: Array<String>) {
    val c = ColorLCHABa.findMaxChroma(60.3, 2.0, ColorXYZa.NEUTRAL)
    println(ColorLCHABa(60.3, c, 2.0, 1.0, ColorXYZa.NEUTRAL).toRGBa())

}