import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Winding
import org.openrndr.shape.contour
import kotlin.test.*

class TestShapeContourEquivalence {
    @Test
    fun `two equivalent org_openrndr_shape_contours`() {
        val a = Circle(40.0, 40.0, 100.0).contour
        val b = Circle(40.0, 40.0, 100.0).contour
        assertEquals(a, b, "should be equal")
    }

    @Test
    fun `two non-equivalent org_openrndr_shape_contours`() {
        val a = Circle(40.0, 40.0, 100.0).contour
        val b = Circle(40.0, 80.0, 100.0).contour
        assertNotEquals(a, b, "should not be equal")
    }
}

class TestShapeContour {
    val c = contour {
        moveTo(0.0, 0.0)
        lineTo(100.0, 100.0)
    }

    @Test
    fun `can sample 0 points`() {
        val points = c.equidistantPositions(0)
        assertEquals(0, points.size)
    }

    @Test
    fun `can sample 1 points`() {
        val points = c.equidistantPositions(1)
        assertVeryNear(points[0], Vector2.ZERO)
        assertEquals(1, points.size)

    }

    @Test
    fun `can sample 2 points`() {
        val points = c.equidistantPositions(2)
        assertVeryNear(Vector2.ZERO, points[0])
        assertVeryNear(Vector2(100.0, 100.0), points[1])
        assertEquals(2, points.size)

    }

    @Test
    fun `can sample 3 points`() {
        val points = c.equidistantPositions(3)
        assertVeryNear(Vector2.ZERO, points[0])
        assertVeryNear(Vector2(50.0, 50.0), points[1])
        assertVeryNear(Vector2(100.0, 100.0), points[2])
        assertEquals(3, points.size)
    }

    @Test
    fun `can sample 4 points`() {
        val points = c.equidistantPositions(4)
        assertVeryNear(Vector2.ZERO, points[0])
        assertVeryNear(Vector2(100.0 / 3, 100.0 / 3), points[1])
        assertVeryNear(Vector2(200.0 / 3, 200.0 / 3), points[2])
        assertVeryNear(Vector2(100.0, 100.0), points[3])
        assertEquals(4, points.size)
    }
}

class TestShapeContourWithTwoLines {
    val curve = contour {
        moveTo(0.0, 0.0)
        lineTo(100.0, 100.0)
        lineTo(200.0, 100.0)
    }

    @Test
    fun `can be sampled adaptively`() {
        val resampled = curve.sampleLinear()
        assertVeryNear(curve.position(0.0), resampled.position(0.0))
        assertVeryNear(curve.position(1.0), resampled.position(1.0))
        assertFalse(resampled.closed)
    }

    @Test
    fun `can be sampled for equidistant points`() {
        for (i in 4 until 100) {
            val points = curve.equidistantPositions(i)
            assertEquals(i, points.size)
        }
    }

    @Test
    fun `can be sampled for equidistant linear segments`() {
        for (i in 4 until 100) {
            val resampled = curve.sampleEquidistant(i)
            assertVeryNear(curve.position(0.0), resampled.position(0.0))
            assertVeryNear(curve.position(1.0), resampled.position(1.0))
            assertFalse(resampled.closed)
        }
    }
}

class TestSimpleOpenContour {
    val width = 640
    val height = 480

    val curve = contour {
        moveTo(Vector2(0.1 * width, 0.3 * height))
        continueTo(Vector2(0.5 * width, 0.5 * height))
        continueTo(Vector2(0.9 * width, 0.3 * height))
    }

    @Test
    fun `returns the expected number of points for equidistantPositions`() {
        assertEquals(100, curve.equidistantPositions(100).size)
    }
}

class TestSimpleClosedContour {
    val width = 640
    val height = 480
    val curve = contour {
        moveTo(Vector2(0.1 * width, 0.3 * height))
        continueTo(Vector2(0.5 * width, 0.5 * height))
        continueTo(Vector2(0.9 * width, 0.3 * height))
        continueTo(Vector2(0.1 * width, 0.3 * height))
        close()
    }

    @Test
    fun `has proper normals`() {
        for (i in 0..1000) {
            val normal = curve.normal(i / 1000.0)
            assertEquals(normal.x, normal.x)
            assertEquals(normal.y, normal.y)
        }
    }

    @Test
    fun `it has the right number of segments`() {
        assertEquals(3, curve.segments.size)
    }

    @Test
    fun `it is properly closed`() {
        assertVeryNear(curve.position(0.0), curve.position(1.0))
    }

    @Test
    fun `can be sampled for equidistant linear segments`() {
        for (i in 4 until 100) {
            val resampled = curve.sampleEquidistant(i)
            assertVeryNear(resampled.position(0.0), curve.position(0.0))
            assertVeryNear(resampled.position(1.0), curve.position(1.0))
        }
    }

    @Test
    fun `returns the expected number of points for equidistantPositions`() {
        assertEquals(100, curve.equidistantPositions(100).size)
    }
}

class TestSimpleClosedContour2 {
    // https://github.com/openrndr/openrndr/issues/79#issuecomment-601119834
    val curve = contour {
        moveTo(Vector2(60.0, 200.0))
        continueTo(Vector2(300.0, 300.0))
        continueTo(Vector2(280.0, 200.0))
        continueTo(Vector2(60.0, 200.0))
        close()
    }

    @Test
    fun `has no zero-length segments`() {
        val positions = curve.adaptivePositions()
        assertTrue(positions.zipWithNext().all { (it.second - it.first).squaredLength > 0.0 })
    }

    @Test
    fun `has CCW winding`() {
        assertEquals(Winding.COUNTER_CLOCKWISE, curve.winding)
    }

    @Test
    fun `can be sampled adaptively`() {
        val resampled = curve.sampleLinear()
        assertTrue(resampled.closed)
        assertVeryNear(resampled.position(0.0), curve.position(0.0))
        assertVeryNear(resampled.position(1.0), curve.position(1.0))
    }
}

class TestCircleContour {
    val curve = Circle(100.0, 100.0, 200.0).contour

    @Test
    fun `can be sampled adaptively`() {
        val resampled = curve.sampleLinear()
        assertTrue(resampled.closed)
        assertVeryNear(curve.position(0.0), resampled.position(0.0))
        assertVeryNear(curve.position(1.0), resampled.position(1.0))
    }

    @Test
    fun `can be sampled for equidistant linear segments`() {
        for (i in 4 until 100) {
            val resampled = curve.sampleEquidistant(i)
            assertVeryNear(curve.position(0.0), resampled.position(0.0))
            assertTrue(resampled.closed)
        }
    }

    @Test
    fun `it can be subbed from 0_0 to 1_0`() {
        val s = curve.sub(0.0, 1.0)
        assertVeryNear(curve.position(0.0), s.position(0.0))
        assertVeryNear(curve.position(1.0), s.position(1.0))
    }

    @Test
    fun `it can be subbed from -0_1 to 0_0`() {
        curve.sub(-0.1, 0.0)
    }

    @Test
    fun `it can be subbed from -2_0 to -1_0`() {
        val s = curve.sub(-2.0, -1.0)
        assertVeryNear(curve.position(0.0), s.position(0.0))
        assertVeryNear(curve.position(1.0), s.position(1.0))
    }

    @Test
    fun `it can be subbed from 0 to 0`() {
        val s = curve.sub(0.0, 0.0)
        println(s.length)
    }

    @Test
    fun `it can be subbed from -1 to -1`() {
        curve.sub(-1.0, -1.0)
    }
//        @Test
//        fun `it can be subbed from 0.0 to 0.001`() {
//            for (i in -2000 .. 10000) {
//                val o = i / 10000
//                val s = curve.sub(0.0 + o, 0.01 + o)
//                s.position(0.0) `should be somewhat near` curve.position(0.0 + o)
//                s.position(1.0) `should be somewhat near` curve.position(0.01+ o)
//            }
//        }

    @Test
    fun `it can be subbed from 0_0 to 0_000001 `() {
        val s = curve.sub(0.0, 1 / 100000.0)
        assertSomewhatNear(s.position(1.0), curve.position(1 / 100000.0))
    }

    @Test
    fun `adaptive positions and corners should have the same length`() {
        val pc = curve.adaptivePositionsAndCorners()
        assertEquals(pc.first.size, pc.second.size)
    }

//        @Test
//        fun `it can be subbed from 0.0 to 0.001`() {
//                val s = curve.sub(0.0, i/100000.0)
//                println(i)
//                assertSomewhatNear(s.position(1.0), curve.position(i/100000.0))
//            }
//        }

}

class TestRectangleContour {
    val curve = Rectangle(100.0, 100.0, 200.0, 200.0).contour

    @Test
    fun `can be sampled adaptively`() {
        val resampled = curve.sampleLinear()
        assertTrue(resampled.closed)
        assertVeryNear(curve.position(0.0), resampled.position(0.0))
        assertVeryNear(curve.position(1.0), resampled.position(1.0))
    }

    @Test
    fun `can be sampled for equidistant linear segments`() {
        for (i in 4 until 100) {
            val resampled = curve.sampleEquidistant(i)
            assertVeryNear(curve.position(0.0), resampled.position(0.0))
            assertVeryNear(curve.position(1.0), resampled.position(1.0))
            assertTrue(resampled.closed)
        }
    }
}

class TestVerySpecificContour {
    val c = contour {
        moveTo(0.0, 0.0)
        curveTo(204.0, 378.0, 800.0, 240.0)
        curveTo(204.0, 378.0, 0.0, 480.0)
    }

    @Test
    fun `returns the expected number of points for equidistantPositions`() {
        val points = c.equidistantPositions(100)
        assertEquals(100, points.size)
    }
}
