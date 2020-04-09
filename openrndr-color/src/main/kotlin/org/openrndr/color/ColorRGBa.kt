package org.openrndr.color

import org.openrndr.math.Vector3
import org.openrndr.math.Vector4
import java.lang.NumberFormatException
import kotlin.math.pow

enum class Linearity {
    UNKNOWN,
    LINEAR,
    SRGB,
    ASSUMED_LINEAR,
    ASSUMED_SRGB
}

/**
 * Color in RGBa space
 *
 * @param r red in [0,1]
 * @param g green in [0,1]
 * @param b blue in [0,1]
 * @param a alpha in [0,1]
 */
data class ColorRGBa(val r: Double, val g: Double, val b: Double, val a: Double = 1.0, val linearity: Linearity = Linearity.UNKNOWN) {

    operator fun invoke(r: Double = this.r, g: Double = this.g, b: Double = this.b, a: Double = this.a) = ColorRGBa(r, g, b, a)

    enum class Component {
        R,
        G,
        B,
        a
    }


    companion object {
        fun fromHex(hex: Int): ColorRGBa {
            val r = hex and (0xff0000) shr 16
            val g = hex and (0x00ff00) shr 8
            val b = hex and (0x0000ff)
            return ColorRGBa(r / 255.0, g / 255.0, b / 255.0, 1.0, Linearity.SRGB)
        }

        fun fromHex(hex: String): ColorRGBa {
            val parsedHex = hex.replace("#", "")
            val len = parsedHex.length
            val mult = len / 3

            val colors = (0..2).map { idx ->
                var c = parsedHex.substring(idx * mult, (idx + 1) * mult)

                c = if (len == 3) c + c else c

                try {
                    c.toInt(16)
                } catch(e : NumberFormatException) {
                    throw IllegalArgumentException("Cannot convert input '$hex' to an RGBa color value.")
                }
            }

            val (r, g, b) = colors

            return ColorRGBa(r / 255.0, g / 255.0, b / 255.0, 1.0, Linearity.SRGB)
        }

        val PINK = fromHex(0xffc0cb)
        val BLACK = ColorRGBa(0.0, 0.0, 0.0, 1.0)
        val WHITE = ColorRGBa(1.0, 1.0, 1.0, 1.0)
        val RED = ColorRGBa(1.0, 0.0, 0.0, 1.0)
        val BLUE = ColorRGBa(0.0, 0.0, 1.0)
        val GREEN = ColorRGBa(0.0, 1.0, 0.0)
        val YELLOW = ColorRGBa(1.0, 1.0, 0.0)
        val GRAY = ColorRGBa(0.5, 0.5, 0.5)
        val TRANSPARENT = ColorRGBa(0.0, 0.0, 0.0, 0.0)

        fun fromVector(vector: Vector3, alpha: Double = 1.0): ColorRGBa {
            return ColorRGBa(vector.x, vector.y, vector.z, alpha)
        }

        fun fromVector(vector: Vector4): ColorRGBa {
            return ColorRGBa(vector.x, vector.y, vector.z, vector.w)
        }
    }

    fun opacify(opacity: Double): ColorRGBa = ColorRGBa(r, g, b, a * opacity)
    fun shade(shade: Double): ColorRGBa = ColorRGBa(r * shade, g * shade, b * shade, a)

    /**
     * Copy of the the color with all of its fields coerced to [0, 1]
     */
    val saturated get() = ColorRGBa(r.coerceIn(0.0, 1.0), g.coerceIn(0.0, 1.0), b.coerceIn(0.0, 1.0), a.coerceIn(0.0, 1.0))
    val alphaMultiplied get() = ColorRGBa(r * a, g * a, b * a, a)
    val minValue get() = r.coerceAtMost(g).coerceAtMost(b)
    val maxValue get() = r.coerceAtLeast(g).coerceAtLeast(b)

    fun toHSVa(): ColorHSVa = ColorHSVa.fromRGBa(this.toSRGB())
    fun toHSLa(): ColorHSLa = ColorHSLa.fromRGBa(this.toSRGB())
    fun toXYZa(): ColorXYZa = ColorXYZa.fromRGBa(this.toLinear())
    fun toLABa(ref: ColorXYZa = ColorXYZa.NEUTRAL): ColorLABa = ColorLABa.fromRGBa(this.toLinear(), ref)
    fun toLUVa(ref: ColorXYZa = ColorXYZa.NEUTRAL): ColorLUVa = ColorLUVa.fromRGBa(this.toLinear(), ref)
    fun toLCHABa(ref: ColorXYZa = ColorXYZa.NEUTRAL): ColorLCHABa = toXYZa().toLABa(ref).toLCHABa()
    fun toLCHUVa(ref: ColorXYZa = ColorXYZa.NEUTRAL): ColorLCHUVa = toLUVa(ref).toLCHUVa()

    fun toLinear(): ColorRGBa {
        fun t(x: Double): Double {
            return if (x <= 0.04045) x / 12.92 else ((x + 0.055) / (1 + 0.055)).pow(2.4)
        }
        return when (linearity) {
            Linearity.SRGB -> ColorRGBa(t(r), t(g), t(b), a, Linearity.LINEAR)
            Linearity.UNKNOWN, Linearity.ASSUMED_SRGB -> ColorRGBa(t(r), t(g), t(b), a, Linearity.ASSUMED_LINEAR)
            Linearity.ASSUMED_LINEAR, Linearity.LINEAR -> this
        }
    }

    fun toSRGB(): ColorRGBa {
        fun t(x: Double): Double {
            return if (x <= 0.0031308) 12.92 * x else (1 + 0.055) * x.pow(1.0 / 2.4) - 0.055
        }
        return when (linearity) {
            Linearity.LINEAR -> ColorRGBa(t(r), t(g), t(b), a, Linearity.SRGB)
            Linearity.UNKNOWN, Linearity.ASSUMED_LINEAR -> ColorRGBa(t(r), t(g), t(b), a, Linearity.ASSUMED_SRGB)
            Linearity.ASSUMED_SRGB, Linearity.SRGB -> this
        }
    }
}

/**
 * Mixes two colors in RGBa space
 */
fun mix(left: ColorRGBa, right: ColorRGBa, x: Double): ColorRGBa {
    val sx = x.coerceIn(0.0, 1.0)
    return ColorRGBa(
            (1.0 - sx) * left.r + sx * right.r,
            (1.0 - sx) * left.g + sx * right.g,
            (1.0 - sx) * left.b + sx * right.b,
            (1.0 - sx) * left.a + sx * right.a)
}

/**
 * Color in RGBa space. Specify one value only to obtain a shade of gray.
 * @param r red in [0,1]
 * @param g green in [0,1]
 * @param b blue in [0,1]
 */
fun rgb(r: Double, g: Double = r, b: Double = r) = ColorRGBa(r, g, b)

/**
 * Color in RGBa space
 *
 * @param r red in [0,1]
 * @param g green in [0,1]
 * @param b blue in [0,1]
 * @param a alpha in [0,1]
 */
fun rgba(r: Double, g: Double, b: Double, a: Double) = ColorRGBa(r, g, b, a)

fun rgb(hex: String) = ColorRGBa.fromHex(hex)

