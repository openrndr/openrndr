package org.openrndr.color

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

interface AlgebraicColor<T : AlgebraicColor<T>> {
    operator fun plus(other: T): T
    operator fun minus(other: T): T
    operator fun times(factor: Double): T
    fun mix(other: T, factor: Double): T = ((this * (1.0 - factor)) as AlgebraicColor<T>) + (other as AlgebraicColor<T> * factor)
}