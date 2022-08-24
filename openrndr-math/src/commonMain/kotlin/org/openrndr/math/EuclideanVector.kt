package org.openrndr.math

import kotlin.math.pow
import kotlin.math.sqrt

interface EuclideanVector<T> : LinearType<T> where T : EuclideanVector<T>, T : LinearType<T> {
    /**
     * length in Euclidean space
     */
    val length: Double

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

    // returns the area of the parallelogram formed by extruding this over [other]
    fun areaBetween(other: T): Double {
        return sqrt(squaredLength * other.squaredLength - dot(other).pow(2.0))
    }

    /**
     * dot product between this and [other]
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


}