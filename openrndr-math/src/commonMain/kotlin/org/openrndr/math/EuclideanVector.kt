package org.openrndr.math

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

    /**
     * dot product between this and [other]
     */
    infix fun dot(other: T): Double

    /**
     * project this vector on [on]
     */
    infix fun projectedOn(on: T) = on * ((this dot on) / (on dot on))

    /**
     * reflect this vector over [surfaceNormal]
     */
    infix fun reflectedOver(surfaceNormal: T) = this - surfaceNormal * (this dot surfaceNormal) * 2.0


}