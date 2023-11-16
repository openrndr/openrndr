package org.openrndr.color

import kotlinx.serialization.Serializable
import org.openrndr.math.Matrix55
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4
import kotlin.jvm.JvmOverloads
import kotlin.math.pow


@Serializable
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
 * @param alpha alpha as a percentage between 0.0 and 1.0
 * @see [rgb]
 * @see [rgba]
 */
@Serializable
@Suppress("EqualsOrHashCode") // generated equals() is ok, only hashCode() needs to be overridden
data class ColorRGBa @JvmOverloads constructor(
    val r: Double,
    val g: Double,
    val b: Double,
    override val alpha: Double = 1.0,
    val linearity: Linearity = Linearity.UNKNOWN
) :
    ColorModel<ColorRGBa>,
    ShadableColor<ColorRGBa>,
    AlgebraicColor<ColorRGBa> {

    enum class Component {
        R,
        G,
        B
    }

    companion object {
        /**
         * Calculates a color from hexadecimal value. For values with transparency
         * use the [String] variant of this function.
         */
        fun fromHex(hex: Int): ColorRGBa {
            val r = hex and (0xff0000) shr 16
            val g = hex and (0x00ff00) shr 8
            val b = hex and (0x0000ff)
            return ColorRGBa(r / 255.0, g / 255.0, b / 255.0, 1.0, Linearity.SRGB)
        }

        /**
         * Calculates a color from hexadecimal notation, like in CSS.
         *
         * Supports the following formats
         * * `RGB`
         * * `RGBA`
         * * `RRGGBB`
         * * `RRGGBBAA`
         *
         * where every character is a valid hex digit between `0..f` (case-insensitive).
         * Supports leading "#" or "0x".
         */
        fun fromHex(hex: String): ColorRGBa {
            val pos = when {
                hex.startsWith("#") -> 1
                hex.startsWith("0x") -> 2
                else -> 0
            }
            fun fromHex1(str: String, pos: Int): Double {
                return 17 * str[pos].digitToInt(16) / 255.0
            }
            fun fromHex2(str: String, pos: Int): Double {
                return (16 * str[pos].digitToInt(16) + str[pos + 1].digitToInt(16)) / 255.0
            }
            return when (hex.length - pos) {
                3 -> ColorRGBa(fromHex1(hex, pos), fromHex1(hex, pos + 1), fromHex1(hex, pos + 2), 1.0, Linearity.SRGB)
                4 -> ColorRGBa(fromHex1(hex, pos), fromHex1(hex, pos + 1), fromHex1(hex, pos + 2), fromHex1(hex, pos + 3), Linearity.SRGB)
                6 -> ColorRGBa(fromHex2(hex, pos), fromHex2(hex, pos + 2), fromHex2(hex, pos + 4), 1.0, Linearity.SRGB)
                8 -> ColorRGBa(fromHex2(hex, pos), fromHex2(hex, pos + 2), fromHex2(hex, pos + 4), fromHex2(hex, pos + 6), Linearity.SRGB)
                else -> throw IllegalArgumentException("Invalid hex length/format for '$hex'")
            }
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
        val CYAN = ColorRGBa(0.0, 1.0, 1.0, 1.0, Linearity.SRGB)

        /** @suppress */
        val MAGENTA = ColorRGBa(1.0, 0.0, 1.0, 1.0, Linearity.SRGB)

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

    @Deprecated("Legacy alpha parameter name", ReplaceWith("alpha"))
    val a = alpha

    /**
     * Creates a copy of color with adjusted opacity
     * @param factor a scaling factor used for the opacity
     * @return A [ColorRGBa] with scaled opacity
     * @see shade
     */
    override fun opacify(factor: Double): ColorRGBa = ColorRGBa(r, g, b, alpha * factor, linearity)

    /**
     * Creates a copy of color with adjusted color
     * @param factor a scaling factor used for the opacity
     * @return A [ColorRGBa] with scaled colors
     * @see opacify
     */
    override fun shade(factor: Double): ColorRGBa = ColorRGBa(r * factor, g * factor, b * factor, alpha, linearity)

    /**
     * Copy of the color with all of its fields clamped to `[0, 1]`
     */
    val saturated: ColorRGBa
        get() = ColorRGBa(
            r.coerceIn(0.0, 1.0),
            g.coerceIn(0.0, 1.0),
            b.coerceIn(0.0, 1.0),
            alpha.coerceIn(0.0, 1.0), linearity
        )
    val alphaMultiplied: ColorRGBa
        get() = ColorRGBa(r * alpha, g * alpha, b * alpha, alpha, linearity)

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
            Linearity.SRGB -> ColorRGBa(t(r), t(g), t(b), alpha, Linearity.LINEAR)
            Linearity.UNKNOWN, Linearity.ASSUMED_SRGB -> ColorRGBa(t(r), t(g), t(b), alpha, Linearity.ASSUMED_LINEAR)
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
            Linearity.LINEAR -> ColorRGBa(t(r), t(g), t(b), alpha, Linearity.SRGB)
            Linearity.UNKNOWN, Linearity.ASSUMED_LINEAR -> ColorRGBa(t(r), t(g), t(b), alpha, Linearity.ASSUMED_SRGB)
            Linearity.ASSUMED_SRGB, Linearity.SRGB -> this
        }
    }

    override fun toRGBa(): ColorRGBa = this

    // This is here because the default hashing of enums on the JVM is not stable.
    override fun hashCode(): Int {
        var result = r.hashCode()
        result = 31 * result + g.hashCode()
        result = 31 * result + b.hashCode()
        result = 31 * result + alpha.hashCode()
        // here we overcome the unstable hash by using the ordinal value
        result = 31 * result + linearity.ordinal.hashCode()
        return result
    }

    override fun plus(right: ColorRGBa) = copy(
        r = r + right.r,
        g = g + right.g,
        b = b + right.b,
        alpha = alpha + right.alpha
    )

    override fun minus(right: ColorRGBa) = copy(
        r = r - right.r,
        g = g - right.g,
        b = b - right.b,
        alpha = alpha - right.alpha
    )

    override fun times(scale: Double) = copy(r = r * scale, g = g * scale, b = b * scale, alpha = alpha * scale)

    override fun mix(other: ColorRGBa, factor: Double): ColorRGBa {
        return mix(this, other, factor)
    }

    override fun toVector4(): Vector4 = Vector4(r, g, b, alpha)
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
            (1.0 - sx) * left.alpha + sx * right.alpha,
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
 * Shorthand for calling [ColorRGBa].
 * Specify only one value to obtain a shade of gray.
 * @param r red in `[0,1]`
 * @param g green in `[0,1]`
 * @param b blue in `[0,1]`
 * @param a alpha in `[0,1]`, defaults to `1.0`
 */
fun rgb(r: Double, g: Double, b: Double, a: Double = 1.0) = ColorRGBa(r, g, b, a, linearity = Linearity.SRGB)

/**
 * Shorthand for calling [ColorRGBa].
 * @param gray shade of gray in `[0,1]`
 * @param a alpha in `[0,1]`, defaults to `1.0`
 */
fun rgb(gray: Double, a: Double = 1.0) = ColorRGBa(gray, gray, gray, a, linearity = Linearity.SRGB)

/**
 * Create a color in RGBa space
 * This function is a shorthand for using the ColorRGBa constructor
 * @param r red in `[0,1]`
 * @param g green in `[0,1]`
 * @param b blue in `[0,1]`
 * @param a alpha in `[0,1]`
 */
@Deprecated("Use rgb(r, g, b, a)", ReplaceWith("rgb(r, g, b, a)"), DeprecationLevel.WARNING)
fun rgba(r: Double, g: Double, b: Double, a: Double) = ColorRGBa(r, g, b, a, linearity = Linearity.SRGB)

/**
 * Shorthand for calling [ColorRGBa.fromHex].
 * Creates a [ColorRGBa] from a hex string.
 * @param hex string encoded hex value, for example `"ffc0cd"`
 */
fun rgb(hex: String) = ColorRGBa.fromHex(hex)

operator fun Matrix55.times(color: ColorRGBa): ColorRGBa {
    return color.copy(
        r = color.r * c0r0 + color.g * c1r0 + color.b * c2r0 + color.alpha * c3r0 + c4r0,
        g = color.r * c0r1 + color.g * c1r1 + color.b * c2r1 + color.alpha * c3r1 + c4r1,
        b = color.r * c0r2 + color.g * c1r2 + color.b * c2r2 + color.alpha * c3r2 + c4r2,
        alpha = color.r * c0r3 + color.g * c1r3 + color.b * c2r3 + color.alpha * c3r3 + c4r3
    )
}