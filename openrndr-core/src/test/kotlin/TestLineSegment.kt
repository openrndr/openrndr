import org.openrndr.math.Vector2
import org.openrndr.shape.LineSegment
import org.openrndr.shape.intersection
import io.kotest.core.spec.style.DescribeSpec

class TestLineSegment : DescribeSpec({
    describe("a crossing horizontal and vertical line segment") {
        val h = LineSegment(-100.0, 0.0, 100.0, 0.0)
        val v = LineSegment(0.0, -100.0, 0.0, 100.0)

        it("they should intersect") {
            val i = intersection(h, v, eps = 0.0)
            i `should be near` Vector2.ZERO
        }
    }

    describe("a ┬ shaped crossing of horizontal and vertical line segments") {
        val h = LineSegment(-100.0, 0.0, 100.0, 0.0)
        val v = LineSegment(0.0, 0.0, 0.0, 100.0)

        it("should intersect") {
            val i = intersection(h, v, eps = 0.0)
            i `should be near` Vector2.ZERO
        }
    }

    describe("a ├ shaped crossing of horizontal and vertical line segments") {
        val h = LineSegment(0.0, 0.0, 100.0, 0.0)
        val v = LineSegment(0.0, -100.0, 0.0, 100.0)

        it("should intersect") {
            val i = intersection(h, v, eps = 0.0)
            i `should be near` Vector2.ZERO
        }
    }

    describe("a L shaped crossing horizontal and vertical line segment") {
        val h = LineSegment(0.0, 0.0, 100.0, 0.0)
        val v = LineSegment(0.0, 0.0, 0.0, 100.0)

        it("they should intersect") {
            val i = intersection(h, v, eps = 0.0)
            i `should be near` Vector2.ZERO
        }
    }

    describe("two parallel lines") {
        val h = LineSegment(-100.0, 0.0, 100.0, 0.0)
        val v = LineSegment(-100.0, 1.0, 100.0, 1.0)

        it("they should not intersect") {
            val i = intersection(h, v)
            i `should be near` Vector2.INFINITY
        }
    }

})