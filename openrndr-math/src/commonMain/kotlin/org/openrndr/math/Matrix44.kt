package org.openrndr.math

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * A 4x4 matrix with double precision
 */
@Serializable
data class Matrix44(
        val c0r0: Double = 0.0, val c1r0: Double = 0.0, val c2r0: Double = 0.0, val c3r0: Double = 0.0,
        val c0r1: Double = 0.0, val c1r1: Double = 0.0, val c2r1: Double = 0.0, val c3r1: Double = 0.0,
        val c0r2: Double = 0.0, val c1r2: Double = 0.0, val c2r2: Double = 0.0, val c3r2: Double = 0.0,
        val c0r3: Double = 0.0, val c1r3: Double = 0.0, val c2r3: Double = 0.0, val c3r3: Double = 0.0) : LinearType<Matrix44> {

    companion object {
        /**
         * 4x4 identity matrix
         */
        val IDENTITY = Matrix44(c0r0 = 1.0, c1r1 = 1.0, c2r2 = 1.0, c3r3 = 1.0)

        /**
         * 4x4 zero matrix
         */
        val ZERO = Matrix44()

        /**
         * Creates a 4x4 matrix from column vectors.
         *
         * @param c0 The first column vector
         * @param c1 The second column vector
         * @param c2 The third column vector
         * @param c3 The fourth column vector
         */
        fun fromColumnVectors(c0: Vector4, c1: Vector4, c2: Vector4, c3: Vector4): Matrix44 =
                Matrix44(c0.x, c1.x, c2.x, c3.x,
                        c0.y, c1.y, c2.y, c3.y,
                        c0.z, c1.z, c2.z, c3.z,
                        c0.w, c1.w, c2.w, c3.w)


        /**
         * Constructs a matrix from `DoubleArray` with values in row-major order.
         */
        fun fromDoubleArray(a: DoubleArray): Matrix44 {
            require(a.size >= 16) { "input array is too short (${a.size}}, should have at least a length of 16)" }
            return Matrix44(
                    a[0], a[1], a[2], a[3],
                    a[4], a[5], a[6], a[7],
                    a[8], a[9], a[10], a[11],
                    a[12], a[13], a[14], a[15]
            )
        }
    }

    /**
     * Convert matrix to a `DoubleArray` in row-major order
     */
    fun toDoubleArray(): DoubleArray {
        return doubleArrayOf(
                c0r0, c1r0, c2r0, c3r0,
                c0r1, c1r1, c2r1, c3r1,
                c0r2, c1r2, c2r2, c3r2,
                c0r3, c1r3, c2r3, c3r3)
    }

    /**
     * Returns a column vector
     */
    operator fun get(index: Int) =
            when (index) {
                0 -> Vector4(c0r0, c0r1, c0r2, c0r3)
                1 -> Vector4(c1r0, c1r1, c1r2, c1r3)
                2 -> Vector4(c2r0, c2r1, c2r2, c2r3)
                3 -> Vector4(c3r0, c3r1, c3r2, c3r3)
                else -> throw RuntimeException("not implemented")
            }

    /**
     * Inversed version of the 4x4 matrix
     */
    @Suppress("TRANSIENT_IS_REDUNDANT")
    @Transient
    val inversed: Matrix44
        get() {
            if (this === IDENTITY) {
                return this
            }
            val n00 = c1r2 * c2r3 * c3r1 - c1r3 * c2r2 * c3r1 + c1r3 * c2r1 * c3r2 - c1r1 * c2r3 * c3r2 - c1r2 * c2r1 * c3r3 + c1r1 * c2r2 * c3r3
            val n01 = c0r3 * c2r2 * c3r1 - c0r2 * c2r3 * c3r1 - c0r3 * c2r1 * c3r2 + c0r1 * c2r3 * c3r2 + c0r2 * c2r1 * c3r3 - c0r1 * c2r2 * c3r3
            val n02 = c0r2 * c1r3 * c3r1 - c0r3 * c1r2 * c3r1 + c0r3 * c1r1 * c3r2 - c0r1 * c1r3 * c3r2 - c0r2 * c1r1 * c3r3 + c0r1 * c1r2 * c3r3
            val n03 = c0r3 * c1r2 * c2r1 - c0r2 * c1r3 * c2r1 - c0r3 * c1r1 * c2r2 + c0r1 * c1r3 * c2r2 + c0r2 * c1r1 * c2r3 - c0r1 * c1r2 * c2r3
            val n10 = c1r3 * c2r2 * c3r0 - c1r2 * c2r3 * c3r0 - c1r3 * c2r0 * c3r2 + c1r0 * c2r3 * c3r2 + c1r2 * c2r0 * c3r3 - c1r0 * c2r2 * c3r3
            val n11 = c0r2 * c2r3 * c3r0 - c0r3 * c2r2 * c3r0 + c0r3 * c2r0 * c3r2 - c0r0 * c2r3 * c3r2 - c0r2 * c2r0 * c3r3 + c0r0 * c2r2 * c3r3
            val n12 = c0r3 * c1r2 * c3r0 - c0r2 * c1r3 * c3r0 - c0r3 * c1r0 * c3r2 + c0r0 * c1r3 * c3r2 + c0r2 * c1r0 * c3r3 - c0r0 * c1r2 * c3r3
            val n13 = c0r2 * c1r3 * c2r0 - c0r3 * c1r2 * c2r0 + c0r3 * c1r0 * c2r2 - c0r0 * c1r3 * c2r2 - c0r2 * c1r0 * c2r3 + c0r0 * c1r2 * c2r3
            val n20 = c1r1 * c2r3 * c3r0 - c1r3 * c2r1 * c3r0 + c1r3 * c2r0 * c3r1 - c1r0 * c2r3 * c3r1 - c1r1 * c2r0 * c3r3 + c1r0 * c2r1 * c3r3
            val n21 = c0r3 * c2r1 * c3r0 - c0r1 * c2r3 * c3r0 - c0r3 * c2r0 * c3r1 + c0r0 * c2r3 * c3r1 + c0r1 * c2r0 * c3r3 - c0r0 * c2r1 * c3r3
            val n22 = c0r1 * c1r3 * c3r0 - c0r3 * c1r1 * c3r0 + c0r3 * c1r0 * c3r1 - c0r0 * c1r3 * c3r1 - c0r1 * c1r0 * c3r3 + c0r0 * c1r1 * c3r3
            val n23 = c0r3 * c1r1 * c2r0 - c0r1 * c1r3 * c2r0 - c0r3 * c1r0 * c2r1 + c0r0 * c1r3 * c2r1 + c0r1 * c1r0 * c2r3 - c0r0 * c1r1 * c2r3
            val n30 = c1r2 * c2r1 * c3r0 - c1r1 * c2r2 * c3r0 - c1r2 * c2r0 * c3r1 + c1r0 * c2r2 * c3r1 + c1r1 * c2r0 * c3r2 - c1r0 * c2r1 * c3r2
            val n31 = c0r1 * c2r2 * c3r0 - c0r2 * c2r1 * c3r0 + c0r2 * c2r0 * c3r1 - c0r0 * c2r2 * c3r1 - c0r1 * c2r0 * c3r2 + c0r0 * c2r1 * c3r2
            val n32 = c0r2 * c1r1 * c3r0 - c0r1 * c1r2 * c3r0 - c0r2 * c1r0 * c3r1 + c0r0 * c1r2 * c3r1 + c0r1 * c1r0 * c3r2 - c0r0 * c1r1 * c3r2
            val n33 = c0r1 * c1r2 * c2r0 - c0r2 * c1r1 * c2r0 + c0r2 * c1r0 * c2r1 - c0r0 * c1r2 * c2r1 - c0r1 * c1r0 * c2r2 + c0r0 * c1r1 * c2r2

            val d = determinant
            return Matrix44(n00 / d, n10 / d, n20 / d, n30 / d,
                    n01 / d, n11 / d, n21 / d, n31 / d,
                    n02 / d, n12 / d, n22 / d, n32 / d,
                    n03 / d, n13 / d, n23 / d, n33 / d)
        }

    /**
     * The trace of the 4x4 matrix
     */
    @Suppress("TRANSIENT_IS_REDUNDANT")
    @Transient
    val trace get() = c0r0 + c1r1 + c2r2 + c3r3

    @Suppress("TRANSIENT_IS_REDUNDANT")
    @Transient
    val determinant
        get() = c0r3 * c1r2 * c2r1 * c3r0 - c0r2 * c1r3 * c2r1 * c3r0 - c0r3 * c1r1 * c2r2 * c3r0 + c0r1 * c1r3 * c2r2 * c3r0 +
                c0r2 * c1r1 * c2r3 * c3r0 - c0r1 * c1r2 * c2r3 * c3r0 - c0r3 * c1r2 * c2r0 * c3r1 + c0r2 * c1r3 * c2r0 * c3r1 +
                c0r3 * c1r0 * c2r2 * c3r1 - c0r0 * c1r3 * c2r2 * c3r1 - c0r2 * c1r0 * c2r3 * c3r1 + c0r0 * c1r2 * c2r3 * c3r1 +
                c0r3 * c1r1 * c2r0 * c3r2 - c0r1 * c1r3 * c2r0 * c3r2 - c0r3 * c1r0 * c2r1 * c3r2 + c0r0 * c1r3 * c2r1 * c3r2 +
                c0r1 * c1r0 * c2r3 * c3r2 - c0r0 * c1r1 * c2r3 * c3r2 - c0r2 * c1r1 * c2r0 * c3r3 + c0r1 * c1r2 * c2r0 * c3r3 +
                c0r2 * c1r0 * c2r1 * c3r3 - c0r0 * c1r2 * c2r1 * c3r3 - c0r1 * c1r0 * c2r2 * c3r3 + c0r0 * c1r1 * c2r2 * c3r3

    /**
     * Matrix addition
     * @param right the other matrix
     */
    override operator fun plus(right: Matrix44) = Matrix44(
            c0r0 + right.c0r0, c1r0 + right.c1r0, c2r0 + right.c2r0, c3r0 + right.c3r0,
            c0r1 + right.c0r1, c1r1 + right.c1r1, c2r1 + right.c2r1, c3r1 + right.c3r1,
            c0r2 + right.c0r2, c1r2 + right.c1r2, c2r2 + right.c2r2, c3r2 + right.c3r2,
            c0r3 + right.c0r3, c1r3 + right.c1r3, c2r3 + right.c2r3, c3r3 + right.c3r3)

    /**
     * Matrix subtraction
     * @param right the other matrix
     */
    override operator fun minus(right: Matrix44) = Matrix44(
            c0r0 - right.c0r0, c1r0 - right.c1r0, c2r0 - right.c2r0, c3r0 - right.c3r0,
            c0r1 - right.c0r1, c1r1 - right.c1r1, c2r1 - right.c2r1, c3r1 - right.c3r1,
            c0r2 - right.c0r2, c1r2 - right.c1r2, c2r2 - right.c2r2, c3r2 - right.c3r2,
            c0r3 - right.c0r3, c1r3 - right.c1r3, c2r3 - right.c2r3, c3r3 - right.c3r3)


    /**
     * Returns a transposed version of the matrix
     */
    @Suppress("TRANSIENT_IS_REDUNDANT")
    @Transient
    val transposed
        get() = Matrix44(
                c0r0, c0r1, c0r2, c0r3,
                c1r0, c1r1, c1r2, c1r3,
                c2r0, c2r1, c2r2, c2r3,
                c3r0, c3r1, c3r2, c3r3)

    /**
     * The 3x3 top-left part of the 4x4 matrix
     */
    @Suppress("TRANSIENT_IS_REDUNDANT")
    @Transient
    val matrix33
        get() = Matrix33(c0r0, c1r0, c2r0,
                c0r1, c1r1, c2r1,
                c0r2, c1r2, c2r2)

    /**
     * Multiplies the 4x4 matrix with a vector 4
     */
    operator fun times(v: Vector4) = Vector4(
            v.x * c0r0 + v.y * c1r0 + v.z * c2r0 + v.w * c3r0,
            v.x * c0r1 + v.y * c1r1 + v.z * c2r1 + v.w * c3r1,
            v.x * c0r2 + v.y * c1r2 + v.z * c2r2 + v.w * c3r2,
            v.x * c0r3 + v.y * c1r3 + v.z * c2r3 + v.w * c3r3)

    /**
     * Multiplies all the elements in the 4x4 matrix with a scalar
     */
    override operator fun times(scale: Double) = Matrix44(scale * c0r0, scale * c1r0, scale * c2r0, scale * c3r0,
            scale * c0r1, scale * c1r1, scale * c2r1, scale * c3r1,
            scale * c0r2, scale * c1r2, scale * c2r2, scale * c3r2,
            scale * c0r3, scale * c1r3, scale * c2r3, scale * c3r3)

    override operator fun div(scale: Double) = Matrix44(scale / c0r0, scale / c1r0, scale / c2r0, scale / c3r0,
            scale / c0r1, scale / c1r1, scale / c2r1, scale / c3r1,
            scale / c0r2, scale / c1r2, scale / c2r2, scale / c3r2,
            scale / c0r3, scale / c1r3, scale / c2r3, scale / c3r3)


    /**
     * Matrix concatenation
     */
    operator fun times(mat: Matrix44) = when {
        this === IDENTITY -> mat
        mat === IDENTITY -> this
        else -> Matrix44(
                this.c0r0 * mat.c0r0 + this.c1r0 * mat.c0r1 + this.c2r0 * mat.c0r2 + this.c3r0 * mat.c0r3, // m00
                this.c0r0 * mat.c1r0 + this.c1r0 * mat.c1r1 + this.c2r0 * mat.c1r2 + this.c3r0 * mat.c1r3, // c1r0
                this.c0r0 * mat.c2r0 + this.c1r0 * mat.c2r1 + this.c2r0 * mat.c2r2 + this.c3r0 * mat.c2r3, // c2r0
                this.c0r0 * mat.c3r0 + this.c1r0 * mat.c3r1 + this.c2r0 * mat.c3r2 + this.c3r0 * mat.c3r3, // c3r0

                this.c0r1 * mat.c0r0 + this.c1r1 * mat.c0r1 + this.c2r1 * mat.c0r2 + this.c3r1 * mat.c0r3, // c0r1
                this.c0r1 * mat.c1r0 + this.c1r1 * mat.c1r1 + this.c2r1 * mat.c1r2 + this.c3r1 * mat.c1r3, // c1r1
                this.c0r1 * mat.c2r0 + this.c1r1 * mat.c2r1 + this.c2r1 * mat.c2r2 + this.c3r1 * mat.c2r3, // c2r1
                this.c0r1 * mat.c3r0 + this.c1r1 * mat.c3r1 + this.c2r1 * mat.c3r2 + this.c3r1 * mat.c3r3, // c3r1

                this.c0r2 * mat.c0r0 + this.c1r2 * mat.c0r1 + this.c2r2 * mat.c0r2 + this.c3r2 * mat.c0r3, // c0r2
                this.c0r2 * mat.c1r0 + this.c1r2 * mat.c1r1 + this.c2r2 * mat.c1r2 + this.c3r2 * mat.c1r3, // c1r2
                this.c0r2 * mat.c2r0 + this.c1r2 * mat.c2r1 + this.c2r2 * mat.c2r2 + this.c3r2 * mat.c2r3, // c2r2
                this.c0r2 * mat.c3r0 + this.c1r2 * mat.c3r1 + this.c2r2 * mat.c3r2 + this.c3r2 * mat.c3r3, // c3r2

                this.c0r3 * mat.c0r0 + this.c1r3 * mat.c0r1 + this.c2r3 * mat.c0r2 + this.c3r3 * mat.c0r3, // c0r3
                this.c0r3 * mat.c1r0 + this.c1r3 * mat.c1r1 + this.c2r3 * mat.c1r2 + this.c3r3 * mat.c1r3, // c1r3
                this.c0r3 * mat.c2r0 + this.c1r3 * mat.c2r1 + this.c2r3 * mat.c2r2 + this.c3r3 * mat.c2r3, // c2r3
                this.c0r3 * mat.c3r0 + this.c1r3 * mat.c3r1 + this.c2r3 * mat.c3r2 + this.c3r3 * mat.c3r3 // c3r3
        )
    }

    override fun toString(): String =
            "$c0r0, $c1r0, $c2r0, $c3r0,\n$c0r1, $c1r1, $c2r1, $c3r1,\n$c0r2, $c1r2, $c2r2, $c3r2,\n$c0r3, $c1r3, $c2r3, $c3r3"
}

operator fun Double.times(m: Matrix44) = m * this
