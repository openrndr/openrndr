package org.openrndr.color

import org.openrndr.math.mixAngle

data class ColorXSVa(val x: Double, val s: Double, val v: Double, val a: Double = 1.0) :
        ConvertibleToColorRGBa,
        ShadableColor<ColorXSVa>,
        HueShiftableColor<ColorXSVa>,
        SaturatableColor<ColorXSVa>,
        OpacifiableColor<ColorXSVa>,
        AlgebraicColor<ColorXSVa> {

    companion object {
        fun fromHSVa(hsva: ColorHSVa): ColorXSVa {
            val h = ((hsva.h % 360.0) + 360.0) % 360.0
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
            return ColorXSVa(x, hsva.s, hsva.v, hsva.a)
        }
    }

    fun toHSVa(): ColorHSVa {
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
        return ColorHSVa(h, s, v, a)
    }

    override fun toRGBa() = toHSVa().toRGBa()

    override fun shiftHue(shiftInDegrees: Double) = copy(x = (x + shiftInDegrees))
    override fun saturate(factor: Double) = copy(s = s * factor)
    override fun shade(factor: Double) = copy(v = v * factor)
    override fun opacify(factor: Double): ColorXSVa = copy(a = a * factor)

    override fun plus(other: ColorXSVa) = copy(x = x + other.x, s = s + other.s, v = v + other.v, a = a + other.a)
    override fun minus(other: ColorXSVa) = copy(x = x - other.x, s = s - other.s, v = v - other.v, a = a - other.a)
    override fun times(factor: Double) = copy(x = x * factor, s = s * factor, v = v * factor, a = a * factor)

    override fun mix(other: ColorXSVa, factor: Double) = mix(this, other, factor)
}

private fun map(x: Double, a: Double, b: Double, c: Double, d: Double): Double {
    return ((x - a) / (b - a)) * (d - c) + c
}

/**
 * Mixes two colors in XSVa space
 * @param left the left hand ColorXSVa color
 * @param right the right hand ColorXSVa
 * @param x the mix amount
 * @return a mix of [left] and [right], x == 0.0 corresponds with left, x == 1.0 corresponds with right
 */
fun mix(left: ColorXSVa, right: ColorXSVa, x: Double): ColorXSVa {
    val sx = x.coerceIn(0.0, 1.0)
    return ColorXSVa(
            mixAngle(left.x, right.x, sx),
            (1.0 - sx) * left.s + sx * right.s,
            (1.0 - sx) * left.v + sx * right.v,
            (1.0 - sx) * left.a + sx * right.a)
}
