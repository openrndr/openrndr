package org.openrndr.math

import java.io.Serializable
import java.lang.Math.toRadians
import kotlin.math.*

enum class YPolarity {
    CCW_POSITIVE_Y,
    CW_NEGATIVE_Y
}

/**
 * Double precision vector 2
 */
data class Vector2(val x: Double, val y: Double) : Serializable {

    constructor(x: Double) : this(x, x)

    operator fun invoke(x: Double = this.x, y: Double = this.y) = Vector2(x, y)

    val length: Double
        get() = sqrt(x * x + y * y)

    val squaredLength: Double
        get() = x * x + y * y


    fun perpendicular(polarity: YPolarity = YPolarity.CW_NEGATIVE_Y): Vector2 = when (polarity) {
        YPolarity.CCW_POSITIVE_Y -> Vector2(-y, x)
        YPolarity.CW_NEGATIVE_Y -> Vector2(y, -x)
    }

    val normalized: Vector2
        get() {
            val localLength = length
            return if (localLength > 0.0) {
                this / length
            } else {
                Vector2.ZERO
            }
        }

    infix fun dot(right: Vector2) = x * right.x + y * right.y
    infix fun reflect(surfaceNormal: Vector2): Vector2 = this - surfaceNormal * (this dot surfaceNormal) * 2.0

    fun rotate(degrees: Double, origin: Vector2 = ZERO): Vector2 {
        val p = this - origin
        val a = toRadians(degrees)

        val w = Vector2(
                p.x * cos(a) - p.y * sin(a),
                p.y * cos(a) + p.x * sin(a)
        )

        return w + origin
    }

    val yx get() = Vector2(y, x)
    val xx get() = Vector2(x, x)
    val yy get() = Vector2(y, y)
    val xy0 get() = Vector3(x, y, 0.0)
    val xy01 get() = Vector4(x, y, 0.0, 1.0)

    fun vector3(x: Double = this.x, y: Double = this.y, z: Double = 0.0): Vector3 {
        return Vector3(x, y, z)
    }

    operator fun get(i: Int): Double {
        return when (i) {
            0 -> x
            1 -> y
            else -> throw RuntimeException("unsupported index")
        }
    }

    operator fun unaryMinus() = Vector2(-x, -y)

    operator fun plus(vector2: Vector2) = Vector2(x + vector2.x, y + vector2.y)
    operator fun plus(d: Double) = Vector2(x + d, y + d)

    operator fun minus(vector2: Vector2) = Vector2(x - vector2.x, y - vector2.y)
    operator fun minus(d: Double) = Vector2(x - d, y - d)

    operator fun times(d: Double) = Vector2(x * d, y * d)
    operator fun times(v: Vector2) = Vector2(x * v.x, y * v.y)

    operator fun div(d: Double) = Vector2(x / d, y / d)
    operator fun div(d: Vector2) = Vector2(x / d.x, y / d.y)

    fun distanceTo(o: Vector2): Double {
        val dx = o.x - x
        val dy = o.y - y
        return sqrt(dx * dx + dy * dy)
    }

    fun squaredDistanceTo(o: Vector2): Double {
        val dx = o.x - x
        val dy = o.y - y
        return dx * dx + dy * dy
    }

    fun mix(o: Vector2, mix: Double): Vector2 = this * (1 - mix) + o * mix

    companion object {
        val ZERO = Vector2(0.0, 0.0)
        val ONE = Vector2(1.0, 1.0)
        val UNIT_X = Vector2(1.0, 0.0)
        val UNIT_Y = Vector2(0.0, 1.0)
        val INFINITY = Vector2(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY)

        fun fromPolar(polar: Polar): Vector2 {
            val theta = toRadians(polar.theta)
            val x = cos(theta)
            val y = sin(theta)

            return Vector2(x, y) * polar.radius
        }
    }
}

operator fun Double.times(v: Vector2) = v * this

fun min(a: Vector2, b: Vector2): Vector2 = Vector2(min(a.x, b.x), min(a.y, b.y))
fun max(a: Vector2, b: Vector2): Vector2 = Vector2(max(a.x, b.x), max(a.y, b.y))

fun mix(a: Vector2, b: Vector2, mix:Double): Vector2 = a * (1 - mix) + b * mix
