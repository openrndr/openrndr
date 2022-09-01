package org.openrndr.math

import kotlin.jvm.JvmInline
import kotlin.math.abs
import kotlin.math.pow

@JvmInline
value class Vector1(val x: Double) : EuclideanVector<Vector1> {
    override val zero: Vector1
        get() = ZERO
    override val length: Double
        get() = abs(x)
    override val squaredLength: Double
        get() = x*x

    override fun times(scale: Double): Vector1 {
        return Vector1(x * scale)
    }

    override fun div(scale: Double): Vector1 {
        return Vector1(x / scale)
    }

    override fun minus(right: Vector1): Vector1 {
        return Vector1(x - right.x)
    }

    override fun plus(right: Vector1): Vector1 {
        return Vector1(x + right.x)
    }

    override fun dot(right: Vector1): Double {
        return x * right.x
    }

    override fun squaredDistanceTo(other: Vector1): Double {
        val dx = x - other.x
        return dx * dx
    }

    override fun distanceTo(other: Vector1): Double {
        return abs(x - other.x)
    }

    override fun areaBetween(other: Vector1): Double {
        return abs(x - other.x).pow(2.0)
    }
    companion object {
        val ZERO = Vector1(0.0)
    }

}