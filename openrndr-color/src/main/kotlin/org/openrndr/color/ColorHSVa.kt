@file:Suppress("unused")

package org.openrndr.color


/**
 * A color respresentation in HSVa space
 *
 * @param h hue in n * [0, 360)
 * @param s value in [0, 1]
 * @param v value in [0, 1]
 * @param a alpha in [0, 1]
 */
@Suppress("unused")
data class ColorHSVa(val h: Double, val s: Double, val v: Double, val a: Double = 1.0) {

    companion object {
        fun fromRGBa(rgb: ColorRGBa): ColorHSVa {
            var min = Double.POSITIVE_INFINITY
            var max = Double.NEGATIVE_INFINITY

            var h: Double
            var maxArg: ColorRGBa.Component? = null

            if (rgb.r <= rgb.b && rgb.r <= rgb.g) {
                min = rgb.r
            }
            if (rgb.g <= rgb.b && rgb.g <= rgb.r) {
                min = rgb.g
            }
            if (rgb.b <= rgb.r && rgb.b <= rgb.g) {
                min = rgb.b
            }

            if (rgb.r >= rgb.b && rgb.r >= rgb.g) {
                maxArg = ColorRGBa.Component.R
                max = rgb.r
            }
            if (rgb.g >= rgb.b && rgb.g >= rgb.r) {
                maxArg = ColorRGBa.Component.G
                max = rgb.g
            }
            if (rgb.b >= rgb.r && rgb.b >= rgb.g) {
                maxArg = ColorRGBa.Component.B
                max = rgb.b
            }

            val s: Double
            val v = max
            val delta = max - min
            if (max != 0.0) {
                s = delta / max
            } else {
                // r = g = b = 0		// s = 0, v is undefined
                s = 0.0
                h = 0.0
                return ColorHSVa(h, s, v, rgb.a)
            }
            if (maxArg == ColorRGBa.Component.R) {
                h = (rgb.g - rgb.b) / delta        // between yellow & magenta
            } else if (maxArg == ColorRGBa.Component.G) {
                h = 2 + (rgb.b - rgb.r) / delta    // between cyan & yellow
            } else {
                h = 4 + (rgb.r - rgb.g) / delta    // between magenta & cyan
            }
            h *= 60.0                // degrees
            if (h < 0) {
                h += 360.0
            }
            return ColorHSVa(h, s, v, rgb.a)
        }
    }


    fun scaleHue(shift: Double): ColorHSVa = copy(h = (h + shift))
    fun shiftHue(shift: Double): ColorHSVa = copy(h = (h + shift))

    fun scaleSaturation(scale: Double): ColorHSVa = copy(s = s * scale)
    fun shiftSaturation(shift: Double): ColorHSVa = copy(s = s + shift)


    fun shiftValue(shift: Double): ColorHSVa = copy(v = v + shift)
    fun scaleValue(scale: Double): ColorHSVa = copy(v = v * scale)

    /**
     * a unit presentation of this ColorHSVa, essentially brings the hue back in [0, 360)
     * @return a copy with the hue value in [0, 360)
     */
    val unit get() = copy(h = ((h % 360) + 360) % 360)

    fun toRGBa(): ColorRGBa {
        val i: Int
        val f: Double

        val r: Double
        val g: Double
        val b: Double
        val hsv = this

        val sh = hsv.h / 60            // sector 0 to 5
        i = Math.floor(sh).toInt()
        f = sh - i            // factorial part of h
        val p = hsv.v * (1 - hsv.s)
        val q = hsv.v * (1 - hsv.s * f)
        val t = hsv.v * (1 - hsv.s * (1 - f))
        when (i) {
            0 -> {
                r = hsv.v
                g = t
                b = p
            }
            1 -> {
                r = q
                g = hsv.v
                b = p
            }
            2 -> {
                r = p
                g = hsv.v
                b = t
            }
            3 -> {
                r = p
                g = q
                b = hsv.v
            }
            4 -> {
                r = t
                g = p
                b = hsv.v
            }
            else        // case 5:
            -> {
                r = hsv.v
                g = p
                b = q
            }
        }
        return ColorRGBa(r, g, b, hsv.a, Linearity.SRGB)
    }
}

/**
 * Mixes two colors in HSVa space
 * @param left the left hand ColorHSVa color
 * @param right the right hand ColorHSVa
 * @param x the mix amount
 * @return a mix of [left] and [right], x == 0.0 corresponds with left, x == 1.0 corresponds with right
 */
fun mix(left: ColorHSVa, right: ColorHSVa, x: Double): ColorHSVa {
    val sx = x.coerceIn(0.0, 1.0)
    return ColorHSVa(
            (1.0 - sx) * left.h + sx * right.h,
            (1.0 - sx) * left.s + sx * right.s,
            (1.0 - sx) * left.v + sx * right.v,
            (1.0 - sx) * left.a + sx * right.a)
}