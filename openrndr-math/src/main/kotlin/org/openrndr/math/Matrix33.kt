package org.openrndr.math

/**
 * A 3x3 matrix with double precision
 */
data class Matrix33(
        val c0r0: Double = 0.0, val c1r0: Double = 0.0, val c2r0: Double = 0.0,
        val c0r1: Double = 0.0, val c1r1: Double = 0.0, val c2r1: Double = 0.0,
        val c0r2: Double = 0.0, val c1r2: Double = 0.0, val c2r2: Double = 0.0) : LinearType<Matrix33> {

    companion object {
        val IDENTITY = Matrix33(c0r0 = 1.0, c1r1 = 1.0, c2r2 = 1.0)
        val ZERO = Matrix33()

        fun fromColumnVectors(c0: Vector3, c1: Vector3, c2: Vector3): Matrix33 =
                Matrix33(c0.x, c1.x, c2.x,
                        c0.y, c1.y, c2.y,
                        c0.z, c1.z, c2.z)

    }

    /**
     * Returns a column vector
     */
    operator fun get(index: Int) =
            when (index) {
                0 -> Vector3(c0r0, c0r1, c0r2)
                1 -> Vector3(c1r0, c1r1, c1r2)
                2 -> Vector3(c2r0, c2r1, c2r2)

                else -> throw RuntimeException("not implemented")
            }

    val trace get() = c0r0 + c1r1 + c2r2

    val determinant: Double
        get() {
            val x = c0r0 * (c1r1 * c2r2 - c1r2 * c2r1)
            val y = -c1r0 * (c0r1 * c2r2 - c0r2 * c2r1)
            val z = c2r0 * (c0r1 * c1r2 - c1r1 * c0r2)
            return x + y + z
        }

    val inversed: Matrix33
        get() {
            val invDet = 1.0 / determinant
            return Matrix33(
                    invDet * (c1r1 * c2r2 - c2r1 * c1r2),
                    invDet * (c2r0 * c1r2 - c1r0 * c2r2),
                    invDet * (c1r0 * c2r1 - c2r0 * c1r1),
                    invDet * (c2r1 * c0r2 - c0r1 * c2r2),
                    invDet * (c0r0 * c2r2 - c2r0 * c0r2),
                    invDet * (c2r0 * c0r1 - c0r0 * c2r1),
                    invDet * (c0r1 * c1r2 - c1r1 * c0r2),
                    invDet * (c1r0 * c0r2 - c0r0 * c1r2),
                    invDet * (c0r0 * c1r1 - c1r0 * c0r1))
        }

    override operator fun plus(o: Matrix33) = Matrix33(
            c0r0 + o.c0r0, c1r0 + o.c1r0, c2r0 + o.c2r0,
            c0r1 + o.c0r1, c1r1 + o.c1r1, c2r1 + o.c2r1,
            c0r2 + o.c0r2, c1r2 + o.c1r2, c2r2 + o.c2r2)

    override operator fun minus(o: Matrix33) = Matrix33(
            c0r0 - o.c0r0, c1r0 - o.c1r0, c2r0 - o.c2r0,
            c0r1 - o.c0r1, c1r1 - o.c1r1, c2r1 - o.c2r1,
            c0r2 - o.c0r2, c1r2 - o.c1r2, c2r2 - o.c2r2)


    val transposed
        get() = Matrix33(
                c0r0, c0r1, c0r2,
                c1r0, c1r1, c1r2,
                c2r0, c2r1, c2r2)

    val matrix44
        get() = Matrix44(c0r0, c1r0, c2r0, 0.0,
                c0r1, c1r1, c2r1, 0.0,
                c0r2, c1r2, c2r2, 0.0,
                0.0, 0.0, 0.0, 1.0)

    operator fun times(v: Vector3) = Vector3(
            v.x * c0r0 + v.y * c1r0 + v.z * c2r0,
            v.x * c0r1 + v.y * c1r1 + v.z * c2r1,
            v.x * c0r2 + v.y * c1r2 + v.z * c2r2)

    override operator fun times(s: Double) = Matrix33(s * c0r0, s * c1r0, s * c2r0,
            s * c0r1, s * c1r1, s * c2r1,
            s * c0r2, s * c1r2, s * c2r2)

    override operator fun div(s: Double) = Matrix33(s / c0r0, s / c1r0, s / c2r0,
            s / c0r1, s / c1r1, s / c2r1,
            s / c0r2, s / c1r2, s / c2r2)

    operator fun times(mat: Matrix33) = Matrix33(
            this.c0r0 * mat.c0r0 + this.c1r0 * mat.c0r1 + this.c2r0 * mat.c0r2,
            this.c0r0 * mat.c1r0 + this.c1r0 * mat.c1r1 + this.c2r0 * mat.c1r2,
            this.c0r0 * mat.c2r0 + this.c1r0 * mat.c2r1 + this.c2r0 * mat.c2r2,

            this.c0r1 * mat.c0r0 + this.c1r1 * mat.c0r1 + this.c2r1 * mat.c0r2,
            this.c0r1 * mat.c1r0 + this.c1r1 * mat.c1r1 + this.c2r1 * mat.c1r2,
            this.c0r1 * mat.c2r0 + this.c1r1 * mat.c2r1 + this.c2r1 * mat.c2r2,

            this.c0r2 * mat.c0r0 + this.c1r2 * mat.c0r1 + this.c2r2 * mat.c0r2,
            this.c0r2 * mat.c1r0 + this.c1r2 * mat.c1r1 + this.c2r2 * mat.c1r2,
            this.c0r2 * mat.c2r0 + this.c1r2 * mat.c2r1 + this.c2r2 * mat.c2r2)

    override fun toString(): String =
            "${c0r0}, ${c1r0}, ${c2r0}\n${c0r1}, ${c1r1}, ${c2r1}\n${c0r2}, ${c1r2}, ${c2r2}\n"
}

operator fun Double.times(m: Matrix33) = m * this
