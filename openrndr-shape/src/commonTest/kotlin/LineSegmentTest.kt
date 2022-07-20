import org.openrndr.math.Vector2
import org.openrndr.shape.LineSegment
import kotlin.test.*

class LineSegmentTest {

    @Test
    fun shouldCalculateSimplePointAtLength() {
        val line = LineSegment(Vector2(110.0, 150.0), Vector2(210.0, 30.0))
        assertEquals(Vector2(113.2009219983224, 146.1588936020131), line.pointAtLength(5.0), 0.0001)
        assertEquals(Vector2(186.8221279597376, 57.81344644831489), line.pointAtLength(120.0), 0.0001)
        assertEquals(Vector2(206.027659949672, 34.76680806039363), line.pointAtLength(150.0), 0.0001)
        assertEquals(line.start, line.pointAtLength(-500.0))
        assertEquals(line.end, line.pointAtLength(500.0))
    }
}