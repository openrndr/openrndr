package org.openrndr.color

data class ColorLCHUVa(val l: Double, val c: Double, val h: Double, val alpha: Double = 1.0, val ref: ColorXYZa = ColorXYZa.NEUTRAL) {

    companion object {
        fun fromLUVa(luva: ColorLUVa): ColorLCHUVa {
            val l = luva.l
            val c = Math.sqrt(luva.u * luva.u + luva.v * luva.v)
            var h = Math.atan2(luva.v, luva.u)

            if (h < 0) {
                h += Math.PI * 2
            }
            h = Math.toDegrees(h)
            return ColorLCHUVa(l, c, h, luva.alpha, luva.ref)
        }

        fun findMaxChroma(l: Double, h: Double, ref: ColorXYZa): Double {
            var left = 0.0
            var right = 2000.0
            var bestGuess = left
            while (true) {

                if (right - left < 0.0001) {
                    return bestGuess
                }

                val leftTry = ColorLCHUVa(l, left, h, 1.0, ref)
                val rightTry = ColorLCHUVa(l, right, h, 1.0, ref)
                var middle = (left + right) / 2
                var middleTry = ColorLCHUVa(l, middle, h, 1.0, ref)

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
    }

    fun scaleHue(shift: Double): ColorLCHUVa = copy(h = (h + shift))
    fun shiftHue(shift: Double): ColorLCHUVa = copy(h = (h + shift))

    fun scaleLuminosity(scale: Double): ColorLCHUVa = copy(l = l * scale)
    fun shiftLuminosity(shift: Double): ColorLCHUVa = copy(l = l + shift)

    fun shiftChroma(shift: Double): ColorLCHUVa = copy(c = c + shift)
    fun scaleChroma(scale: Double): ColorLCHUVa = copy(c = c * scale)

    fun toLUVa(): ColorLUVa {
        val u = c * Math.cos(Math.toRadians(h))
        val v = c * Math.sin(Math.toRadians(h))
        return ColorLUVa(l, u, v, alpha, ref)
    }

    fun toLSHUVa() = ColorLSHUVa.fromLCHUVa(this)
    fun toRGBa() = toLUVa().toRGBa()
}