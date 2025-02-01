package org.openrndr.math

/**
 * Represents a 1-dimensional parametric function that computes a value of type [R] based on a parameter [t] of type [Double].
 * This interface can be implemented to define various parametric mathematical or functional relationships.
 *
 * @param R The return type of the function, representing the computed value.
 */
interface Parametric1D<R> {
    fun value(t: Double) : R
}

/**
 * Parametric2D defines a two-dimensional parametric function.
 *
 * With this interface, any implementing class represents arbitrary
 * parametric functions that take two input parameters `u` and `v`
 * and return a result of type `R`.
 *
 * @param R The return type of the parametric function.
 */
interface Parametric2D<R> {
    fun value(u: Double, v : Double) : R
}

/**
 * Represents a three-dimensional parametric function.
 *
 * This interface models a parametric function that takes three parameters `u`, `v`, and `w`
 * representing input variables, and calculates a value of type `R` based on their values.
 *
 * @param R The return type of the parametric function.
 */
interface Parametric3D<R> {
    fun value(u: Double, v : Double, w : Double) : R
}

/**
 * Represents a parametrized function defined in a 4D space.
 * Provides the means to compute a value of type [R] given
 * four input parameters: [u], [v], [w], and [t], typically
 * representing dimensions or coordinates in the 4D space.
 *
 * @param R The return type of the computed parametric value.
 */
interface Parametric4D<R> {
    fun value(u: Double, v : Double, w : Double, t: Double) : R
}