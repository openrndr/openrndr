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
data class Vector2(val x: Double, val y: Double) : Serializable, LinearType<Vector2> {

    constructor(x: Double) : this(x, x)

    /**
     * the Euclidean length of the vector
     */
    val length: Double
        get() = sqrt(x * x + y * y)

    /**
     * the squared Euclidean length of the vector
     */
    val squaredLength: Double
        get() = x * x + y * y


    /**
     * calculate perpendicular vector
     * @param polarity polarity, default is [YPolarity.CW_NEGATIVE_Y] (screen space)
     */
    fun perpendicular(polarity: YPolarity = YPolarity.CW_NEGATIVE_Y): Vector2 = when (polarity) {
        YPolarity.CCW_POSITIVE_Y -> Vector2(-y, x)
        YPolarity.CW_NEGATIVE_Y -> Vector2(y, -x)
    }

    /**
     * a normalized version of the vector
     */
    val normalized: Vector2
        get() {
            val localLength = length
            return if (localLength > 0.0) {
                this / length
            } else {
                Vector2.ZERO
            }
        }

    /**
     * calculate cross product between this [Vector2] and [right]
     */
    infix fun cross(right: Vector2) = x * right.y - y * right.x

    /**
     * calculate dot product between this [Vector2] and [right]
     */
    infix fun dot(right: Vector2) = x * right.x + y * right.y
    infix fun reflect(surfaceNormal: Vector2): Vector2 = this - surfaceNormal * (this dot surfaceNormal) * 2.0

    /**
     * calculate rotation of [Vector2]
     * @param degrees the rotation in degrees
     * @param origin the point around which the vector is rotated, default is [Vector2.ZERO]
     */
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
    val xy1 get() = Vector3(x, y, 1.0)
    val xy00 get() = Vector4(x, y, 0.0, 0.0)
    val xy01 get() = Vector4(x, y, 0.0, 1.0)

    /**
     * upcast to [Vector3]
     * @param x value for x component, default is [x]
     * @param y value for y component, default is [y]
     * @param z value for z component, default is 0.0
     */
    fun vector3(x: Double = this.x, y: Double = this.y, z: Double = 0.0): Vector3 {
        return Vector3(x, y, z)
    }

    /**
     * upcast to [Vector4]
     * @param x value for x component, default is [x]
     * @param y value for y component, default is [y]
     * @param z value for z component, default is 0.0
     * @param w value for w component, default is 0.0
     */
    fun vector4(x: Double = this.x, y: Double = this.y, z: Double = 0.0, w: Double = 0.0): Vector4 {
        return Vector4(x, y, z, w)
    }


    operator fun get(i: Int): Double {
        return when (i) {
            0 -> x
            1 -> y
            else -> throw RuntimeException("unsupported index")
        }
    }

    operator fun unaryMinus() = Vector2(-x, -y)

    override operator fun plus(vector2: Vector2) = Vector2(x + vector2.x, y + vector2.y)
    operator fun plus(d: Double) = Vector2(x + d, y + d)

    override operator fun minus(vector2: Vector2) = Vector2(x - vector2.x, y - vector2.y)
    operator fun minus(d: Double) = Vector2(x - d, y - d)

    override operator fun times(d: Double) = Vector2(x * d, y * d)
    operator fun times(v: Vector2) = Vector2(x * v.x, y * v.y)

    override operator fun div(d: Double) = Vector2(x / d, y / d)
    operator fun div(d: Vector2) = Vector2(x / d.x, y / d.y)

    /**
     * calculate the Euclidean distance to [other]
     */
    fun distanceTo(other: Vector2): Double {
        val dx = other.x - x
        val dy = other.y - y
        return sqrt(dx * dx + dy * dy)
    }

    /**
     * calculate the squared Euclidean distance to [other]
     */
    fun squaredDistanceTo(other: Vector2): Double {
        val dx = other.x - x
        val dy = other.y - y
        return dx * dx + dy * dy
    }

    fun mix(o: Vector2, mix: Double): Vector2 = this * (1 - mix) + o * mix

    companion object {
        val ZERO = Vector2(0.0, 0.0)
        val ONE = Vector2(1.0, 1.0)
        val UNIT_X = Vector2(1.0, 0.0)
        val UNIT_Y = Vector2(0.0, 1.0)

        /**
         * a [Vector2] representation for infinite values
         */
        val INFINITY = Vector2(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY)

        fun fromPolar(polar: Polar): Vector2 {
            val theta = toRadians(polar.theta)
            val x = cos(theta)
            val y = sin(theta)
            return Vector2(x, y) * polar.radius
        }
    }

    /**
     * convert to [DoubleArray]
     */
    fun toDoubleArray() = doubleArrayOf(x, y)

    /**
     * convert to [IntVector2]
     */
    fun toInt() = IntVector2(x.toInt(), y.toInt())
}

operator fun Double.times(v: Vector2) = v * this

fun min(a: Vector2, b: Vector2): Vector2 = Vector2(min(a.x, b.x), min(a.y, b.y))
fun max(a: Vector2, b: Vector2): Vector2 = Vector2(max(a.x, b.x), max(a.y, b.y))

fun mix(a: Vector2, b: Vector2, mix: Double): Vector2 = a * (1 - mix) + b * mix

