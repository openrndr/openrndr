import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should not be equal to`
import org.openrndr.math.Vector2
import org.openrndr.math.YPolarity
import org.openrndr.shape.Circle
import org.openrndr.shape.Winding
import org.openrndr.shape.shape
import io.kotest.core.spec.style.DescribeSpec

class TestShape : DescribeSpec({
    describe("two equivalent shapes") {
        val a = Circle(40.0, 40.0, 100.0).shape
        val b = Circle(40.0, 40.0, 100.0).shape
        it("should be equal") {
            a `should be equal to` b
        }
    }

    describe("two non-equivalent shapes") {
        val a = Circle(40.0, 40.0, 100.0).shape
        val b = Circle(40.0, 80.0, 100.0).shape
        it("should not be equal") {
            a `should not be equal to` b
        }
    }

    describe("a shape") {
        val width = 640
        val height = 480

        val s = shape {
            contour {
                moveTo(Vector2(width / 2.0 - 150.0, height / 2.0 - 150.00))
                lineTo(cursor + Vector2(300.0, 0.0))
                lineTo(cursor + Vector2(0.0, 300.0))
                lineTo(anchor)
                close()
            }
            contour {
                moveTo(Vector2(width / 2.0 - 80.0, height / 2.0 - 100.0))
                lineTo(cursor + Vector2(200.0, 0.0))
                lineTo(cursor + Vector2(0.0, 200.00))
                lineTo(anchor)
                close()
            }
        }

        it("should have 2 org.openrndr.shape.contours") {
            s.contours.size `should be equal to` 2
        }

        it("all org.openrndr.shape.contours should be closed") {
            s.contours.all { it.closed } `should be equal to` true
        }

        it("all org.openrndr.shape.contours have negative polarity") {
            s.contours.all { it.polarity == YPolarity.CW_NEGATIVE_Y } `should be equal to` true
        }

        it("first contour should have clockwise winding") {
            s.contours.first().winding `should be equal to` Winding.CLOCKWISE
        }

        it("second contour should have counter-clockwise winding") {
            s.contours[1].winding `should be equal to` Winding.COUNTER_CLOCKWISE
        }
    }
})