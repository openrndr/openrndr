import org.amshove.kluent.`should be equal to`
import org.openrndr.math.YPolarity
import org.openrndr.shape.Circle
import org.openrndr.shape.Winding
import io.kotest.core.spec.style.DescribeSpec

object TestCircle : DescribeSpec({
    describe("A circle's contour") {
        val c = Circle(200.0, 200.0, 200.0).contour
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

})