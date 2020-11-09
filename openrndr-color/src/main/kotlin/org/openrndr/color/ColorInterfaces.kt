package org.openrndr.color

import org.openrndr.math.LinearType

/**
 * interface for colors that can be converted to [ColorRGBa]
 */
interface ConvertibleToColorRGBa {
    /**
     * convert to [ColorRGBa]
     */
    fun toRGBa(): ColorRGBa
}

/**
 * interface for shadable colors
 */
interface ShadableColor<T> {
    /**
     * shades the color by multiplication
     * @param factor the shade factor
     */
    fun shade(factor: Double): T
}

/**
 * interface for hue-shiftable colors
 */
interface HueShiftableColor<T> {
    /**
     * shift the hue by a given amount of degrees
     * @param shiftInDegrees the hue shift in degrees
     */
    fun shiftHue(shiftInDegrees: Double): T
}

/**
 * interface for saturatable color
 */
interface SaturatableColor<T> {
    /**
     * adjust saturation by multiplication
     */
    fun saturate(factor: Double): T
}

/**
 * interface for opacifable color
 */
interface OpacifiableColor<T> {
    /**
     * adjust opacity by multiplication
     */
    fun opacify(factor: Double): T
}

/**
 * interface for algebraic color
 */
interface AlgebraicColor<T : AlgebraicColor<T>> : LinearType<T> {
    override operator fun div(factor: Double): T = times(1.0 / factor)
    fun mix(other: T, factor: Double): T = ((this * (1.0 - factor)) as AlgebraicColor<T>) + (other as AlgebraicColor<T> * factor)
}