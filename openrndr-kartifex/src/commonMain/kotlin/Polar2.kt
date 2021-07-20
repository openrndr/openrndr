package org.openrndr.kartifex

import org.openrndr.kartifex.Vec2
import kotlin.math.cos
import kotlin.math.sin

class Polar2(val theta: Double, val r: Double) {
    fun rotate(theta: Double): Polar2 {
        return Polar2(this.theta + theta, r)
    }

    fun vec2(): Vec2 {
        val x = cos(theta)
        val y = sin(theta)
        return Vec2(x * r, y * r)
    }
}
