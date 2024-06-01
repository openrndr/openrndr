package org.openrndr.kartifex

import kotlin.jvm.JvmRecord
import kotlin.math.cos
import kotlin.math.sin

@JvmRecord
data class Polar2(val theta: Double, val r: Double) {
    fun rotate(theta: Double) = Polar2(this.theta + theta, r)

    fun vec2(): Vec2 {
        val x = cos(theta)
        val y = sin(theta)
        return Vec2(x * r, y * r)
    }
}
