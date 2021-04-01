package org.openrndr.math

/**
 * A 5x5 matrix with double precision
 */
data class Matrix55(
        val c0r0: Double = 0.0, val c1r0: Double = 0.0, val c2r0: Double = 0.0, val c3r0: Double = 0.0, val c4r0: Double = 0.0,
        val c0r1: Double = 0.0, val c1r1: Double = 0.0, val c2r1: Double = 0.0, val c3r1: Double = 0.0, val c4r1: Double = 0.0,
        val c0r2: Double = 0.0, val c1r2: Double = 0.0, val c2r2: Double = 0.0, val c3r2: Double = 0.0, val c4r2: Double = 0.0,
        val c0r3: Double = 0.0, val c1r3: Double = 0.0, val c2r3: Double = 0.0, val c3r3: Double = 0.0, val c4r3: Double = 0.0,
        val c0r4: Double = 0.0, val c1r4: Double = 0.0, val c2r4: Double = 0.0, val c3r4: Double = 0.0, val c4r4: Double = 0.0) {

    companion object {
        /** A 5x5 identity matrix */
        val IDENTITY = Matrix55(c0r0 = 1.0, c1r1 = 1.0, c2r2 = 1.0, c3r3 = 1.0, c4r4 = 1.0)
        /** A 5x5 zero matrix */
        val ZERO = Matrix55()
    }

    /**
     * Convert the 5x5 matrix to an array of floats
     * @return array of floats in row major format
     */
    val floatArray
        get() = floatArrayOf(c0r0.toFloat(), c0r1.toFloat(), c0r2.toFloat(), c0r3.toFloat(), c0r4.toFloat(),
                c1r0.toFloat(), c1r1.toFloat(), c1r2.toFloat(), c1r3.toFloat(), c1r4.toFloat(),
                c2r0.toFloat(), c2r1.toFloat(), c2r2.toFloat(), c2r3.toFloat(), c2r4.toFloat(),
                c3r0.toFloat(), c3r1.toFloat(), c3r2.toFloat(), c3r3.toFloat(), c3r4.toFloat(),
                c4r0.toFloat(), c4r1.toFloat(), c4r2.toFloat(), c4r3.toFloat(), c4r4.toFloat()
        )

    /**
     * Matrix concatenation
     */
    operator fun times(mat: Matrix55) = Matrix55(
            this.c0r0 * mat.c0r0 + this.c1r0 * mat.c0r1 + this.c2r0 * mat.c0r2 + this.c3r0 * mat.c0r3 + this.c4r0 * mat.c0r4,
            this.c0r0 * mat.c1r0 + this.c1r0 * mat.c1r1 + this.c2r0 * mat.c1r2 + this.c3r0 * mat.c1r3 + this.c4r0 * mat.c1r4,
            this.c0r0 * mat.c2r0 + this.c1r0 * mat.c2r1 + this.c2r0 * mat.c2r2 + this.c3r0 * mat.c2r3 + this.c4r0 * mat.c2r4,
            this.c0r0 * mat.c3r0 + this.c1r0 * mat.c3r1 + this.c2r0 * mat.c3r2 + this.c3r0 * mat.c3r3 + this.c4r0 * mat.c3r4,
            this.c0r0 * mat.c4r0 + this.c1r0 * mat.c4r1 + this.c2r0 * mat.c4r2 + this.c3r0 * mat.c3r3 + this.c4r0 * mat.c4r4,

            this.c0r1 * mat.c0r0 + this.c1r1 * mat.c0r1 + this.c2r1 * mat.c0r2 + this.c3r1 * mat.c0r3 + this.c4r1 * mat.c0r4,
            this.c0r1 * mat.c1r0 + this.c1r1 * mat.c1r1 + this.c2r1 * mat.c1r2 + this.c3r1 * mat.c1r3 + this.c4r1 * mat.c1r4,
            this.c0r1 * mat.c2r0 + this.c1r1 * mat.c2r1 + this.c2r1 * mat.c2r2 + this.c3r1 * mat.c2r3 + this.c4r1 * mat.c2r4,
            this.c0r1 * mat.c3r0 + this.c1r1 * mat.c3r1 + this.c2r1 * mat.c3r2 + this.c3r1 * mat.c3r3 + this.c4r1 * mat.c3r4,
            this.c0r1 * mat.c4r0 + this.c1r1 * mat.c4r1 + this.c2r1 * mat.c4r2 + this.c3r1 * mat.c4r3 + this.c4r1 * mat.c4r4,

            this.c0r2 * mat.c0r0 + this.c1r2 * mat.c0r1 + this.c2r2 * mat.c0r2 + this.c3r2 * mat.c0r3 + this.c4r2 * mat.c0r4,
            this.c0r2 * mat.c1r0 + this.c1r2 * mat.c1r1 + this.c2r2 * mat.c1r2 + this.c3r2 * mat.c1r3 + this.c4r2 * mat.c1r4,
            this.c0r2 * mat.c2r0 + this.c1r2 * mat.c2r1 + this.c2r2 * mat.c2r2 + this.c3r2 * mat.c2r3 + this.c4r2 * mat.c2r4,
            this.c0r2 * mat.c3r0 + this.c1r2 * mat.c3r1 + this.c2r2 * mat.c3r2 + this.c3r2 * mat.c3r3 + this.c4r2 * mat.c3r4,
            this.c0r2 * mat.c4r0 + this.c1r2 * mat.c4r1 + this.c2r2 * mat.c4r2 + this.c3r2 * mat.c4r3 + this.c4r2 * mat.c4r4,

            this.c0r3 * mat.c0r0 + this.c1r3 * mat.c0r1 + this.c2r3 * mat.c0r2 + this.c3r3 * mat.c0r3 + this.c4r3 * mat.c0r4,
            this.c0r3 * mat.c1r0 + this.c1r3 * mat.c1r1 + this.c2r3 * mat.c1r2 + this.c3r3 * mat.c1r3 + this.c4r3 * mat.c1r4,
            this.c0r3 * mat.c2r0 + this.c1r3 * mat.c2r1 + this.c2r3 * mat.c2r2 + this.c3r3 * mat.c2r3 + this.c4r3 * mat.c2r4,
            this.c0r3 * mat.c3r0 + this.c1r3 * mat.c3r1 + this.c2r3 * mat.c3r2 + this.c3r3 * mat.c3r3 + this.c4r3 * mat.c3r4,
            this.c0r3 * mat.c4r0 + this.c1r3 * mat.c4r1 + this.c2r3 * mat.c4r2 + this.c3r3 * mat.c4r3 + this.c4r3 * mat.c4r4,

            this.c0r4 * mat.c0r0 + this.c1r4 * mat.c0r1 + this.c2r4 * mat.c0r2 + this.c3r4 * mat.c0r3 + this.c4r4 * mat.c0r4,
            this.c0r4 * mat.c1r0 + this.c1r4 * mat.c1r1 + this.c2r4 * mat.c1r2 + this.c3r4 * mat.c1r3 + this.c4r4 * mat.c1r4,
            this.c0r4 * mat.c2r0 + this.c1r4 * mat.c2r1 + this.c2r4 * mat.c2r2 + this.c3r4 * mat.c2r3 + this.c4r4 * mat.c2r4,
            this.c0r4 * mat.c3r0 + this.c1r4 * mat.c3r1 + this.c2r4 * mat.c3r2 + this.c3r4 * mat.c3r3 + this.c4r4 * mat.c3r4,
            this.c0r4 * mat.c4r0 + this.c1r4 * mat.c4r1 + this.c2r4 * mat.c4r2 + this.c3r4 * mat.c4r3 + this.c4r4 * mat.c4r4
    )

    /**
     * Matrix element-wise addition
     */
    operator fun plus(mat: Matrix55) = Matrix55(
            c0r0 + mat.c0r0, c1r0 + mat.c1r0, c2r0 + mat.c2r0, c3r0 + mat.c3r0, c4r0 + mat.c4r0,
            c0r1 + mat.c0r1, c1r1 + mat.c1r1, c2r1 + mat.c2r1, c3r1 + mat.c3r1, c4r1 + mat.c4r1,
            c0r2 + mat.c0r2, c1r2 + mat.c1r2, c2r2 + mat.c2r2, c3r2 + mat.c3r2, c4r2 + mat.c4r2,
            c0r3 + mat.c0r3, c1r3 + mat.c1r3, c2r3 + mat.c2r3, c3r3 + mat.c3r3, c4r3 + mat.c4r3,
            c0r4 + mat.c0r4, c1r4 + mat.c1r4, c2r4 + mat.c2r4, c3r4 + mat.c3r4, c4r4 + mat.c4r4)

    /**
     * Matrix element-wise multiplication
     */
    operator fun times(s: Double) = Matrix55(
            c0r0 * s, c1r0 * s, c2r0 * s, c3r0 * s, c4r0 * s,
            c0r1 * s, c1r1 * s, c2r1 * s, c3r1 * s, c4r1 * s,
            c0r2 * s, c1r2 * s, c2r2 * s, c3r2 * s, c4r2 * s,
            c0r3 * s, c1r3 * s, c2r3 * s, c3r3 * s, c4r3 * s,
            c0r4 * s, c1r4 * s, c2r4 * s, c3r4 * s, c4r4 * s)

}

