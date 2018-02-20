package org.openrndr.math

data class IntVector2(val x: Int, val y: Int) {
    val length: Double
        get() = Math.sqrt(1.0 * x * x + y * y)

    val squaredLength: Int
        get() = x* x + y * y


    infix fun dot(right: IntVector2) = x * right.x + y * right.y

    val yx: IntVector2 get() = IntVector2(y, x)
    val xx: IntVector2 get() = IntVector2(x, x)
    val yy: IntVector2 get() = IntVector2(y, y)
    operator fun plus(vector2: IntVector2) = IntVector2(x + vector2.x, y + vector2.y)
    operator fun minus(vector2: IntVector2) = IntVector2(x - vector2.x, y - vector2.y)
    operator fun times(d:Int) = IntVector2(x * d, y * d)
    operator fun div(d: Int) = IntVector2(x * d, y * d)

    companion object {
        val ZERO = IntVector2(0, 0)
        val UNIT_X = IntVector2(1, 0)
        val UNIT_Y = IntVector2(0, 1)
    }
}