package org.openrndr.math

/**
 * Represents a linear type that supports basic arithmetic operations for objects of the same type.
 * This interface enforces a linear structure that can be used to define custom mathematical types.
 *
 * @param T The type that implements this interface, constrained by itself to maintain type safety.
 */
interface LinearType<T : LinearType<T>> {
    /**
     * Adds the given value `right` to this value and returns the result.
     *
     * @param right the value to be added to this value
     * @return the result of adding the given value to this value
     */
    operator fun plus(right: T): T
    /**
     * Subtracts the specified value from the current value.
     *
     * @param right The value to be subtracted from the current value.
     * @return The result of the subtraction.
     */
    operator fun minus(right: T): T
    /**
     * Multiplies the current instance by the specified scalar value.
     *
     * @param scale The scalar factor by which the instance is multiplied.
     * @return An instance of type T scaled by the specified factor.
     */
    operator fun times(scale: Double): T
    /**
     * Divides the current object by the given scale factor.
     *
     * @param scale The divisor used to scale down the current object.
     * @return The result of the division as a new instance of type T.
     */
    operator fun div(scale: Double): T
}