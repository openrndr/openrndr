package org.openrndr.math

interface LinearType<T : LinearType<T>> {
    operator fun plus(right: T): T
    operator fun minus(right: T): T
    operator fun times(scale: Double): T
    operator fun div(scale: Double): T
}