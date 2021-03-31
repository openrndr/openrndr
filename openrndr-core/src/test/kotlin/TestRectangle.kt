import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should be greater or equal to`
import org.amshove.kluent.`should be less or equal to`
import org.openrndr.math.Vector2
import org.openrndr.math.YPolarity
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Winding
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TestRectangle : Spek({

    describe("A rectangle with default height") {
        val width = 200.0

        it("has default height equal to width") {
            Rectangle(50.0, 50.0, width).height `should be equal to` width
            Rectangle.fromCenter(Vector2.ZERO, width).height `should be equal to` width
            Rectangle(Vector2.ZERO, width).height `should be equal to` width
        }
    }

    describe("A rectangle's contour") {

        val c = Rectangle(50.0, 50.0, 200.0, 200.0).contour
        it("is closed") {
            c.closed `should be equal to` true
        }
        it("the winding is CCW") {
            c.winding `should be equal to` Winding.CLOCKWISE
        }

        it("the polarity is CW negative y") {
            c.polarity `should be equal to` YPolarity.CW_NEGATIVE_Y
        }
    }

    describe("A specific rectangle's contour") {
        // testing for issue described in https://github.com/openrndr/openrndr/issues/135
        val c = Rectangle(100.0, 100.0, 300.0, 300.0).contour

        it("can be sampled for adaptive positions") {
            val aps = c.adaptivePositions()
            for (p in aps) {
                p.x `should be greater or equal to` 100.0
                p.y `should be greater or equal to` 100.0
                p.x `should be less or equal to` 400.0
                p.y `should be less or equal to` 400.0
            }
        }

        it("can be sampled for equidistant positions") {
            val eps = c.equidistantPositions(10)
            eps.size `should be equal to` 11

            for (p in eps) {
                p.x `should be greater or equal to` 100.0 - 1E-6
                p.y `should be greater or equal to` 100.0 - 1E-6
                p.x `should be less or equal to` 400.0 + 1E-6
                p.y `should be less or equal to` 400.0 + 1E-6
            }
        }
    }

    describe("A rectangle") {
        val a = Rectangle.fromCenter(Vector2.ZERO, 50.0)
        val b = Rectangle.fromCenter(Vector2.ZERO, 100.0)
        val c = Rectangle(400.0, 400.0, 50.0)
        val d = Rectangle(410.0, 410.0, 50.0)

        it("intersects other rectangles") {
            a.intersects(b) `should be equal to` true
            b.intersects(a) `should be equal to` true
            c.intersects(d) `should be equal to` true
        }

        it("doesn't intersect other rectangles") {
            a.intersects(c) `should be equal to` false
        }
    }
})