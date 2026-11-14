package org.openrndr.math

import org.openrndr.math.transforms.rotate
import org.openrndr.math.transforms.scale
import org.openrndr.math.transforms.translate
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * [Matrix44.timesTranslate] and [Matrix44.timesScale] give exactly the results of multiplying by the translation and
 * scale matrices
 */
class TestMatrix44TimesTransforms {
    private fun Matrix44.entries() = listOf(
        c0r0, c1r0, c2r0, c3r0, c0r1, c1r1, c2r1, c3r1, c0r2, c1r2, c2r2, c3r2, c0r3, c1r3, c2r3, c3r3
    )

    /** asserts that [actual] has exactly the entries of [expected], also the signs of zeros */
    private fun assertSameValues(expected: Matrix44, actual: Matrix44) {
        for ((e, a) in expected.entries().zip(actual.entries())) {
            assertTrue(e.toRawBits() == a.toRawBits(), "expected\n$expected\nbut was\n$actual")
        }
    }

    private fun randomMatrix(random: Random) = Matrix44(
        random.nextDouble(-10.0, 10.0), random.nextDouble(-10.0, 10.0), random.nextDouble(-10.0, 10.0), random.nextDouble(-10.0, 10.0),
        random.nextDouble(-10.0, 10.0), random.nextDouble(-10.0, 10.0), random.nextDouble(-10.0, 10.0), random.nextDouble(-10.0, 10.0),
        random.nextDouble(-10.0, 10.0), random.nextDouble(-10.0, 10.0), random.nextDouble(-10.0, 10.0), random.nextDouble(-10.0, 10.0),
        random.nextDouble(-10.0, 10.0), random.nextDouble(-10.0, 10.0), random.nextDouble(-10.0, 10.0), random.nextDouble(-10.0, 10.0)
    )

    @Test
    fun timesTranslateIsTheProductWithTheTranslationMatrix() {
        val random = Random(1)
        repeat(1000) {
            val m = randomMatrix(random)
            val (x, y, z) = Triple(random.nextDouble(-100.0, 100.0), random.nextDouble(-100.0, 100.0), random.nextDouble(-100.0, 100.0))
            assertSameValues(m * Matrix44.translate(x, y, z), m.timesTranslate(x, y, z))
        }
        // a transform like the ones of a drawer
        val m = Matrix44.rotate(Vector3.UNIT_Z, 30.0) * Matrix44.scale(2.0, 2.0, 1.0)
        assertSameValues(m * Matrix44.translate(10.0, 20.0, 0.0), m.timesTranslate(10.0, 20.0, 0.0))
        // matrices with negative zero entries
        val n = Matrix44.scale(-1.0, -1.0, -1.0) * Matrix44.rotate(Vector3.UNIT_Z, 90.0)
        assertSameValues(n * Matrix44.translate(-3.0, 4.0, 0.0), n.timesTranslate(-3.0, 4.0, 0.0))
    }

    @Test
    fun timesScaleIsTheProductWithTheScaleMatrix() {
        val random = Random(2)
        repeat(1000) {
            val m = randomMatrix(random)
            val (x, y, z) = Triple(random.nextDouble(-4.0, 4.0), random.nextDouble(-4.0, 4.0), random.nextDouble(-4.0, 4.0))
            assertSameValues(m * Matrix44.scale(x, y, z), m.timesScale(x, y, z))
        }
        // negative scales of zero entries
        val m = Matrix44.rotate(Vector3.UNIT_Z, 30.0)
        assertSameValues(m * Matrix44.scale(1.5, -2.0, 1.0), m.timesScale(1.5, -2.0, 1.0))
    }

    @Test
    fun theIdentityGivesTheTransformMatrix() {
        assertEquals(Matrix44.translate(1.0, 2.0, 3.0), Matrix44.IDENTITY.timesTranslate(1.0, 2.0, 3.0))
        assertEquals(Matrix44.scale(1.0, 2.0, 3.0), Matrix44.IDENTITY.timesScale(1.0, 2.0, 3.0))
    }
}
