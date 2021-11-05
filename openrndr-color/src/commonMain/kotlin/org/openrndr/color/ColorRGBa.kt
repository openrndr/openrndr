package org.openrndr.color

import org.openrndr.math.CastableToVector4
import org.openrndr.math.Matrix55
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4
import kotlin.jvm.JvmOverloads
import kotlin.math.pow

enum class Linearity(val certainty: Int) {
    UNKNOWN(-1),
    LINEAR(1),
    SRGB(1),
    ASSUMED_LINEAR(0),
    ASSUMED_SRGB(0)
    ;

    fun leastCertain(other: Linearity): Linearity {
        return if (this.certainty <= other.certainty) {
            this
        } else {
            other
        }
    }

    fun isEquivalent(other: Linearity): Boolean {
        return if (this == UNKNOWN || other == UNKNOWN) {
            false
        } else if (this == other) {
            true
        } else {
            if ((this == LINEAR || this == ASSUMED_LINEAR) && (other == LINEAR || other == ASSUMED_LINEAR)) {
                true
            } else (this == SRGB || this == ASSUMED_SRGB) && (other == SRGB || other == ASSUMED_SRGB)
        }
    }

}

/**
 * A generic RGB color space capable of representing
 * both the linear and the sRGB color spaces.
 *
 * @param r red as a percentage between 0.0 and 1.0
 * @param g green as a percentage between 0.0 and 1.0
 * @param b blue as a percentage between 0.0 and 1.0
 * @param a alpha as a percentage between 0.0 and 1.0
 * @see [rgb]
 * @see [rgba]
 */
@Suppress("EqualsOrHashCode") // generated equals() is ok, only hashCode() needs to be overridden
data class ColorRGBa(
    val r: Double,
    val g: Double,
    val b: Double,
    val a: Double = 1.0,
    val linearity: Linearity = Linearity.UNKNOWN
) :
    ConvertibleToColorRGBa,
    CastableToVector4,
    OpacifiableColor<ColorRGBa>,
    ShadableColor<ColorRGBa>,
    AlgebraicColor<ColorRGBa> {

    operator fun invoke(r: Double = this.r, g: Double = this.g, b: Double = this.b, a: Double = this.a) =
        ColorRGBa(r, g, b, a)

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
                } catch (e: NumberFormatException) {
                    throw IllegalArgumentException("Cannot convert input '$hex' to an RGBa color value.")
                }
            }

            val (r, g, b) = colors

            return ColorRGBa(r / 255.0, g / 255.0, b / 255.0, 1.0, Linearity.SRGB)
        }

        /** @suppress */
        val PINK = fromHex(0xffc0cb)

        /** @suppress */
        val BLACK = ColorRGBa(0.0, 0.0, 0.0, 1.0, Linearity.SRGB)

        /** @suppress */
        val WHITE = ColorRGBa(1.0, 1.0, 1.0, 1.0, Linearity.SRGB)

        /** @suppress */
        val RED = ColorRGBa(1.0, 0.0, 0.0, 1.0, Linearity.SRGB)

        /** @suppress */
        val BLUE = ColorRGBa(0.0, 0.0, 1.0, 1.0, Linearity.SRGB)

        /** @suppress */
        val GREEN = ColorRGBa(0.0, 1.0, 0.0, 1.0, Linearity.SRGB)

        /** @suppress */
        val YELLOW = ColorRGBa(1.0, 1.0, 0.0, 1.0, Linearity.SRGB)

        /** @suppress */
        val GRAY = ColorRGBa(0.5, 0.5, 0.5, 1.0, Linearity.SRGB)

        /** @suppress */
        val TRANSPARENT = ColorRGBa(0.0, 0.0, 0.0, 0.0, Linearity.SRGB)

        /**
         * Create a ColorRGBa object from a [Vector3]
         * @param vector input vector, `[x, y, z]` is mapped to `[r, g, b]`
         * @param alpha optional alpha value, default is 1.0
         */
        fun fromVector(vector: Vector3, alpha: Double = 1.0, linearity: Linearity = Linearity.SRGB): ColorRGBa {
            return ColorRGBa(vector.x, vector.y, vector.z, alpha, linearity)
        }


        /**
         * Create a ColorRGBa object from a [Vector4]
         * @param vector input vector, `[x, y, z, w]` is mapped to `[r, g, b, a]`
         */
        fun fromVector(vector: Vector4, linearity: Linearity = Linearity.SRGB): ColorRGBa {
            return ColorRGBa(vector.x, vector.y, vector.z, vector.w, linearity)
        }
    }

    /**
     * Creates a copy of color with adjusted opacity
     * @param factor a scaling factor used for the opacity
     * @return A [ColorRGBa] with scaled opacity
     * @see shade
     */
    override fun opacify(factor: Double): ColorRGBa = ColorRGBa(r, g, b, a * factor, linearity)

    /**
     * Creates a copy of color with adjusted color
     * @param factor a scaling factor used for the opacity
     * @return A [ColorRGBa] with scaled colors
     * @see opacify
     */
    override fun shade(factor: Double): ColorRGBa = ColorRGBa(r * factor, g * factor, b * factor, a, linearity)

    /**
     * Copy of the the color with all of its fields clamped to `[0, 1]`
     */
    val saturated
        get() = ColorRGBa(
            r.coerceIn(0.0, 1.0),
            g.coerceIn(0.0, 1.0),
            b.coerceIn(0.0, 1.0),
            a.coerceIn(0.0, 1.0), linearity
        )
    val alphaMultiplied get() = ColorRGBa(r * a, g * a, b * a, a, linearity)

    /**
     * The minimum value over `r`, `g`, `b`
     * @see maxValue
     */
    val minValue get() = r.coerceAtMost(g).coerceAtMost(b)

    /**
     * The maximum value over `r`, `g`, `b`
     * @see minValue
     */
    val maxValue get() = r.coerceAtLeast(g).coerceAtLeast(b)

    /**
     * calculate luminance value
     * luminance value is according to <a>https://www.w3.org/TR/2008/REC-WCAG20-20081211/#relativeluminancedef</a>
     */
    val luminance: Double
        get() = when (linearity) {
            Linearity.SRGB -> toLinear().luminance
            else -> 0.2126 * r + 0.7152 * g + 0.0722 * b
        }

    /**
     * calculate the contrast value between this color and the given color
     * contrast value is accordingo to <a>// see http://www.w3.org/TR/2008/REC-WCAG20-20081211/#contrast-ratiodef</a>
     */
    fun getContrastRatio(other: ColorRGBa): Double {
        val l1 = luminance
        val l2 = other.luminance
        return if (l1 > l2) (l1 + 0.05) / (l2 + 0.05) else (l2 + 0.05) / (l1 + 0.05)
    }

    fun toHSVa(): ColorHSVa = ColorHSVa.fromRGBa(this.toSRGB())
    fun toHSLa(): ColorHSLa = ColorHSLa.fromRGBa(this.toSRGB())
    fun toXSVa(): ColorXSVa = ColorHSVa.fromRGBa(this.toSRGB()).toXSVa()
    fun toXSLa(): ColorXSLa = ColorHSLa.fromRGBa(this.toSRGB()).toXSLa()
    fun toXYZa(): ColorXYZa = ColorXYZa.fromRGBa(this.toLinear())

    @JvmOverloads
    fun toLABa(ref: ColorXYZa = ColorXYZa.NEUTRAL): ColorLABa = ColorLABa.fromRGBa(this.toLinear(), ref)

    @JvmOverloads
    fun toLUVa(ref: ColorXYZa = ColorXYZa.NEUTRAL): ColorLUVa = ColorLUVa.fromRGBa(this.toLinear(), ref)

    @JvmOverloads
    fun toLCHABa(ref: ColorXYZa = ColorXYZa.NEUTRAL): ColorLCHABa = toXYZa().toLABa(ref).toLCHABa()

    @JvmOverloads
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

    /**
     * Convert to SRGB
     * @see toLinear
     */
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

    override fun toRGBa(): ColorRGBa = this

    // This is here because the default hashing of enums on the JVM is not stable.
    override fun hashCode(): Int {
        var result = r.hashCode()
        result = 31 * result + g.hashCode()
        result = 31 * result + b.hashCode()
        result = 31 * result + a.hashCode()
        // here we overcome the unstable hash by using the ordinal value
        result = 31 * result + linearity.ordinal.hashCode()
        return result
    }

    override fun plus(right: ColorRGBa) = copy(r = r + right.r, g = g + right.g, b = b + right.b, a = a + right.a)

    override fun minus(right: ColorRGBa) = copy(r = r - right.r, g = g - right.g, b = b - right.b, a = a - right.a)

    override fun times(scale: Double) = copy(r = r * scale, g = g * scale, b = b * scale, a = a * scale)

    override fun mix(other: ColorRGBa, factor: Double): ColorRGBa {
        return mix(this, other, factor)
    }

    override fun toVector4(): Vector4 = Vector4(r, g, b, a)
}

/**
 * Weighted mix between two colors in the generic RGB color space.
 * @param x the weighting of colors, a value 0.0 is equivalent to [left],
 * 1.0 is equivalent to [right] and at 0.5 both colors contribute to the result equally
 * @return a mix of [left] and [right] weighted by [x]
 */
fun mix(left: ColorRGBa, right: ColorRGBa, x: Double): ColorRGBa {
    val sx = x.coerceIn(0.0, 1.0)

    if (left.linearity.isEquivalent(right.linearity)) {
        return ColorRGBa(
            (1.0 - sx) * left.r + sx * right.r,
            (1.0 - sx) * left.g + sx * right.g,
            (1.0 - sx) * left.b + sx * right.b,
            (1.0 - sx) * left.a + sx * right.a,
            linearity = left.linearity.leastCertain(right.linearity)
        )
    } else {
        return when (right.linearity) {
            Linearity.LINEAR, Linearity.ASSUMED_LINEAR -> {
                mix(left.toLinear(), right.toLinear(), x)
            }
            Linearity.SRGB, Linearity.ASSUMED_SRGB -> {
                mix(left.toSRGB(), right.toSRGB(), x)
            }
            else -> {
                error("can't blend ${right.linearity} with ${left.linearity}")
            }
        }
    }
}

/**
 * Color in RGBa space. Specify one value only to obtain a shade of gray.
 * @param r red in `[0,1]`
 * @param g green in `[0,1]`
 * @param b blue in `[0,1]`
 */
fun rgb(r: Double, g: Double = r, b: Double = r) = ColorRGBa(r, g, b, linearity = Linearity.SRGB)

/**
 * Create a color in RGBa space
 * This function is a short-hand for using the ColorRGBa constructor
 * @param r red in `[0,1]`
 * @param g green in `[0,1]`
 * @param b blue in `[0,1]`
 * @param a alpha in `[0,1]`
 */
fun rgba(r: Double, g: Double, b: Double, a: Double) = ColorRGBa(r, g, b, a, linearity = Linearity.SRGB)

/**
 * Create color from a string encoded hex value
 * @param hex string encoded hex value, for example `"ffc0cd"`
 */
fun rgb(hex: String) = ColorRGBa.fromHex(hex)

operator fun Matrix55.times(color: ColorRGBa): ColorRGBa {
    return color.copy(
        r = color.r * c0r0 + color.g * c1r0 + color.b * c2r0 + color.a * c3r0 + c4r0,
        g = color.r * c0r1 + color.g * c1r1 + color.b * c2r1 + color.a * c3r1 + c4r1,
        b = color.r * c0r2 + color.g * c1r2 + color.b * c2r2 + color.a * c3r2 + c4r2,
        a = color.r * c0r3 + color.g * c1r3 + color.b * c2r3 + color.a * c3r3 + c4r3
    )
}