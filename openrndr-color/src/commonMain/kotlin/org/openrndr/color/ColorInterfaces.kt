package org.openrndr.color

import org.openrndr.math.CastableToVector4
import org.openrndr.math.LinearType

interface ConvertibleToColorRGBa {
    /** Convert into [ColorRGBa]. */
    fun toRGBa(): ColorRGBa
}

interface ShadableColor<T> {
    /** Multiply the shade by a factor. */
    fun shade(factor: Double): T
}

interface HueShiftableColor<T> {
    /**
     * Shift the hue of a color by the given amount of degrees.
     * @param shiftInDegrees the hue shift in degrees
     */
    fun shiftHue(shiftInDegrees: Double): T
}

interface SaturatableColor<T> {
    /** Multiply the saturation by a factor. */
    fun saturate(factor: Double): T
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