package org.openrndr.color

import org.openrndr.math.CastableToVector4
import org.openrndr.math.LinearType
import org.openrndr.math.mixAngle

/**
 * Represents an interface for objects that can be converted into an instance of [ColorRGBa].
 */
interface ConvertibleToColorRGBa {
    /** Convert into [ColorRGBa]. */
    fun toRGBa(): ColorRGBa
}

/**
 * Represents a color that can have its shade manipulated.
 *
 * @param T The type of the object that represents the color.
 */
interface ShadableColor<T> {
    /** Multiply the shade by a factor. */
    fun shade(factor: Double): T
}

/**
 * An interface representing a color with adjustable luminosity. It provides methods to modify
 * or manipulate the luminosity of a color and access its current luminosity value.
 *
 * @param T The type of the class implementing this interface.
 */
interface LuminosityColor<T> {
    fun withLuminosity(luminosity: Double): T

    val luminosity: Double
    fun shadeLuminosity(factor: Double) = withLuminosity(factor * luminosity)

    fun mixLuminosity(luminosity: Double, factor: Double) =
        withLuminosity(this.luminosity * (1.0 - factor) + luminosity * factor)
}


/**
 * Represents a color interface capable of hue manipulation.
 *
 * Provides functionality to shift, modify, and mix hues while maintaining the color representation.
 *
 * @param T the type of color returned by this interface.
 */
interface HueShiftableColor<T> {
    /**
     * Shift the hue of a color by the given amount of degrees.
     * @param shiftInDegrees the hue shift in degrees
     */
    fun shiftHue(shiftInDegrees: Double): T = withHue(hue + shiftInDegrees)

    /**
     * Creates a new color instance with the specified hue value.
     *
     * @param hue the desired hue value to set for the new color instance, expressed in degrees.
     * @return a color instance of type T with the specified hue applied.
     */
    fun withHue(hue: Double): T

    /**
     * Represents the hue component of a color, expressed in degrees.
     *
     * The hue is a circular value where 0° is red, 120° is green, 240° is blue,
     * and values wrap around cyclically (e.g., 360° is equivalent to 0°).
     * It defines the dominant wavelength of color and is used for color manipulation
     * such as hue shifting and mixing.
     */
    val hue: Double

    /**
     * Mixes the current hue with the specified hue using a linear interpolation factor.
     *
     * This method blends the current hue of the color with the given hue, considering the
     * circular nature of hues (e.g., 5° and 355° are treated as being 10° apart rather than 350°).
     *
     * @param hue the target hue to mix with, expressed in degrees.
     * @param factor the interpolation factor, where 0.0 results in the current hue,
     * and 1.0 results in the specified hue. Values between 0.0 and 1.0 produce interpolated hues.
     * @return a new color of type T with the mixed hue applied.
     */
    fun mixHue(hue: Double, factor: Double): T = withHue(mixAngle(this.hue, hue, factor))
}

/**
 * Represents a color that contains chroma information, provides utility methods
 * for manipulating chroma values, and can produce a new instance of the implementing type
 * with adjusted chroma values.
 *
 * @param T The type that implements the interface, typically representing a color model.
 */
interface ChromaColor<T> {
    /**
     * Creates a new instance of the implementing type with the specified chroma value.
     *
     * @param chroma The chroma value for the new instance.
     * @return A new instance of the implementing type with the updated chroma value.
     */
    fun withChroma(chroma: Double): T

    /**
     * Represents the chromatic intensity of a color.
     * Chroma is a measure of the color's vividness or strength, typically ranging
     * from neutral gray tones (low chroma) to highly saturated, vivid colors (high chroma).
     * This property can be manipulated to adjust the visual characteristics of the color.
     */
    val chroma: Double

    /**
     * Adjusts the chroma of the color by the specified shift value and returns a new instance
     * with the updated chroma. The resulting chroma is calculated by adding the provided shift value
     * to the current chroma.
     *
     * @param shift The amount by which the chroma should be adjusted. Positive values increase
     * the chroma (making the color more vivid), while negative values decrease the chroma
     * (making the color less saturated).
     */
    fun shiftChroma(shift: Double) = withChroma(chroma + shift)

    /**
     * Adjusts the chroma of the color by scaling it with the specified factor and returns a new instance
     * with the updated chroma value. This method allows for proportionally increasing or decreasing
     * the chromatic intensity of the color.
     *
     * @param factor The multiplier for the chroma value. A factor greater than 1 increases the chroma,
     * while a factor between 0 and 1 decreases it. A factor of 1 leaves the chroma unchanged.
     */
    fun modulateChroma(factor: Double) = withChroma(chroma * factor)

    fun mixChroma(target: Double, factor: Double) = withChroma(chroma * (1.0 - factor) + target * factor)
}


interface SaturatableColor<T> {
    /** Multiply the saturation by a factor. */
    fun saturate(factor: Double): T = withSaturation(saturation * factor)

    /**
     * Adjusts the saturation level of the color to the specified value.
     *
     * @param saturation The new saturation value to set. It is expected to be within the valid range for saturation.
     * @return A new instance of the color with the specified saturation level applied.
     */
    fun withSaturation(saturation: Double): T

    /**
     * Represents the saturation level of the color.
     *
     * Saturation determines the intensity or purity of the color. A lower saturation value results in a more desaturated (grayer) color,
     * while a higher saturation value results in a more vivid or pure color. The valid range for this value depends on the implementation.
     */
    val saturation: Double

    fun mixSaturation(saturation: Double, factor: Double): T =
        withSaturation(this.saturation * (1.0 - factor) + saturation * factor)
}

interface OpacifiableColor<T> {
    /** The opacity of the given color model. */
    val alpha: Double

    /**
     * Returns a copy of the color with the opacity ([alpha]) multiplied by a factor.
     * @param factor a scaling factor used for the opacity
     */
    fun opacify(factor: Double): T
}

/**
 * Represents a reference white point used in color calculations.
 * A reference white point is a standard defining the white color
 * under specific lighting conditions, serving as a baseline for
 * measurements and color conversions in a color space.
 */
interface ReferenceWhitePoint {
    /** The reference white point against which the color is calculated. */
    val ref: ColorXYZa
}

interface ColorModel<T : ColorModel<T>> : OpacifiableColor<T>, ConvertibleToColorRGBa, CastableToVector4

/**
 * Allows performing select algebraic operations on colors of this kind.
 */
interface AlgebraicColor<T : AlgebraicColor<T>> : LinearType<T> {
    override operator fun div(scale: Double): T = times(1.0 / scale)
    fun mix(other: T, factor: Double): T =
        ((this * (1.0 - factor)) as AlgebraicColor<T>) + (other as AlgebraicColor<T> * factor)
}