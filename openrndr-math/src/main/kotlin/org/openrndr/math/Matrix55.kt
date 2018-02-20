package org.openrndr.math

data class Matrix55(
        val c0r0: Double = 0.0, val c1r0: Double = 0.0, val c2r0: Double = 0.0, val c3r0: Double = 0.0, val c4r0: Double = 0.0,
        val c0r1: Double = 0.0, val c1r1: Double = 0.0, val c2r1: Double = 0.0, val c3r1: Double = 0.0, val c4r1: Double = 0.0,
        val c0r2: Double = 0.0, val c1r2: Double = 0.0, val c2r2: Double = 0.0, val c3r2: Double = 0.0, val c4r2: Double = 0.0,
        val c0r3: Double = 0.0, val c1r3: Double = 0.0, val c2r3: Double = 0.0, val c3r3: Double = 0.0, val c4r3: Double = 0.0,
        val c0r4: Double = 0.0, val c1r4: Double = 0.0, val c2r4: Double = 0.0, val c3r4: Double = 0.0, val c4r4: Double = 0.0) {

    companion object {
        val IDENTITY = Matrix55(c0r0 = 1.0, c1r1 = 1.0, c2r2 = 1.0, c3r3 = 1.0, c4r4 = 1.0)
        val ZERO = Matrix55()
    }

    val floatArray: FloatArray get() {
        return floatArrayOf(c0r0.toFloat(), c0r1.toFloat(), c0r2.toFloat(), c0r3.toFloat(), c0r4.toFloat(),
                c1r0.toFloat(), c1r1.toFloat(), c1r2.toFloat(), c1r3.toFloat(), c1r4.toFloat(),
                c2r0.toFloat(), c2r1.toFloat(), c2r2.toFloat(), c2r3.toFloat(), c2r4.toFloat(),
                c3r0.toFloat(), c3r1.toFloat(), c3r2.toFloat(), c3r3.toFloat(), c3r4.toFloat(),
                c4r0.toFloat(), c4r1.toFloat(), c4r2.toFloat(), c4r3.toFloat(), c4r4.toFloat()
                )
    }

}

