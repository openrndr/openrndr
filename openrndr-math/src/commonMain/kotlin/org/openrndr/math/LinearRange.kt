package org.openrndr.math

import kotlin.jvm.JvmRecord

/**
 * Represents a linear range between two values, defined by a start and an end point,
 * where the type of the values implements the `LinearType` interface.
 * This class allows interpolation and evenly spaced steps within the range.
 *
 * @param T The type of the range's start and end values, constrained by the `LinearType` interface.
 * @property start The starting value of the range.
 * @property end The ending value of the range.
 */
@JvmRecord
data class LinearRange1D<T : LinearType<T>>(val start: T, val end: T) :
    LinearType<LinearRange1D<T>>,
    Parametric1D<T> {
    /**
     * Computes a value interpolated linearly between the start and end points of the range based on the parameter `t`.
     *
     * @param t A parameter in the range [0.0, 1.0]. When `t` is 0.0, the result is the start value.
     *          When `t` is 1.0, the result is the end value. Intermediate values of `t` result in a linear interpolation.
     * @return The interpolated value between the start and end points of the range.
     */
    override fun value(t: Double) = start * (1.0 - t) + end * t

    fun steps(count: Int): Sequence<T> = sequence {
        for (i in 0 until count) {
            val t = i / (count - 1.0)
            yield(value(t))
        }
    }

    override fun plus(right: LinearRange1D<T>): LinearRange1D<T> =
        copy(start = start + right.start, end = end + right.end)

    override fun minus(right: LinearRange1D<T>): LinearRange1D<T> =
        copy(start = start - right.start, end = end - right.end)

    override fun times(scale: Double): LinearRange1D<T> =
        copy(start = start * scale, end = end * scale)

    override fun div(scale: Double): LinearRange1D<T> =
        copy(start = start / scale, end = end / scale)
}

/**
 * Creates a range from the current linear type instance to the specified end value.
 *
 * @param end The end value of the range.
 * @return A LinearRange that represents the range from the current instance to the specified end value.
 */
operator fun <T : LinearType<T>> LinearType<T>.rangeTo(end: T): LinearRange1D<T> = LinearRange1D(this as T, end)

/**
 * Represents a two-dimensional linear range defined by two one-dimensional linear ranges,
 * where the `start` and `end` ranges provide endpoints for interpolation.
 *
 * This class enables bilinear interpolation between the `start` and `end` ranges
 * based on parameters `u` and `v`.
 *
 * @param T The type of the values being interpolated, constrained by the `LinearType` interface.
 * @property start The starting one-dimensional linear range.
 * @property end The ending one-dimensional linear range.
 */
@JvmRecord
data class LinearRange2D<T : LinearType<T>>(val start: LinearRange1D<T>, val end: LinearRange1D<T>) :
    LinearType<LinearRange2D<T>>,
    Parametric2D<T> {
    override fun value(u: Double, v: Double) = start.value(u) * (1.0 - v) + end.value(u) * v

    override fun plus(right: LinearRange2D<T>): LinearRange2D<T> =
        copy(start = start + right.start, end = end + right.end)

    override fun minus(right: LinearRange2D<T>): LinearRange2D<T> =
        copy(start = start - right.start, end = end - right.end)

    override fun times(scale: Double): LinearRange2D<T> =
        copy(start = start * scale, end = end * scale)

    override fun div(scale: Double): LinearRange2D<T> =
        copy(start = start / scale, end = end / scale)
}

/**
 * Creates a `LinearRange2D` instance using this `LinearRange1D` as the starting range
 * and the specified `end` as the ending range.
 *
 * @param end The ending `LinearRange1D` to create a 2D range.
 * @return A `LinearRange2D` instance representing the range from this starting range to the specified ending range.
 */
operator fun <T : LinearType<T>> LinearRange1D<T>.rangeTo(end: LinearRange1D<T>): LinearRange2D<T> {
    return LinearRange2D(this, end)
}

/**
 * Represents a three-dimensional linear range defined by two two-dimensional linear ranges,
 * where the `start` and `end` ranges provide endpoints for trilinear interpolation.
 *
 * This class allows for interpolation across three dimensions using the parameters `u`, `v`, and `w`.
 *
 * @param T The type of the values being interpolated, constrained by the `LinearType` interface.
 * @property start The starting two-dimensional linear range.
 * @property end The ending two-dimensional linear range.
 */
@JvmRecord
data class LinearRange3D<T : LinearType<T>>(val start: LinearRange2D<T>, val end: LinearRange2D<T>) :
    LinearType<LinearRange3D<T>>, Parametric3D<T> {
    override fun value(u: Double, v: Double, w: Double) = start.value(u, v) * (1.0 - w) + end.value(u, v) * w

    override fun plus(right: LinearRange3D<T>): LinearRange3D<T> =
        copy(start = start + right.start, end = end + right.end)

    override fun minus(right: LinearRange3D<T>): LinearRange3D<T> =
        copy(start = start - right.start, end = end - right.end)

    override fun times(scale: Double): LinearRange3D<T> =
        copy(start = start * scale, end = end * scale)

    override fun div(scale: Double): LinearRange3D<T> =
        copy(start = start / scale, end = end / scale)
}

/**
 * Creates a 3D linear range from the current 2D linear range to the specified 2D linear range.
 *
 * @param end The ending 2D linear range to define the 3D linear range.
 * @return A new instance of LinearRange3D representing the range from the current 2D range to the specified end range.
 */
operator fun <T : LinearType<T>> LinearRange2D<T>.rangeTo(end: LinearRange2D<T>): LinearRange3D<T> =
    LinearRange3D(this, end)

/**
 * Represents a four-dimensional linear range defined by two three-dimensional linear ranges,
 * providing endpoints for quadrilinear interpolation.
 *
 * This class supports interpolation across four dimensions using the parameters `u`, `v`, `w`, and `t`.
 * The interpolation is computed as a combination of the `start` and `end` LinearRange3D objects:
 * - The `start` LinearRange3D is weighted by `(1.0 - t)`
 * - The `end` LinearRange3D is weighted by `t`
 *
 * @param T The type of the values being interpolated, constrained by the `LinearType` interface.
 * @property start The starting three-dimensional linear range.
 * @property end The ending three-dimensional linear range.
 */
@JvmRecord
data class LinearRange4D<T : LinearType<T>>(val start: LinearRange3D<T>, val end: LinearRange3D<T>) :
    LinearType<LinearRange4D<T>>,
    Parametric4D<T> {
    override fun value(u: Double, v: Double, w: Double, t: Double) =
        start.value(u, v, w) * (1.0 - t) + end.value(u, v, w) * t

    override fun plus(right: LinearRange4D<T>): LinearRange4D<T> =
        copy(start = start + right.start, end = end + right.end)

    override fun minus(right: LinearRange4D<T>): LinearRange4D<T> =
        copy(start = start - right.start, end = end - right.end)

    override fun times(scale: Double): LinearRange4D<T> =
        copy(start = start * scale, end = end * scale)

    override fun div(scale: Double): LinearRange4D<T> =
        copy(start = start / scale, end = end / scale)
}

/**
 * Creates a LinearRange4D object representing the range between this LinearRange4D instance and the specified end LinearRange3D instance.
 *
 * @param end The ending LinearRange3D instance that defines the range.
 * @return A LinearRange4D object representing the range from this instance to the specified end instance.
 */
operator fun <T : LinearType<T>> LinearRange3D<T>.rangeTo(end: LinearRange3D<T>): LinearRange4D<T> {
    return LinearRange4D(this, end)
}