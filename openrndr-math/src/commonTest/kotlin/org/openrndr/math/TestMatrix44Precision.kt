package org.openrndr.math

import org.openrndr.math.transforms.*
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestMatrix44Precision {
    private fun maxDifference(a: Matrix44, b: Matrix44): Double {
        val x = a.toDoubleArray()
        val y = b.toDoubleArray()
        return x.indices.maxOf { abs(x[it] - y[it]) }
    }

    @Test
    fun inversesOfTransformsAndProjections() {
        val transforms = listOf(
            transform { translate(1e6, -2e5, 3e4); rotate(Vector3(1.0, 2.0, 3.0).normalized, 37.0); scale(2.0, 0.5, 1e-3) },
            perspective(45.0, 1.5, 0.1, 1e4) * lookAt(Vector3(300.0, 200.0, 800.0), Vector3.ZERO),
            ortho(0.0, 4000.0, 3000.0, 0.0, -1.0, 1.0),
        )
        for (m in transforms) {
            assertTrue(maxDifference(m * m.inversed, Matrix44.IDENTITY) < 1e-9, "$m")
        }
    }

    @Test
    fun integerMatricesHaveExactInversesAndDeterminants() {
        // a unimodular matrix: its determinant is 1 and its inverse has integer elements
        val m = Matrix44(
            2.0, 1.0, 0.0, 0.0,
            1.0, 1.0, 0.0, 0.0,
            0.0, 0.0, 1.0, 3.0,
            0.0, 0.0, 0.0, 1.0
        )
        assertEquals(1.0, m.determinant)
        assertEquals(
            Matrix44(
                1.0, -1.0, 0.0, 0.0,
                -1.0, 2.0, 0.0, 0.0,
                0.0, 0.0, 1.0, -3.0,
                0.0, 0.0, 0.0, 1.0
            ),
            m.inversed + Matrix44.ZERO // adding zero turns -0.0 into 0.0
        )
        assertEquals(24.0, Matrix44.scale(2.0, 3.0, 4.0).determinant)
    }

    @Test
    fun normalMatricesAreTheInverseTransposeOfTheUpper3x3() {
        val m = Matrix44.scale(2.0, 1.0, 0.5) * Matrix44.rotate(Vector3(1.0, 1.0, 0.0).normalized, 30.0) *
                Matrix44.translate(10.0, 20.0, 30.0)
        val expected = Matrix44(
            m.c0r0, m.c1r0, m.c2r0, 0.0,
            m.c0r1, m.c1r1, m.c2r1, 0.0,
            m.c0r2, m.c1r2, m.c2r2, 0.0,
            0.0, 0.0, 0.0, 1.0
        ).inversed.transposed.copy(c3r3 = 0.0)
        assertTrue(maxDifference(normalMatrix(m), expected) < 1e-14)
    }

    @Test
    fun quarterTurnsAreExact() {
        assertEquals(Matrix44(0.0, -1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0), Matrix44.rotateZ(90.0))
        assertEquals(0.0, Matrix44.rotateX(180.0).c2r1 + 0.0)
        assertEquals(-1.0, Matrix44.rotateY(-90.0).c2r0)
        assertEquals(0.0, Matrix44.rotate(Vector3.UNIT_Y, 270.0).c0r0)
        var m = Matrix44.IDENTITY
        repeat(4) { m *= Matrix44.rotateZ(90.0) }
        assertEquals(0.0, maxDifference(m, Matrix44.IDENTITY))
        assertEquals(0.0, maxDifference(Matrix44.rotateZ(720.0), Matrix44.IDENTITY))
    }

    @Test
    fun otherAnglesAreUnchanged() {
        val a = PI / 6
        assertEquals(
            Matrix44(cos(a), -sin(a), 0.0, 0.0, sin(a), cos(a), 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0),
            Matrix44.rotateZ(30.0)
        )
    }
}
