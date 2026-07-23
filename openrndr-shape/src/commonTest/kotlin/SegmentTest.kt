import org.openrndr.math.Vector2
import org.openrndr.shape.Segment2D
import org.openrndr.shape.findIntersectionsX
import org.openrndr.shape.findIntersectionsY
import kotlin.test.*

class SegmentTest {

    @Test
    fun shouldCalculateSimplePointAtLength() {
        val curve =
            Segment2D(Vector2(110.0, 150.0), listOf(Vector2(25.0, 190.0), Vector2(210.0, 250.0)), Vector2(210.0, 30.0))
        assertEquals(Vector2(105.53567504882812, 152.2501678466797), curve.pointAtLength(5.0, 0.0001), 0.0005)
        assertEquals(Vector2(162.22564697265625, 170.3757781982422), curve.pointAtLength(120.0, 0.0001), 0.0005)
        assertEquals(curve.start, curve.pointAtLength(-500.0, 0.0001))
        assertEquals(curve.end, curve.pointAtLength(500.0, 0.0001))
    }

    @Test
    fun testFindIntersectionsX() {
        run {
            val linear = Segment2D(Vector2(0.0, 0.0), Vector2(100.0, 100.0))
            val intersections = linear.findIntersectionsX(50.0)
            assertEquals(1, intersections.size)
            assertEquals(0.5, intersections[0], 1e-6)
        }

        run {
            val quadratic = Segment2D(Vector2(0.0, 0.0), Vector2(100.0, 0.0), Vector2(0.0, 100.0))
            val intersections = quadratic.findIntersectionsX(50.0)
            assertEquals(1, intersections.size)
            assertEquals(0.5, intersections[0], 1e-6)
        }

        run {
            val cubic = Segment2D(Vector2(0.0, 0.0), Vector2(100.0, 0.0), Vector2(100.0, 100.0), Vector2(0.0, 100.0))
            val intersections = cubic.findIntersectionsX(75.0)
            assertEquals(1, intersections.size)
            assertEquals(0.5, intersections[0], 1e-6)
        }
    }

    @Test
    fun testFindIntersectionsY() {
        run {
            val linear = Segment2D(Vector2(0.0, 0.0), Vector2(100.0, 100.0))
            val intersections = linear.findIntersectionsY(50.0)
            assertEquals(1, intersections.size)
            assertEquals(0.5, intersections[0], 1e-6)
        }

        run {
            val quadratic = Segment2D(Vector2(0.0, 0.0), Vector2(0.0, 100.0), Vector2(100.0, 0.0))
            val intersections = quadratic.findIntersectionsY(50.0)
            assertEquals(1, intersections.size)
            assertEquals(0.5, intersections[0], 1e-6)
        }

        run {
            val cubic = Segment2D(Vector2(0.0, 0.0), Vector2(0.0, 100.0), Vector2(100.0, 100.0), Vector2(100.0, 0.0))
            val intersections = cubic.findIntersectionsY(75.0)
            assertEquals(1, intersections.size)
            assertEquals(0.5, intersections[0], 1e-6)
        }
    }
}