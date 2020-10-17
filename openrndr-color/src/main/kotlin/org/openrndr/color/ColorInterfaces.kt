package org.openrndr.color

import org.openrndr.math.LinearType

interface ConvertibleToColorRGBa {
    fun toRGBa(): ColorRGBa
}

interface ShadableColor<T> {
    fun shade(factor: Double): T
}

interface HueShiftableColor<T> {
    fun shiftHue(shiftInDegrees: Double): T
}

interface SaturatableColor<T> {
    fun saturate(factor: Double): T
}

interface OpacifiableColor<T> {
    fun opacify(factor: Double): T
}

interface AlgebraicColor<T : AlgebraicColor<T>> : LinearType<T> {
    override operator fun div(factor: Double): T = times(1.0 / factor)
    fun mix(other: T, factor: Double): T = ((this * (1.0 - factor)) as AlgebraicColor<T>) + (other as AlgebraicColor<T> * factor)
}