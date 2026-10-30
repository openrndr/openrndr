package org.openrndr.color

import org.openrndr.math.Vector2
import kotlin.math.abs
import kotlin.math.pow

/**
 * The transfer function of a color profile, which decodes encoded components to linear components
 */
sealed interface ColorTransfer {
    /**
     * The transfer function of sRGB, also used by Display P3
     */
    data object SRGB : ColorTransfer

    /**
     * Components are linear
     */
    data object Linear : ColorTransfer

    /**
     * A power function: `y = x^gamma`
     */
    data class Gamma(val gamma: Double) : ColorTransfer

    /**
     * An ICC parametric curve: `y = (a * x + b)^g + e` for `x >= d`, `y = c * x + f` otherwise
     */
    data class Parametric(
        val g: Double,
        val a: Double,
        val b: Double,
        val c: Double,
        val d: Double,
        val e: Double,
        val f: Double
    ) : ColorTransfer {
        fun decode(x: Double): Double = if (x >= d) (a * x + b).coerceAtLeast(0.0).pow(g) + e else c * x + f
    }

    /**
     * A sampled curve, [values] are spaced evenly over `[0, 1]`
     */
    class Table(val values: DoubleArray) : ColorTransfer {
        override fun equals(other: Any?) = other is Table && values.contentEquals(other.values)
        override fun hashCode() = values.contentHashCode()
        override fun toString() = "Table(${values.size} values)"
    }
}

/**
 * A color profile, as described by an ICC profile: the chromaticities of the primaries and the white point, and the
 * transfer function.
 *
 * @property name the description of the profile, such as "Display P3"
 * @property red the xy chromaticity of the red primary
 * @property green the xy chromaticity of the green primary
 * @property blue the xy chromaticity of the blue primary
 * @property white the xy chromaticity of the white point
 * @property transfer the transfer function
 * @property icc the ICC profile the color profile was read from, if any
 */
class ColorProfile(
    val name: String?,
    val red: Vector2,
    val green: Vector2,
    val blue: Vector2,
    val white: Vector2,
    val transfer: ColorTransfer,
    val icc: ByteArray? = null
) {
    /**
     * The [Linearity] with the primaries and white point of this profile: [Linearity.SRGB] for sRGB,
     * [Linearity.DISPLAY_P3] for Display P3 or null for other color spaces. The transfer function is not compared.
     */
    val linearity: Linearity? by lazy {
        when {
            hasPrimariesOf(SRGB) -> Linearity.SRGB
            hasPrimariesOf(DISPLAY_P3) -> Linearity.DISPLAY_P3
            else -> null
        }
    }

    private fun hasPrimariesOf(other: ColorProfile): Boolean {
        fun close(a: Vector2, b: Vector2) = abs(a.x - b.x) <= TOLERANCE && abs(a.y - b.y) <= TOLERANCE
        return close(red, other.red) && close(green, other.green) && close(blue, other.blue) && close(white, other.white)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ColorProfile) return false
        return name == other.name && red == other.red && green == other.green && blue == other.blue &&
                white == other.white && transfer == other.transfer
    }

    override fun hashCode(): Int {
        var result = name?.hashCode() ?: 0
        result = 31 * result + red.hashCode()
        result = 31 * result + green.hashCode()
        result = 31 * result + blue.hashCode()
        result = 31 * result + white.hashCode()
        result = 31 * result + transfer.hashCode()
        return result
    }

    override fun toString(): String =
        "ColorProfile(name=$name, linearity=$linearity, red=$red, green=$green, blue=$blue, white=$white, transfer=$transfer)"

    companion object {
        /** the largest difference of xy chromaticities that matches a known color space */
        private const val TOLERANCE = 0.01

        private val D65 = Vector2(0.3127, 0.3290)

        /**
         * The sRGB color profile
         */
        val SRGB = ColorProfile(
            "sRGB", Vector2(0.64, 0.33), Vector2(0.30, 0.60), Vector2(0.15, 0.06), D65, ColorTransfer.SRGB
        )

        /**
         * The Display P3 color profile
         */
        val DISPLAY_P3 = ColorProfile(
            "Display P3", Vector2(0.680, 0.320), Vector2(0.265, 0.690), Vector2(0.150, 0.060), D65, ColorTransfer.SRGB
        )

        /**
         * Reads a color profile from an RGB matrix/TRC ICC profile (version 2 or 4)
         * @return the color profile, or null when [bytes] is not an RGB matrix/TRC profile
         */
        fun fromICC(bytes: ByteArray): ColorProfile? = readIccProfile(bytes)
    }
}
