import org.amshove.kluent.`should be equal to`
import org.openrndr.math.YPolarity
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Winding
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TestRectangle : Spek({


    describe("A rectangle's contour") {

        val c = Rectangle(50.0,50.0, 200.0, 200.0).contour
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