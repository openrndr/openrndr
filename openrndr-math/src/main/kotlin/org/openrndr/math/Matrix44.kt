package org.openrndr.math

data class Matrix44(
        val c0r0: Double = 0.0, val c1r0: Double = 0.0, val c2r0: Double = 0.0, val c3r0: Double = 0.0,
        val c0r1: Double = 0.0, val c1r1: Double = 0.0, val c2r1: Double = 0.0, val c3r1: Double = 0.0,
        val c0r2: Double = 0.0, val c1r2: Double = 0.0, val c2r2: Double = 0.0, val c3r2: Double = 0.0,
        val c0r3: Double = 0.0, val c1r3: Double = 0.0, val c2r3: Double = 0.0, val c3r3: Double = 0.0) {

    companion object {

        val IDENTITY = Matrix44(c0r0 = 1.0, c1r1 = 1.0, c2r2 = 1.0, c3r3 = 1.0)
        val ZERO = Matrix44()

        fun fromColumnVectors(c0: Vector4, c1: Vector4, c2: Vector4, c3: Vector4): Matrix44 {
            return Matrix44(c0.x, c1.x, c2.x, c3.x,
                            c0.y, c1.y, c2.y, c3.y,
                            c0.z, c1.z, c2.z, c3.z,
                            c0.w, c1.w, c2.w, c3.w)
        }
    }

    /**
     * Returns a column vector
     */
    operator fun get(index: Int): Vector4 =
            when (index) {
                0    -> Vector4(c0r0, c0r1, c0r2, c0r3)
                1    -> Vector4(c1r0, c1r1, c1r2, c1r3)
                2    -> Vector4(c2r0, c2r1, c2r2, c2r3)
                3    -> Vector4(c3r0, c3r1, c3r2, c1r3)
                else -> throw RuntimeException("not implemented")
            }

    val inversed: Matrix44 get()  {
        val n00 = c1r2 * c2r3 * c3r1 - c1r3 * c2r2 * c3r1 + c1r3 * c2r1 * c3r2 - c1r1 * c2r3 * c3r2 - c1r2 * c2r1 * c3r3 + c1r1 * c2r2 * c3r3
        val n01 = c0r3 * c2r2 * c3r1 - c0r2 * c2r3 * c3r1 - c0r3 * c2r1 * c3r2 + c0r1 * c2r3 * c3r2 + c0r2 * c2r1 * c3r3 - c0r1 * c2r2 * c3r3
        val n02 = c0r2 * c1r3 * c3r1 - c0r3 * c1r2 * c3r1 + c0r3 * c1r1 * c3r2 - c0r1 * c1r3 * c3r2 - c0r2 * c1r1 * c3r3 + c0r1 * c1r2 * c3r3
        val n03 = c0r3 * c1r2 * c2r1 - c0r2 * c1r3 * c2r1 - c0r3 * c1r1 * c2r2 + c0r1 * c1r3 * c2r2 + c0r2 * c1r1 * c2r3 - c0r1 * c1r2 * c2r3
        val n10 = c1r3 * c2r2 * c3r0 - c1r2 * c2r3 * c3r0 - c1r3 * c2r0 * c3r2 + c1r0* c2r3 * c3r2 + c1r2 * c2r0 * c3r3 - c1r0* c2r2 * c3r3
        val n11 = c0r2 * c2r3 * c3r0 - c0r3 * c2r2 * c3r0 + c0r3 * c2r0 * c3r2 - c0r0 * c2r3 * c3r2 - c0r2 * c2r0 * c3r3 + c0r0 * c2r2 * c3r3
        val n12 = c0r3 * c1r2 * c3r0 - c0r2 * c1r3 * c3r0 - c0r3 * c1r0* c3r2 + c0r0 * c1r3 * c3r2 + c0r2 * c1r0* c3r3 - c0r0 * c1r2 * c3r3
        val n13 = c0r2 * c1r3 * c2r0 - c0r3 * c1r2 * c2r0 + c0r3 * c1r0* c2r2 - c0r0 * c1r3 * c2r2 - c0r2 * c1r0* c2r3 + c0r0 * c1r2 * c2r3
        val n20 = c1r1 * c2r3 * c3r0 - c1r3 * c2r1 * c3r0 + c1r3 * c2r0 * c3r1 - c1r0* c2r3 * c3r1 - c1r1 * c2r0 * c3r3 + c1r0* c2r1 * c3r3
        val n21 = c0r3 * c2r1 * c3r0 - c0r1 * c2r3 * c3r0 - c0r3 * c2r0 * c3r1 + c0r0 * c2r3 * c3r1 + c0r1 * c2r0 * c3r3 - c0r0 * c2r1 * c3r3
        val n22 = c0r1 * c1r3 * c3r0 - c0r3 * c1r1 * c3r0 + c0r3 * c1r0* c3r1 - c0r0 * c1r3 * c3r1 - c0r1 * c1r0* c3r3 + c0r0 * c1r1 * c3r3
        val n23 = c0r3 * c1r1 * c2r0 - c0r1 * c1r3 * c2r0 - c0r3 * c1r0* c2r1 + c0r0 * c1r3 * c2r1 + c0r1 * c1r0* c2r3 - c0r0 * c1r1 * c2r3
        val n30 = c1r2 * c2r1 * c3r0 - c1r1 * c2r2 * c3r0 - c1r2 * c2r0 * c3r1 + c1r0* c2r2 * c3r1 + c1r1 * c2r0 * c3r2 - c1r0* c2r1 * c3r2
        val n31 = c0r1 * c2r2 * c3r0 - c0r2 * c2r1 * c3r0 + c0r2 * c2r0 * c3r1 - c0r0 * c2r2 * c3r1 - c0r1 * c2r0 * c3r2 + c0r0 * c2r1 * c3r2
        val n32 = c0r2 * c1r1 * c3r0 - c0r1 * c1r2 * c3r0 - c0r2 * c1r0* c3r1 + c0r0 * c1r2 * c3r1 + c0r1 * c1r0* c3r2 - c0r0 * c1r1 * c3r2
        val n33 = c0r1 * c1r2 * c2r0 - c0r2 * c1r1 * c2r0 + c0r2 * c1r0* c2r1 - c0r0 * c1r2 * c2r1 - c0r1 * c1r0* c2r2 + c0r0 * c1r1 * c2r2

        val d = determinant
        return Matrix44(n00 / d, n10 / d, n20 / d, n30 / d,
                        n01 / d, n11 / d, n21 / d, n31 / d,
                        n02 / d, n12 / d, n22 / d, n32 / d,
                        n03 / d, n13 / d, n23 / d, n33 / d)
    }

    val trace: Double get() = c0r0 + c1r1 + c2r2 + c3r3

    val determinant: Double get() {
        return c0r3 * c1r2 * c2r1 * c3r0 - c0r2 * c1r3 * c2r1 * c3r0 - c0r3 * c1r1 * c2r2 * c3r0 + c0r1 * c1r3 * c2r2 * c3r0 +
               c0r2 * c1r1 * c2r3 * c3r0 - c0r1 * c1r2 * c2r3 * c3r0 - c0r3 * c1r2 * c2r0 * c3r1 + c0r2 * c1r3 * c2r0 * c3r1 +
               c0r3 * c1r0 * c2r2 * c3r1 - c0r0 * c1r3 * c2r2 * c3r1 - c0r2 * c1r0 * c2r3 * c3r1 + c0r0 * c1r2 * c2r3 * c3r1 +
               c0r3 * c1r1 * c2r0 * c3r2 - c0r1 * c1r3 * c2r0 * c3r2 - c0r3 * c1r0 * c2r1 * c3r2 + c0r0 * c1r3 * c2r1 * c3r2 +
               c0r1 * c1r0 * c2r3 * c3r2 - c0r0 * c1r1 * c2r3 * c3r2 - c0r2 * c1r1 * c2r0 * c3r3 + c0r1 * c1r2 * c2r0 * c3r3 +
               c0r2 * c1r0 * c2r1 * c3r3 - c0r0 * c1r2 * c2r1 * c3r3 - c0r1 * c1r0 * c2r2 * c3r3 + c0r0 * c1r1 * c2r2 * c3r3
    }

    operator fun plus(o: Matrix44): Matrix44 {
        return Matrix44(
                c0r0 + o.c0r0, c1r0 + o.c1r0, c2r0 + o.c2r0, c3r0 + o.c3r0,
                c0r1 + o.c0r1, c1r1 + o.c1r1, c2r1 + o.c2r1, c3r1 + o.c3r1,
                c0r2 + o.c0r2, c1r2 + o.c1r2, c2r2 + o.c2r2, c3r2 + o.c3r2,
                c0r3 + o.c0r3, c1r3 + o.c1r3, c2r3 + o.c2r3, c3r3 + o.c3r3)
    }

    operator fun minus(o: Matrix44): Matrix44 {
        return Matrix44(
                c0r0 - o.c0r0, c1r0 - o.c1r0, c2r0 - o.c2r0, c3r0 - o.c3r0,
                c0r1 - o.c0r1, c1r1 - o.c1r1, c2r1 - o.c2r1, c3r1 - o.c3r1,
                c0r2 - o.c0r2, c1r2 - o.c1r2, c2r2 - o.c2r2, c3r2 - o.c3r2,
                c0r3 - o.c0r3, c1r3 - o.c1r3, c2r3 - o.c2r3, c3r3 - o.c3r3)
    }

    val transposed: Matrix44 get() {
        return Matrix44(
                c0r0, c0r1, c0r2, c0r3,
                c1r0, c1r1, c1r2, c1r3,
                c2r0, c2r1, c2r2, c2r3,
                c3r0, c3r1, c3r2, c3r3
                       )
    }

    operator fun times(v: Vector4): Vector4 {
        return Vector4(v.x * c0r0 + v.y * c1r0 + v.z * c2r0 + v.w * c3r0,
                       v.x * c0r1 + v.y * c1r1 + v.z * c2r1 + v.w * c3r1,
                       v.x * c0r2 + v.y * c1r2 + v.z * c2r2 + v.w * c3r2,
                       v.x * c0r3 + v.y * c1r3 + v.z * c2r3 + v.w * c3r3)
    }

    operator fun times(v: Vector3): Vector3 = Vector3(v.x * c0r0 + v.y * c1r0 + v.z * c2r0,
            v.x * c0r1 + v.y * c1r1 + v.z * c2r1,
            v.x * c0r2 + v.y * c1r2 + v.z * c2r2)

    operator fun times(s: Double): Matrix44 {
        return Matrix44(s * c0r0, s * c1r0, s * c2r0, s * c3r0,
                        s * c0r1, s * c1r1, s * c2r1, s * c3r1,
                        s * c0r2, s * c1r2, s * c2r2, s * c3r2,
                        s * c0r3, s * c1r3, s * c2r3, s * c3r3)
    }


    operator fun times(mat: Matrix44): Matrix44 {

        return Matrix44(
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

    override fun toString(): String {
        return "${c0r0}, ${c1r0}, ${c2r0}, ${c3r0}\n${c0r1}, ${c1r1}, ${c2r1}, ${c3r1}\n${c0r2}, ${c1r2}, ${c2r2}, ${c3r2}\n${c0r3}, ${c1r3}, ${c2r3}, ${c3r3}"
    }


}
