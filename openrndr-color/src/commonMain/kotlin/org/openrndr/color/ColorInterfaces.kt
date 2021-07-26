package org.openrndr.color

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
    /** Multiply the opacity by a factor */
    fun opacify(factor: Double): T
}

/**
 * Allows performing select algebraic operations on colors of this kind.
 */
interface AlgebraicColor<T : AlgebraicColor<T>> : LinearType<T> {
    override operator fun div(scale: Double): T = times(1.0 / scale)
    fun mix(other: T, factor: Double): T =
        ((this * (1.0 - factor)) as AlgebraicColor<T>) + (other as AlgebraicColor<T> * factor)
}