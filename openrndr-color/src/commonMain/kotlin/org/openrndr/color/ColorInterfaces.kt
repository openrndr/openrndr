package org.openrndr.color

import org.openrndr.math.CastableToVector4
import org.openrndr.math.LinearType
import org.openrndr.math.mixAngle

interface ConvertibleToColorRGBa {
    /** Convert into [ColorRGBa]. */
    fun toRGBa(): ColorRGBa
}

interface ShadableColor<T> {
    /** Multiply the shade by a factor. */
    fun shade(factor: Double): T
}

interface LuminosityColor<T> {
    fun withLuminosity(luminosity: Double): T

    val luminosity: Double
    fun shadeLuminosity(factor: Double) = withLuminosity(factor * luminosity)

    fun mixLuminosity(luminosity: Double, factor: Double) =
        withLuminosity(this.luminosity * (1.0 - factor) + luminosity * factor)
}


interface HueShiftableColor<T> {
    /**
     * Shift the hue of a color by the given amount of degrees.
     * @param shiftInDegrees the hue shift in degrees
     */
    fun shiftHue(shiftInDegrees: Double): T = withHue(hue + shiftInDegrees)

    fun withHue(hue: Double): T

    val hue: Double

    fun mixHue(hue: Double, factor: Double): T = withHue(mixAngle(this.hue, hue, factor))
}

interface ChromaColor<T> {
    fun withChroma(chroma: Double): T

    val chroma: Double

    fun shiftChroma(shift: Double) = withChroma(chroma + shift)

    fun modulateChroma(factor: Double) = withChroma(chroma * factor)

    fun mixChroma(target: Double, factor: Double) = withChroma(chroma * (1.0 - factor) + target * factor)
}


interface SaturatableColor<T> {
    /** Multiply the saturation by a factor. */
    fun saturate(factor: Double): T = withSaturation(saturation * factor)

    fun withSaturation(saturation: Double): T

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