package org.openrndr.color

import kotlinx.serialization.Serializable
import org.openrndr.math.Matrix55
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4
import kotlin.math.pow

@Serializable
enum class Linearity(val certainty: Int) {
    /**
     * Represents a linear color space.
     *
     * LINEAR typically signifies that the values in the color space are in a linear relationship,
     * meaning there is no gamma correction or transformation applied to the data.
     */
    LINEAR(1),

    /**
     * Represents a standard RGB (sRGB) color space.
     *
     * SRGB typically refers to a non-linear color space with gamma correction applied,
     * designed for consistent color representation across devices.
     */
    SRGB(1),
    ;

    fun leastCertain(other: Linearity): Linearity {
        return if (this.certainty <= other.certainty) {
            this
        } else {
            other
        }
    }

    fun isEquivalent(other: Linearity): Boolean {
        return this == other
    }

}

/**
 * Represents a color in the RGBA color space. Each component, including red, green, blue, and alpha (opacity),
 * is represented as a `Double` in the range `[0.0, 1.0]`. The color can be defined in either linear or sRGB space,
 * determined by the `linearity` property.
 *
 * This class provides a wide variety of utility functions for manipulating and converting colors, such as shading,
 * opacity adjustment, and format transformations. It also includes methods for parsing colors from hexadecimal
 * notation or vectors.
 *
 * @property r Red component of the color as a value between `0.0` and `1.0`.
 * @property g Green component of the color as a value between `0.0` and `1.0`.
 * @property b Blue component of the color as a value between `0.0` and `1.0`.
 * @property alpha Alpha (opacity) component of the color as a value between `0.0` and `1.0`. Defaults to `1.0`.
 * @property linearity Indicates whether the color is defined in linear or sRGB space. Defaults to [Linearity.LINEAR].
 */
@Serializable
@Suppress("EqualsOrHashCode") // generated equals() is ok, only hashCode() needs to be overridden
data class ColorRGBa(
    val r: Double,
    val g: Double,
    val b: Double,
    override val alpha: Double = 1.0,
    val linearity: Linearity = Linearity.LINEAR
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
                4 -> ColorRGBa(
                    fromHex1(hex, pos),
                    fromHex1(hex, pos + 1),
                    fromHex1(hex, pos + 2),
                    fromHex1(hex, pos + 3),
                    Linearity.SRGB
                )

                6 -> ColorRGBa(fromHex2(hex, pos), fromHex2(hex, pos + 2), fromHex2(hex, pos + 4), 1.0, Linearity.SRGB)
                8 -> ColorRGBa(
                    fromHex2(hex, pos),
                    fromHex2(hex, pos + 2),
                    fromHex2(hex, pos + 4),
                    fromHex2(hex, pos + 6),
                    Linearity.SRGB
                )

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
        val TRANSPARENT = ColorRGBa(0.0, 0.0, 0.0, 0.0, Linearity.LINEAR)

        /**
         * Create a ColorRGBa object from a [Vector3]
         * @param vector input vector, `[x, y, z]` is mapped to `[r, g, b]`
         * @param alpha optional alpha value, default is 1.0
         */
        fun fromVector(vector: Vector3, alpha: Double = 1.0, linearity: Linearity = Linearity.LINEAR): ColorRGBa {
            return ColorRGBa(vector.x, vector.y, vector.z, alpha, linearity)
        }


        /**
         * Create a ColorRGBa object from a [Vector4]
         * @param vector input vector, `[x, y, z, w]` is mapped to `[r, g, b, a]`
         */
        fun fromVector(vector: Vector4, linearity: Linearity = Linearity.LINEAR): ColorRGBa {
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

    @Deprecated("Use clip() instead", replaceWith = ReplaceWith("clip()"))
    val saturated: ColorRGBa
        get() = clip()

    /**
     * Copy of the color with all of its fields clamped to `[0, 1]`
     */
    fun clip(): ColorRGBa = copy(
        r = r.coerceIn(0.0..1.0),
        g = g.coerceIn(0.0..1.0),
        b = b.coerceIn(0.0..1.0),
        alpha = alpha.coerceIn(0.0..1.0)
    )


    /**
     * Returns a new instance of [ColorRGBa] where the red, green, and blue components
     * are multiplied by the alpha value of the original color. The alpha value and linearity
     * remain unchanged.
     *
     * This computed property is commonly used for adjusting the color intensity based
     * on its transparency.
     */
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
     * Converts this color to the specified linearity.
     *
     * @param linearity The target linearity to which the color should be converted.
     *                  Supported values are [Linearity.SRGB] and [Linearity.LINEAR].
     * @return A [ColorRGBa] instance in the specified linearity.
     */
    fun toLinearity(linearity: Linearity): ColorRGBa {
        return when (linearity) {
            Linearity.SRGB -> toSRGB()
            Linearity.LINEAR -> toLinear()
        }
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

    fun toLABa(ref: ColorXYZa = ColorXYZa.NEUTRAL): ColorLABa = ColorLABa.fromRGBa(this.toLinear(), ref)

    fun toLUVa(ref: ColorXYZa = ColorXYZa.NEUTRAL): ColorLUVa = ColorLUVa.fromRGBa(this.toLinear(), ref)

    fun toLCHABa(ref: ColorXYZa = ColorXYZa.NEUTRAL): ColorLCHABa = toXYZa().toLABa(ref).toLCHABa()

    fun toLCHUVa(ref: ColorXYZa = ColorXYZa.NEUTRAL): ColorLCHUVa = toLUVa(ref).toLCHUVa()

    fun toLinear(): ColorRGBa {
        fun t(x: Double): Double {
            return if (x <= 0.04045) x / 12.92 else ((x + 0.055) / (1 + 0.055)).pow(2.4)
        }
        return when (linearity) {
            Linearity.SRGB -> ColorRGBa(t(r), t(g), t(b), alpha, Linearity.LINEAR)
            else -> this
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
            else -> this
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

    /**
     * Retrieves the color's RGBA component value based on the specified index:
     * [index] should be 0 for red, 1 for green, 2 for blue, 3 for alpha.
     * Other index values throw an [IndexOutOfBoundsException].
     */
    operator fun get(index: Int) = when (index) {
        0 -> r
        1 -> g
        2 -> b
        3 -> alpha
        else -> throw IllegalArgumentException("unsupported index")
    }
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
            Linearity.LINEAR -> {
                mix(left.toLinear(), right.toLinear(), x)
            }

            Linearity.SRGB -> {
                mix(left.toSRGB(), right.toSRGB(), x)
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
fun rgb(r: Double, g: Double, b: Double, a: Double = 1.0) = ColorRGBa(r, g, b, a, linearity = Linearity.LINEAR)

/**
 * Shorthand for calling [ColorRGBa].
 * @param gray shade of gray in `[0,1]`
 * @param a alpha in `[0,1]`, defaults to `1.0`
 */
fun rgb(gray: Double, a: Double = 1.0) = ColorRGBa(gray, gray, gray, a, linearity = Linearity.LINEAR)

/**
 * Create a color in RGBa space
 * This function is a shorthand for using the ColorRGBa constructor
 * @param r red in `[0,1]`
 * @param g green in `[0,1]`
 * @param b blue in `[0,1]`
 * @param a alpha in `[0,1]`
 */
@Deprecated("Use rgb(r, g, b, a)", ReplaceWith("rgb(r, g, b, a)"), DeprecationLevel.WARNING)
fun rgba(r: Double, g: Double, b: Double, a: Double) = ColorRGBa(r, g, b, a, linearity = Linearity.LINEAR)

/**
 * Shorthand for calling [ColorRGBa.fromHex].
 * Creates a [ColorRGBa] with [Linearity.SRGB] from a hex string.
 * @param hex string encoded hex value, for example `"ffc0cd"`
 */
fun rgb(hex: String) = ColorRGBa.fromHex(hex)

/**
 * Converts RGB integer color values into a ColorRGBa object with sRGB linearity.
 *
 * @param red The red component of the color, in the range 0-255.
 * @param green The green component of the color, in the range 0-255.
 * @param blue The blue component of the color, in the range 0-255.
 * @param alpha The alpha (transparency) component of the color, in the range 0-255. Default value is 255 (fully opaque).
 */
fun rgb(red: Int, green: Int, blue: Int, alpha: Int = 255) =
    ColorRGBa(red / 255.0, green / 255.0, blue / 255.0, alpha / 255.0, Linearity.SRGB)


/**
 * Multiplies a 5x5 matrix with a ColorRGBa instance.
 *
 * @param color The ColorRGBa instance to be transformed by the matrix.
 * @return A new ColorRGBa instance resulting from the matrix transformation.
 */
operator fun Matrix55.times(color: ColorRGBa): ColorRGBa {
    return color.copy(
        r = color.r * c0r0 + color.g * c1r0 + color.b * c2r0 + color.alpha * c3r0 + c4r0,
        g = color.r * c0r1 + color.g * c1r1 + color.b * c2r1 + color.alpha * c3r1 + c4r1,
        b = color.r * c0r2 + color.g * c1r2 + color.b * c2r2 + color.alpha * c3r2 + c4r2,
        alpha = color.r * c0r3 + color.g * c1r3 + color.b * c2r3 + color.alpha * c3r3 + c4r3
    )
}