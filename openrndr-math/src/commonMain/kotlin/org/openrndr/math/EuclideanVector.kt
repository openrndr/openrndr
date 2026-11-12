package org.openrndr.math

import kotlin.math.sqrt


interface EuclideanVector<T> : LinearType<T> where T : EuclideanVector<T>, T : LinearType<T> {

    /**
     * returns the zero vector
     */
    val zero: T

    /**
     * length in Euclidean space
     */
    val length: Double

    /**
     * normalized vector
     */
    val normalized: T
        get() {
            val l = length
            return if (l == 0.0) {
                zero
            } else {
                this / l
            }
        }

    /**
     * squared length in Euclidean space
     */
    val squaredLength: Double

    /**
     * distance to [other] in Euclidean space
     */
    fun distanceTo(other: T): Double

    /**
     * squared distance to [other] in Euclidean space
     */
    fun squaredDistanceTo(other: T): Double

    /** returns the area of the parallelogram formed by extruding this over [other] */
    fun areaBetween(other: T): Double {
        val lengths2 = squaredLength * other.squaredLength
        val dot2 = dot(other).let { it * it }
        // the Lagrange identity loses at most a bit between 45 and 135 degrees
        if (dot2 <= 0.5 * lengths2) {
            return sqrt(lengths2 - dot2)
        }
        // it cancels for nearly parallel vectors: |a| |b| sin(angle), the sine from the unit vectors u and v as
        // |u - v| |u + v| / 2, which is exact for parallel and opposite vectors
        val la = length
        val lb = other.length
        val u = this / la
        val v = other / lb
        return la * lb * (u - v).length * (u + v).length * 0.5
    }

    /**
     * dot product between this and [right]
     */
    infix fun dot(right: T): Double

    /**
     * project this vector on [on]
     */
    infix fun projectedOn(on: T) = on * ((this dot on) / (on dot on))

    /**
     * reflect this vector over [surfaceNormal]
     */
    infix fun reflectedOver(surfaceNormal: T) = this - surfaceNormal * (this dot surfaceNormal) * 2.0

    /**
     * atan2 style angle between this and [other]
     */
    fun atan2(other: T) : Double {
        val u = this.normalized
        val v = other.normalized
        if (u.squaredLength == 0.0 || v.squaredLength == 0.0) {
            return kotlin.math.atan2(1.0, 0.0)
        }
        // the angle from the lengths of the difference and sum of the unit vectors is accurate for all angles
        return 2.0 * kotlin.math.atan2((u - v).length, (u + v).length)
    }

    /**
     * Applies a given function to each component of the vector and returns a new vector
     * with the transformed components.
     *
     * @param function A lambda function that takes a Double and returns a Double,
     * applied to each component of the vector.
     * @return A new vector of the same type with components transformed by the given function.
     */
    fun map(function: (Double)->Double) : T

}