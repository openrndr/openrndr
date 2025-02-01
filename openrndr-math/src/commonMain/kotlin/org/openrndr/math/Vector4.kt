package org.openrndr.math

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmRecord
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** Double-precision 4D vector. */
@Serializable
@JvmRecord
data class Vector4(val x: Double, val y: Double, val z: Double, val w: Double) : LinearType<Vector4>, EuclideanVector<Vector4> {
    constructor(x: Double) : this(x, x, x, x)

    val xy: Vector2 get() = Vector2(x, y)
    val yx: Vector2 get() = Vector2(y, x)
    val xz: Vector2 get() = Vector2(x, z)
    val yz: Vector2 get() = Vector2(y, z)
    val zx: Vector2 get() = Vector2(z, x)
    val zy: Vector2 get() = Vector2(z, y)

    /** Downcasts to [Vector3] by discarding [w]. */
    val xyz: Vector3 get() = Vector3(x, y, z)

    /** Calculates [Vector3] by dividing [x], [y], [z] by [w]. */
    val div: Vector3 get() = Vector3(x / w, y / w, z / w)

    /** The Euclidean length of the vector. */
    override val length get() = sqrt(x * x + y * y + z * z + w * w)

    /** The squared Euclidean length of the vector. */
    override val squaredLength get() = x * x + y * y + z * z + w * w

    override fun map(function: (Double) -> Double): Vector4 {
        return Vector4(function(x), function(y), function(z), function(w))
    }

    override val zero: Vector4 get() = ZERO

    companion object {
        /**
         * A constant representing the unit vector along the X axis in four-dimensional space.
         *
         * This vector has a value of (1.0, 0.0, 0.0, 0.0), where the first component corresponds
         * to the X axis and the remaining components (Y, Z, W) are zero. It can be used as a
         * basis vector or a reference for transformations and operations within the [Vector4]
         * context.
         */
        val UNIT_X = Vector4(1.0, 0.0, 0.0, 0.0)
        /**
         * Represents a unit vector in the Y-axis direction in 4-dimensional space.
         * It has the components (0.0, 1.0, 0.0, 0.0).
         */
        val UNIT_Y = Vector4(0.0, 1.0, 0.0, 0.0)
        val UNIT_Z = Vector4(0.0, 0.0, 1.0, 0.0)
        val UNIT_W = Vector4(0.0, 0.0, 0.0, 1.0)
        val ZERO = Vector4(0.0, 0.0, 0.0, 0.0)
        val ONE = Vector4(1.0, 1.0, 1.0, 1.0)
        /**
         * A constant representing a [Vector4] where all components (x, y, z, w)
         * are initialized to positive infinity (`Double.POSITIVE_INFINITY`).
         *
         * This can be useful as a representation of an unbounded or maximum value
         * in calculations involving 4-dimensional vectors.
         */
        val INFINITY = Vector4(
            Double.POSITIVE_INFINITY,
            Double.POSITIVE_INFINITY,
            Double.POSITIVE_INFINITY,
            Double.POSITIVE_INFINITY
        )
    }


    operator fun unaryMinus() = Vector4(-x, -y, -z, -w)

    override operator fun plus(right: Vector4) = Vector4(x + right.x, y + right.y, z + right.z, w + right.w)
    operator fun plus(d: Double) = Vector4(x + d, y + d, z + d, w + d)
    override operator fun minus(right: Vector4) = Vector4(x - right.x, y - right.y, z - right.z, w - right.w)
    operator fun minus(d: Double) = Vector4(x - d, y - d, z - d, w - d)
    operator fun times(v: Vector4) = Vector4(x * v.x, y * v.y, z * v.z, w * v.w)
    override operator fun times(scale: Double) = Vector4(x * scale, y * scale, z * scale, w * scale)
    operator fun div(v: Vector4) = Vector4(x / v.x, y / v.y, z / v.z, w / v.w)
    override operator fun div(scale: Double) = Vector4(x / scale, y / scale, z / scale, w / scale)

    /** Calculates a dot product between this [Vector4] and [right]. */
    override infix fun dot(right: Vector4): Double = x * right.x + y * right.y + z * right.z + w * right.w

    /**
     * Retrieves the value corresponding to the provided index.
     *
     * @param i The index of the value to retrieve. Valid indices are 0, 1, 2, and 3.
     * @return The value at the specified index as a Double.
     * @throws IllegalArgumentException If the index is not within the supported range (0-3).
     */
    operator fun get(i: Int): Double {
        return when (i) {
            0 -> x
            1 -> y
            2 -> z
            3 -> w
            else -> throw IllegalArgumentException("unsupported index")
        }
    }

    /** Calculates the Euclidean distance to [other]. */
    override fun distanceTo(other: Vector4): Double {
        val dx = other.x - x
        val dy = other.y - y
        val dz = other.z - z
        val dw = other.z - z
        return sqrt(dx * dx + dy * dy + dz * dz + dw * dw)
    }

    /** Calculates the squared Euclidean distance to [other]. */
    override fun squaredDistanceTo(other: Vector4): Double {
        val dx = other.x - x
        val dy = other.y - y
        val dz = other.z - z
        val dw = other.w - w
        return dx * dx + dy * dy + dz * dz + dw * dw
    }

    /**
     * Performs a linear interpolation between this [Vector4] and another [Vector4] `o`
     * by a given mixing factor `mix`.
     *
     * @param o The target [Vector4] to interpolate to.
     * @param mix The mixing factor, where 0.0 corresponds to this [Vector4],
     *            and 1.0 corresponds to the target [Vector4] `o`.
     * @return A new [Vector4] representing the result of the interpolation.
     */
    fun mix(o: Vector4, mix: Double): Vector4 = this * (1 - mix) + o * mix

    /** Casts to [DoubleArray]. */
    fun toDoubleArray() = doubleArrayOf(x, y, z, w)

    /** Casts to [IntVector4]. */
    fun toInt() = IntVector4(x.toInt(), y.toInt(), z.toInt(), w.toInt())
}

operator fun Double.times(v: Vector4) = v * this

fun min(a: Vector4, b: Vector4): Vector4 = Vector4(min(a.x, b.x), min(a.y, b.y), min(a.z, b.z), min(a.w, b.w))
fun max(a: Vector4, b: Vector4): Vector4 = Vector4(max(a.x, b.x), max(a.y, b.y), max(a.z, b.z), max(a.w, b.w))

fun mix(a: Vector4, b: Vector4, mix:Double): Vector4 = a * (1 - mix) + b * mix

/**
 * An interface representing a type that can be converted into a [Vector4].
 * This allows classes implementing this interface to define a custom transformation
 * or mapping to a `Vector4` instance, enabling compatibility with systems and operations
 * that utilize 4-dimensional vectors.
 */
interface CastableToVector4 {
    /**
     * Converts the implementing type into a [Vector4] representation.
     *
     * @return a [Vector4] instance representing the current type's data as a 4-dimensional vector.
     */
    fun toVector4() : Vector4
}

/**
 * Calculates the summation of all vectors in the iterable.
 *
 * Iterates through the collection of `Vector4` and computes the component-wise
 * sum for each dimension (x, y, z, w).
 *
 * @return A `Vector4` representing the sum of all vectors in the iterable.
 *         If the iterable is empty, returns a `Vector4` with all components set to 0.0.
 */
fun Iterable<Vector4>.sum() : Vector4 {
    var x = 0.0
    var y = 0.0
    var z = 0.0
    var w = 0.0
    for (v in this) {
        x += v.x
        y += v.y
        z += v.z
        w += v.w
    }
    return Vector4(x, y, z, w)
}

/**
 * Calculates the component-wise average of all `Vector4` elements in the iterable.
 *
 * @return A `Vector4` representing the average of all vectors in the iterable.
 *         If the iterable is empty, this will result in a divide-by-zero error.
 */
fun Iterable<Vector4>.average() : Vector4 {
    var x = 0.0
    var y = 0.0
    var z = 0.0
    var w = 0.0
    var count = 0
    for (v in this) {
        x += v.x
        y += v.y
        z += v.z
        w += v.w
        count++
    }
    return Vector4(x / count, y / count, z / count, w / count)
}