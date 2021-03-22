package org.openrndr.math

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.math.*

enum class YPolarity {
    CCW_POSITIVE_Y,
    CW_NEGATIVE_Y
}

/** Double-precision 2D vector. */
@Serializable
data class Vector2(val x: Double, val y: Double) : LinearType<Vector2> {

    constructor(x: Double) : this(x, x)

    /** The Euclidean length of the vector. */
    val length: Double
        get() = sqrt(x * x + y * y)

    /** The squared Euclidean length of the vector. */
    val squaredLength: Double
        get() = x * x + y * y


    /**
     * Calculates a vector perpendicular to the current one.
     *
     * @param polarity The polarity of the new vector, default is [CW_NEGATIVE_Y][YPolarity.CW_NEGATIVE_Y].
     */
    fun perpendicular(polarity: YPolarity = YPolarity.CW_NEGATIVE_Y): Vector2 = when (polarity) {
        YPolarity.CCW_POSITIVE_Y -> Vector2(-y, x)
        YPolarity.CW_NEGATIVE_Y -> Vector2(y, -x)
    }

    /** Returns a normalized version of the vector. (i.e. unit vector) */
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
     * Calculates a cross product between this [Vector2] and [right].
     *
     * Technically you cannot find the
     * [cross product of two 2D vectors](https://stackoverflow.com/a/243984)
     * but it is still possible with clever use of mathematics.
     */
    infix fun cross(right: Vector2) = x * right.y - y * right.x

    /** Calculates a dot product between this [Vector2] and [right]. */
    infix fun dot(right: Vector2) = x * right.x + y * right.y

    infix fun reflect(surfaceNormal: Vector2): Vector2 = this - surfaceNormal * (this dot surfaceNormal) * 2.0

    /**
     * Creates a new [Vector2] with the given rotation and origin.
     *
     * @param degrees The rotation in degrees.
     * @param origin The point around which the vector is rotated, default is [Vector2.ZERO].
     */
    fun rotate(degrees: Double, origin: Vector2 = ZERO): Vector2 {
        val p = this - origin
        val a = degrees.asRadians

        val w = Vector2(
                p.x * cos(a) - p.y * sin(a),
                p.y * cos(a) + p.x * sin(a)
        )

        return w + origin
    }

    // these attributes doesn't have to be transients, but IntelliJ goes into some loop otherwise
    @Suppress("TRANSIENT_IS_REDUNDANT")
    @Transient
    val yx get() = Vector2(y, x)
    @Suppress("TRANSIENT_IS_REDUNDANT")
    @Transient
    val xx get() = Vector2(x, x)
    @Suppress("TRANSIENT_IS_REDUNDANT")
    @Transient
    val yy get() = Vector2(y, y)
    val xy0 get() = Vector3(x, y, 0.0)
    val xy1 get() = Vector3(x, y, 1.0)
    val xy00 get() = Vector4(x, y, 0.0, 0.0)
    val xy01 get() = Vector4(x, y, 0.0, 1.0)

    /**
     * Upcasts to [Vector3].
     *
     * @param x The x component value, default is [x].
     * @param y The y component value, default is [y].
     * @param z The z component value, default is `0.0`.
     */
    fun vector3(x: Double = this.x, y: Double = this.y, z: Double = 0.0): Vector3 {
        return Vector3(x, y, z)
    }

    /**
     * Upcasts to [Vector4].
     *
     * @param x The x component value, default is [x].
     * @param y The y component value, default is [y].
     * @param z The z component value, default is `0.0`.
     * @param w The w component value, default is `0.0`.
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

    /** Calculates the Euclidean distance to [other]. */
    fun distanceTo(other: Vector2): Double {
        val dx = other.x - x
        val dy = other.y - y
        return sqrt(dx * dx + dy * dy)
    }

    /** Calculates the squared Euclidean distance to [other]. */
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

        /** A [Vector2] representation for infinite values. */
        val INFINITY = Vector2(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY)

        fun fromPolar(polar: Polar): Vector2 {
            val theta = polar.theta.asRadians
            val x = cos(theta)
            val y = sin(theta)
            return Vector2(x, y) * polar.radius
        }
    }

    /** Casts to [DoubleArray]. */
    fun toDoubleArray() = doubleArrayOf(x, y)

    /** Casts to [IntVector2]. */
    fun toInt() = IntVector2(x.toInt(), y.toInt())
}

operator fun Double.times(v: Vector2) = v * this

fun min(a: Vector2, b: Vector2): Vector2 = Vector2(min(a.x, b.x), min(a.y, b.y))
fun max(a: Vector2, b: Vector2): Vector2 = Vector2(max(a.x, b.x), max(a.y, b.y))

fun mix(a: Vector2, b: Vector2, mix: Double): Vector2 = a * (1 - mix) + b * mix

