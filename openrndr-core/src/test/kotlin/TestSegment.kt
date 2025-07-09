import org.openrndr.math.Vector2
import org.openrndr.shape.Segment2D
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Deprecated. Remove after removing Kluent.
infix fun Vector2.`should be near`(other: Vector2) {
    assertEquals(other.x, x, 0.00001)
    assertEquals(other.y, y, 0.00001)
}

// Deprecated. Remove after removing Kluent.
infix fun Vector2.`should be somewhat near`(other: Vector2) {
    assertEquals(other.x, x, 0.01)
    assertEquals(other.y, y, 0.01)
}

fun assertVeryNear(a: Vector2, b: Vector2, message: String? = null) {
    assertEquals(a.x, b.x, 0.00001, message)
    assertEquals(a.y, b.y, 0.00001, message)
}

fun assertSomewhatNear(a: Vector2, b: Vector2, message: String? = null) {
    assertEquals(a.x, b.x, 0.01, message)
    assertEquals(a.y, b.y, 0.01, message)
}

class TestLinearSegment {
    @Test
    fun `the normal of a horizontal segment points down`() {
        val segment = Segment2D(
            Vector2(0.0, 100.0),
            Vector2(100.0, 100.0)
        )
        assertVeryNear(segment.normal(0.0), Vector2(0.0, -1.0))
    }

    val segment = Segment2D(
        Vector2(0.0, 0.0),
        Vector2(100.0, 100.0)
    )

    @Test
    fun `a linear segment has evaluable and correct bounds`() {
        val bounds = segment.bounds
        assertEquals(segment.start.x, bounds.x)
        assertEquals(segment.start.y, bounds.y)
        assertEquals(100.0, bounds.width)
        assertEquals(100.0, bounds.height)
    }

    @Test
    fun `a linear segment has an evaluable normal at t = 0_0`() {
        val normal = segment.normal(0.0)
        assertEquals(1.0, normal.length, 10E-6)
    }

    @Test
    fun `a linear segment can be split in half`() {
        val sides = segment.split(0.5)
        assertEquals(2, sides.size)
    }

    //    @Test
    //    fun `a linear segment can be split at 0 and result in 1 part`() {
    //            val sides = segment.split(0.0)
    //            assertEquals(1, sides.size)
    //    }

    //    @Test
    //    fun `a linear segment can be split at 1 and result in 1 part`() {
    //            val sides = segment.split(1.0)
    //            assertEquals(1, sides.size)
    //    }

    @Test
    fun `a linear segment can be subbed from 0_0 to 1_0`() {
        val sub = segment.sub(0.0, 1.0)
        assertEquals(segment, sub)
    }

    @Test
    fun `a linear segment can be subbed from 0_0 to 0_5`() {
        val sub = segment.sub(0.0, 0.5)
        assertEquals(0.0, (sub.start - segment.start).squaredLength)
    }

    @Test
    fun `a linear segment can be subbed from 0_5 to 1_0`() {
        val sub = segment.sub(0.5, 1.0)
        assertEquals(0.0, (sub.end - segment.end).squaredLength)
    }

    @Test
    fun `a linear segment has a length`() {
        assertEquals(sqrt(100.0 * 100.0 * 2.0), segment.length)
    }

    @Test
    fun `a linear segment has a normal`() {
        segment.normal(0.0)
    }

    @Test
    fun `a linear segment can be promoted to a quadratic segment`() {
        val quadratic = segment.quadratic
        assertEquals(segment.position(0.0), quadratic.position(0.0))
        assertEquals(segment.position(0.25), quadratic.position(0.25))
        assertEquals(segment.position(0.5), quadratic.position(0.5))
        assertEquals(segment.position(0.75), quadratic.position(0.75))
        assertEquals(segment.position(1.0), quadratic.position(1.0))

        assertVeryNear(segment.normal(0.0), quadratic.normal(0.0))
        assertVeryNear(segment.normal(0.25), quadratic.normal(0.25))
        assertVeryNear(segment.normal(0.5), quadratic.normal(0.5))
        assertVeryNear(segment.normal(0.75), quadratic.normal(0.75))
        assertVeryNear(segment.normal(1.0), quadratic.normal(1.0))
    }

    @Test
    fun `a linear segment can be promoted to a cubic segment`() {
        val cubic = segment.cubic
        assertVeryNear(segment.position(0.0), cubic.position(0.0))
        assertVeryNear(segment.position(0.25), cubic.position(0.25))
        assertVeryNear(segment.position(0.5), cubic.position(0.5))
        assertVeryNear(segment.position(0.75), cubic.position(0.75))
        assertVeryNear(segment.position(1.0), cubic.position(1.0))
    }

    @Test
    fun `a linear segment can sample equidistant points`() {
        for (i in 2 until 100) {
            val points = segment.equidistantPositions(i)
            assertEquals(i, points.size)
            assertVeryNear(segment.position(0.0), points.first())
            assertVeryNear(segment.position(1.0), points.last())
        }
    }
}

class TestQuadraticSegmentWithCoIncidingP0C0 {
    val segment = Segment2D(
        Vector2(10.0, 10.0),
        Vector2(10.0, 10.0),
        Vector2(100.0, 100.0)
    )

    @Test
    fun `has a non-zero derivative at t=0_0`() {
        assertTrue(segment.derivative(0.0).squaredLength > 0.0)
    }

    @Test
    fun `has a non-zero derivative at t=0_5`() {
        assertTrue(segment.derivative(0.5).squaredLength > 0.0)
    }

    @Test
    fun `has a non-zero derivative at t=1_0`() {
        assertTrue(segment.derivative(1.0).squaredLength > 0.0)
    }

    @Test
    fun `it can sample equidistant points`() {
        for (i in 2 until 100) {
            val points = segment.equidistantPositions(i)
            assertEquals(i, points.size)
            assertVeryNear(segment.position(0.0), points.first())
            assertVeryNear(segment.position(1.0), points.last())
        }
    }
}

class TestQuadraticSegmentWithCoIncidingP1C0 {
    val segment = Segment2D(
        Vector2.ZERO,
        Vector2(100.0, 100.0),
        Vector2(100.0, 100.0)
    )

    @Test
    fun `has a non-zero derivative at t=0_0`() {
        assertTrue(segment.derivative(0.0).squaredLength > 0.0)
    }

    @Test
    fun `has a non-zero derivative at t=0_5`() {
        assertTrue(segment.derivative(0.5).squaredLength > 0.0)
    }

    @Test
    fun `has a non-zero derivative at t=1_0`() {
        assertTrue(segment.derivative(1.0).squaredLength > 0.0)
    }

    @Test
    fun `it can sample equidistant points`() {
        for (i in 2 until 100) {
            val points = segment.equidistantPositions(i)
            assertEquals(i, points.size)
            assertVeryNear(segment.position(0.0), points.first())
            assertVeryNear(segment.position(1.0), points.last())
        }
    }
}

class TestCubicSegmentWithCoIncidingP0C0 {
    val segment = Segment2D(
        Vector2(10.0, 10.0),
        Vector2(10.0, 10.0),
        Vector2(50.0, 50.0),
        Vector2(100.0, 100.0)
    )

    @Test
    fun `has a non-zero derivative at t=0`() {
        assertTrue(segment.derivative(0.0).squaredLength > 0.0)
    }

    @Test
    fun `has a non-zero derivative at t=epsilon`() {
        assertTrue(segment.derivative(0.00001).squaredLength > 0.0)
    }

    @Test
    fun `has a non-zero derivative at t=oneThird`() {
        assertTrue(segment.derivative(1.0 / 3.0).squaredLength > 0.0)
    }

    @Test
    fun `has a non-zero derivative at t=twoThirds`() {
        assertTrue(segment.derivative(2.0 / 3.0).squaredLength > 0.0)
    }

    @Test
    fun `has a non-zero derivative at t=1`() {
        assertTrue(segment.derivative(1.0).squaredLength > 0.0)
    }

    @Test
    fun `it can sample equidistant points`() {
        for (i in 2 until 100) {
            val points = segment.equidistantPositions(i)
            assertEquals(i, points.size)
            assertVeryNear(segment.position(0.0), points.first())
            assertVeryNear(segment.position(1.0), points.last())
        }
    }
}

class TestCubicSegmentWithCoIncidingP1C1 {
    val segment = Segment2D(
        Vector2(10.0, 10.0),
        Vector2(50.0, 50.0),
        Vector2(100.0, 100.0),
        Vector2(100.0, 100.0)
    )

    @Test
    fun `has a non-zero derivative at t=0`() {
        assertTrue(segment.derivative(0.0).squaredLength > 0.0)
    }

    @Test
    fun `has a non-zero derivative at t=epsilon`() {
        assertTrue(segment.derivative(0.00001).squaredLength > 0.0)
    }

    @Test
    fun `has a non-zero derivative at t=oneThird`() {
        assertTrue(segment.derivative(1.0 / 3.0).squaredLength > 0.0)
    }

    @Test
    fun `has a non-zero derivative at t=twoThirds`() {
        assertTrue(segment.derivative(2.0 / 3.0).squaredLength > 0.0)
    }

    @Test
    fun `has a non-zero derivative at t=1`() {
        assertTrue(segment.derivative(1.0).squaredLength > 0.0)
    }

    @Test
    fun `it can sample equidistant points`() {
        for (i in 2 until 100) {
            val points = segment.equidistantPositions(i)
            assertEquals(i, points.size)
            assertVeryNear(segment.position(0.0), points.first())
            assertVeryNear(segment.position(1.0), points.last())
        }
    }
}

class TestCubicSegmentWithCoIncidingP0C0andP1C1 {
    val segment = Segment2D(
        Vector2(10.0, 10.0),
        Vector2(10.0, 10.0),
        Vector2(100.0, 100.0),
        Vector2(100.0, 100.0)
    )

    @Test
    fun `has a non-zero derivative at t=0`() {
        assertTrue(segment.derivative(0.0).squaredLength > 0.0)
    }

    @Test
    fun `has a non-zero derivative at t=epsilon`() {
        assertTrue(segment.derivative(0.00001).squaredLength > 0.0)
    }

    @Test
    fun `has a non-zero derivative at t=oneThird`() {
        assertTrue(segment.derivative(1.0 / 3.0).squaredLength > 0.0)
    }

    @Test
    fun `has a non-zero derivative at t=oneHalf`() {
        assertTrue(segment.derivative(1.0 / 2.0).squaredLength > 0.0)
    }

    @Test
    fun `has a non-zero derivative at t=twoThirds`() {
        assertTrue(segment.derivative(2.0 / 3.0).squaredLength > 0.0)
    }

    @Test
    fun `has a non-zero derivative at t=1`() {
        assertTrue(segment.derivative(1.0).squaredLength > 0.0)
    }

    @Test
    fun `it can sample equidistant points`() {
        for (i in 2 until 100) {
            val points = segment.equidistantPositions(i)
            assertEquals(i, points.size)
            assertVeryNear(segment.position(0.0), points.first())
            assertVeryNear(segment.position(1.0), points.last())
        }
    }
}

class TestCubicSegmentWithCoIncidingC0C1 {
    val segment = Segment2D(
        Vector2(10.0, 10.0),
        Vector2(50.0, 50.0),
        Vector2(50.0, 50.0),
        Vector2(100.0, 100.0)
    )

    @Test
    fun `has a non-zero derivative at t=0`() {
        assertTrue(segment.derivative(0.0).squaredLength > 0.0)
    }

    @Test
    fun `has a non-zero derivative at t=epsilon`() {
        assertTrue(segment.derivative(0.00001).squaredLength > 0.0)
    }

    @Test
    fun `has a non-zero derivative at t=oneThird`() {
        assertTrue(segment.derivative(1.0 / 3.0).squaredLength > 0.0)
    }

    @Test
    fun `has a non-zero derivative at t=oneHalf`() {
        assertTrue(segment.derivative(1.0 / 2.0).squaredLength > 0.0)
    }

    @Test
    fun `has a non-zero derivative at t=twoThirds`() {
        assertTrue(segment.derivative(2.0 / 3.0).squaredLength > 0.0)
    }

    @Test
    fun `has a non-zero derivative at t=1`() {
        assertTrue(segment.derivative(1.0).squaredLength > 0.0)
    }

    @Test
    fun `it can sample equidistant points`() {
        for (i in 2 until 100) {
            val points = segment.equidistantPositions(i)
            assertEquals(i, points.size)
            assertVeryNear(segment.position(0.0), points.first())
            assertVeryNear(segment.position(1.0), points.last())
        }
    }
}

class TestCubicSegment {

    val segment = Segment2D(
        Vector2(0.0, 0.0),
        Vector2(100.0, 100.0),
        Vector2(50.0, 100.0),
        Vector2(0.0, 100.0)
    )

    @Test
    fun `has evaluable bounds`() {
        segment.bounds
    }

    @Test
    fun `has evaluable extrema`() {
        segment.extrema()
    }


    @Test
    fun `can be split in half`() {
        val sides = segment.split(0.5)
        assertEquals(2, sides.size)
    }

//    @Test
//    fun `can be split at 0_0, but result in 1 part`() {
//        val sides = segment.split(0.0)
//        assertEquals(1, sides.size)
//    }
//
//    @Test
//    fun `can be split at 1_0, but result in 1 part`() {
//        val sides = segment.split(1.0)
//        assertEquals(1, sides.size)
//    }

    @Test
    fun `can be subbed from 0_0 to 1_0`() {
        val sub = segment.sub(0.0, 1.0)
        assertEquals(segment, sub)
    }

    @Test
    fun `can be subbed from 0_0 to 0_5`() {
        val sub = segment.sub(0.0, 0.5)
        assertEquals(0.0, (sub.start - segment.start).squaredLength)
    }

    @Test
    fun `can be subbed from 0_5 to 1_0`() {
        val sub = segment.sub(0.5, 1.0)
        assertEquals(0.0, (sub.end - segment.end).squaredLength)
    }

    @Test
    fun `it can sample equidistant points`() {
        for (i in 2 until 100) {
            val points = segment.equidistantPositions(i)
            assertEquals(i, points.size)
            assertVeryNear(segment.position(0.0), points.first())
            assertVeryNear(segment.position(1.0), points.last())
        }
    }
}
