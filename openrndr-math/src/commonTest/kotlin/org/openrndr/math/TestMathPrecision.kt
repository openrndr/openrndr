package org.openrndr.math

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestMathPrecision {
    @Test
    fun solveQuadraticWithATinyPositiveLeadingCoefficient() {
        // the coefficients are normalized by their magnitudes, not their signs
        val roots = solveQuadratic(3.8390986270008325E-13, 238.18752639488488, -2.928167060267697)
        assertTrue(roots.any { abs(it - 2.928167060267697 / 238.18752639488488) < 1e-12 }, "${roots.toList()}")
    }

    @Test
    fun rootsOfALinearDerivative() {
        // the x derivative of the quadratic (0,0)-(100,100)-(0,200) raised to a cubic
        assertEquals(listOf(0.5), roots(listOf(200.0, 0.0, -200.0)))
    }

    @Test
    fun quaternionDot() {
        val q1 = Quaternion(0.1, 0.2, 0.3, 0.9)
        val q2 = Quaternion(-0.4, 0.5, 0.2, 0.7)
        assertEquals(0.1 * -0.4 + 0.2 * 0.5 + 0.3 * 0.2 + 0.9 * 0.7, dot(q1, q2), 1e-15)
    }

    @Test
    fun quaternionsFromMatricesOfHalfTurns() {
        for (axis in listOf(Vector3(0.2, 0.95, 0.1), Vector3(0.95, 0.2, 0.1), Vector3(0.1, 0.2, 0.95))) {
            val a = axis.normalized
            val q = Quaternion(a.x, a.y, a.z, 0.0)
            val back = Quaternion.fromMatrix(q.matrix)
            val sign = if (back.x * q.x + back.y * q.y + back.z * q.z + back.w * q.w < 0) -1.0 else 1.0
            assertEquals(q.x, back.x * sign, 1e-12)
            assertEquals(q.y, back.y * sign, 1e-12)
            assertEquals(q.z, back.z * sign, 1e-12)
            assertEquals(q.w, back.w * sign, 1e-12)
        }
    }

    @Test
    fun slerpKeepsUnitLengthAndAngle() {
        val angle = 40.0.asRadians
        val b = Quaternion(0.0, sin(angle / 2), 0.0, cos(angle / 2))
        val s = slerp(Quaternion.IDENTITY, b, 0.25)
        assertEquals(1.0, s.length, 1e-12)
        assertEquals(angle * 0.25 / 2, kotlin.math.acos(s.w), 1e-12)
    }

    @Test
    fun anglesAndAreasOfParallelVectors() {
        val a = Vector3(1.0, 2.0, 3.0)
        for (s in listOf(0.1, 0.3, 0.7, 1.3, 3.7, 9.1)) {
            assertEquals(0.0, (a * s).atan2(a), 1e-15)
            assertEquals(0.0, (a * s).areaBetween(a), 1e-12)
            assertEquals(0.0, (a * s).xyz0.areaBetween(a.xyz0), 1e-12)
        }
        assertEquals(1e-9, Vector2.UNIT_X.atan2(Vector2(cos(1e-9), sin(1e-9))), 1e-20)
        assertEquals(PI / 2, Vector2.UNIT_X.atan2(Vector2.UNIT_Y), 1e-15)
    }

    @Test
    fun smallFunctions() {
        assertEquals(6.0, Vector3(5.0, 5.0, 5.0).clamp(Vector3(0.0, 0.0, 6.0), Vector3(10.0, 10.0, 10.0)).z)
        assertEquals(6, IntVector3(5, 5, 5).clamp(IntVector3(0, 0, 6), IntVector3(10, 10, 10)).z)
        assertEquals(smoothstep(10.0, 20.0, 11.0), smoothstepIn(10.0, 20.0, 11.0))
        assertEquals(Vector2(0.0, 1.0), Vector2(1.0, 0.0).rotate(90.0))
        assertEquals(Vector2(-1.0, 0.0), Vector2(0.0, 1.0).rotate(90.0))
        val p = Vector2(1.0, 0.0).rotate(30.0)
        assertEquals(cos(PI / 6), p.x, 1e-15)
        assertEquals(sin(PI / 6), p.y, 1e-15)
        assertEquals(5.7295779513082324E-8, Spherical.fromVector(Vector3(1e-9, 1.0, 0.0)).phi, 1e-20)
        val v = Vector3(0.3, -0.5, 0.8)
        val c = Spherical.fromVector(v).cartesian
        assertEquals(v.x, c.x, 1e-15)
        assertEquals(v.y, c.y, 1e-15)
        assertEquals(v.z, c.z, 1e-15)
    }

    @Test
    fun matrix55ProductsUseTheOffsetColumn() {
        val l = Matrix55(c0r0 = 1.0, c3r0 = 1.0, c1r1 = 1.0, c2r2 = 1.0, c3r3 = 1.0, c4r4 = 1.0)
        val m = Matrix55(c0r0 = 1.0, c1r1 = 1.0, c2r2 = 1.0, c3r3 = 1.0, c4r4 = 1.0, c4r0 = 1.0, c4r3 = 2.0)
        assertEquals(3.0, (l * m).c4r0)
    }

    @Test
    fun float16RoundsTiesToEven() {
        // 2^-25 lies halfway between 0 and the smallest subnormal half
        assertEquals(0x0000, Float.fromBits(102 shl 23).toFloat16().v.toInt())
        // 2^-14 (1 + 2^-11) lies halfway between the halves 0x0400 and 0x0401
        assertEquals(0x0400, Float.fromBits((113 shl 23) or (1 shl 12)).toFloat16().v.toInt())
        // 2^-14 (1 + 3 2^-11) lies halfway between 0x0401 and 0x0402
        assertEquals(0x0402, Float.fromBits((113 shl 23) or (3 shl 12)).toFloat16().v.toInt())
        assertEquals(0x3c00, 1.0f.toFloat16().v.toInt())
    }
}
