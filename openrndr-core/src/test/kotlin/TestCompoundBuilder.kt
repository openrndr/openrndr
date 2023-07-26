import org.amshove.kluent.`should be equal to`
import org.openrndr.math.YPolarity
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Winding
import org.openrndr.shape.compound
import io.kotest.core.spec.style.DescribeSpec

object TestCompoundBuilder : DescribeSpec({
    val height = 480
    describe("a simple org.openrndr.shape.union org.openrndr.shape.compound") {
        val sc = compound {
            union {
                shape(Circle(185.0, height / 2.0 - 80.0, 100.0).shape)
                shape(Circle(185.0, height / 2.0 + 80.0, 100.0).shape)
            }
        }
        it("has one shape") {
            sc.size `should be equal to` 1
        }

        it("shape has one contour") {
            sc[0].contours.size `should be equal to` 1
        }

        it("contour has clockwise winding") {
            sc[0].contours[0].winding `should be equal to` Winding.CLOCKWISE
        }

        it("contour has right polarity") {
            sc[0].contours[0].polarity `should be equal to` YPolarity.CW_NEGATIVE_Y
        }
    }

    describe("a simple difference org.openrndr.shape.compound") {
        // -- shape difference
        val sc = compound {
            difference {
                shape(Circle(385.0, height / 2.0 - 80.0, 100.0).shape)
                shape(Circle(385.0, height / 2.0 + 80.0, 100.0).shape)
            }
        }
        it("has one shape") {
            sc.size `should be equal to` 1
        }

        it("shape has one contour") {
            sc[0].contours.size `should be equal to` 1
        }

        it("contour has clockwise winding") {
            sc[0].contours[0].winding `should be equal to` Winding.CLOCKWISE
        }

        it("contour has right polarity") {
            sc[0].contours[0].polarity `should be equal to` YPolarity.CW_NEGATIVE_Y
        }
    }

    describe("a simple intersection org.openrndr.shape.compound") {
        // -- shape difference
        val sc = compound {
            intersection {
                shape(Circle(385.0, height / 2.0 - 80.0, 100.0).shape)
                shape(Circle(385.0, height / 2.0 + 80.0, 100.0).shape)
            }
        }
        it("has one shape") {
            sc.size `should be equal to` 1
        }

        it("shape has one contour") {
            sc[0].contours.size `should be equal to` 1
        }

        it("contour has clockwise winding") {
            sc[0].contours[0].winding `should be equal to` Winding.CLOCKWISE
        }

        it("contour has right polarity") {
            sc[0].contours[0].polarity `should be equal to` YPolarity.CW_NEGATIVE_Y
        }
    }

    describe("a disjunct intersection org.openrndr.shape.compound") {
        val sc = compound {
            intersection {
                shape(Circle(385.0, height / 2.0 - 80.0, 100.0).shape)
                shape(Rectangle(0.0, 0.0, 20.0, 20.0).shape)
            }
        }
        it("it should have no shapes") {
            sc.size `should be equal to` 0
        }
    }
})
