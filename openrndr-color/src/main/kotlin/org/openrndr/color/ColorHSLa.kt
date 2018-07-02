package org.openrndr.color


@Suppress("unused", "MemberVisibilityCanBePrivate")
data class ColorHSLa(val h: Double, val s: Double, val l: Double, val a: Double = 1.0) {

    override fun toString(): String {
        return "ColorHSL{" +
                "h=" + h +
                ", s=" + s +
                ", l=" + l +
                '}'
    }

    companion object {
        fun fromRGBa(rgb: ColorRGBa): ColorHSLa {

            val min = if (rgb.r <= rgb.b && rgb.r <= rgb.g) {
                rgb.r
            } else if (rgb.g <= rgb.b && rgb.g <= rgb.r) {
                rgb.g
            } else if (rgb.b <= rgb.r && rgb.b <= rgb.g) {
                rgb.b
            } else {
                0.0
            }

            val max: Double
            val maxArg: ColorRGBa.Component

            if (rgb.r >= rgb.b && rgb.r >= rgb.g) {
                maxArg = ColorRGBa.Component.R
                max = rgb.r
            } else if (rgb.g >= rgb.b && rgb.g >= rgb.r) {
                maxArg = ColorRGBa.Component.G
                max = rgb.g
            } else {
                maxArg = ColorRGBa.Component.B
                max = rgb.b
            }

            val l = (max + min) / 2.0
            val s: Double
            val h: Double
            if (max == min) {
                s = 0.0
                h = s
            } else {
                val d = max - min
                s = if (l > 0.5) d / (2.0 - max - min) else d / (max + min)
                h = when (maxArg) {
                    ColorRGBa.Component.R -> 60.0 * ((rgb.g - rgb.b) / d + if (rgb.g < rgb.b) 6 else 0)
                    ColorRGBa.Component.G -> 60.0 * ((rgb.b - rgb.r) / d + 2.0)
                    ColorRGBa.Component.B -> 60.0 * ((rgb.r - rgb.g) / d + 4.0)
                    ColorRGBa.Component.a -> 0.0
                }
            }
            return ColorHSLa(h, s, l, rgb.a)
        }
    }

    fun scaleHue(shift: Double): ColorHSLa = copy(h = (h + shift))
    fun shiftHue(shift: Double): ColorHSLa = copy(h = (h + shift))

    fun scaleSaturation(scale: Double): ColorHSLa = copy(s = s * scale)
    fun shiftSaturation(shift: Double): ColorHSLa = copy(s = s + shift)


    fun shiftValue(shift: Double): ColorHSLa = copy(l = l + shift)
    fun scaleValue(scale: Double): ColorHSLa = copy(l = l * scale)


    val unit get() = copy(h = ((h % 360) + 360) % 360)


    /*
    fun blendedWith(other: ColorHSL, blend: Double): ColorHSL {
        var sh = h
        var eh = other.h
        if (Math.abs(eh - sh) > 180) {
            if (eh > sh) {
                sh += 360.0
            } else {
                eh += 360.0
            }
        }
        return ColorHSL(sh * (1.0 - blend) + eh * blend,
                        s * (1.0 - blend) + other.s * blend,
                        l * (1.0 - blend) + other.l * blend)
    }
    */
    fun toRGBa(): ColorRGBa {
        return if (s == 0.0) {
            ColorRGBa(l, l, l, a)
        } else {
            val q = if (l < 0.5) l * (1 + s) else l + s - l * s
            val p = 2 * l - q
            val r = hue2rgb(p, q, h / 360.0 + 1.0 / 3)
            val g = hue2rgb(p, q, h / 360.0)
            val b = hue2rgb(p, q, h / 360.0 - 1.0 / 3)
            ColorRGBa(r, g, b, a, Linearity.SRGB)
        }

    }

    fun toHSVa(): ColorHSVa = toRGBa().toHSVa()
    fun toXYZa(): ColorXYZa = toRGBa().toXYZa()
    fun toLABa(ref: ColorXYZa = ColorXYZa.NEUTRAL) = toRGBa().toXYZa().toLABa(ref)
    fun toLUVa(ref: ColorXYZa = ColorXYZa.NEUTRAL) = toRGBa().toXYZa().toLUVa(ref)
    fun toLCHABa(ref: ColorXYZa = ColorXYZa.NEUTRAL) = toLABa(ref).toLCHABa()
    fun toLCHUVa(ref: ColorXYZa = ColorXYZa.NEUTRAL) = toLUVa(ref).toLCHUVa()

    fun toXSLa() = ColorXSLa.fromHSLa(this)

}

internal fun hue2rgb(p: Double, q: Double, ut: Double): Double {
    var t = ut
    while (t < 0) t += 1.0
    while (t > 1) t -= 1.0
    if (t < 1.0 / 6.0) return p + (q - p) * 6.0 * t
    if (t < 1.0 / 2.0) return q
    return if (t < 2.0 / 3.0) p + (q - p) * (2.0 / 3.0 - t) * 6.0 else p
}

