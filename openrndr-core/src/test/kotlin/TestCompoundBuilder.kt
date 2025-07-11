import org.openrndr.math.YPolarity
import org.openrndr.shape.Circle
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Winding
import org.openrndr.shape.compound
import kotlin.test.Test
import kotlin.test.assertEquals

class TestCompoundBuilderUnion {
    val height = 480

    @Test
    fun `a simple org_openrndr_shape_union org_openrndr_shape_compound can be constructed`() {
        val sc = compound {
            union {
                shape(Circle(185.0, height / 2.0 - 80.0, 100.0).shape)
                shape(Circle(185.0, height / 2.0 + 80.0, 100.0).shape)
            }
        }
        assertEquals(
            1, sc.size,
            "a union compound has one shape"
        )
        assertEquals(
            1, sc[0].contours.size,
            "a union compound shape has one contour"
        )
        assertEquals(
            Winding.CLOCKWISE, sc[0].contours[0].winding,
            "a union compound contour has clockwise winding"
        )
        assertEquals(
            YPolarity.CW_NEGATIVE_Y, sc[0].contours[0].polarity,
            "a union compound contour has right polarity"
        )
    }
}

class TestCompoundBuilderDifference {
    val height = 480

    @Test
    fun `a simple org_openrndr_shape_difference org_openrndr_shape_compound can be constructed`() {
        val sc = compound {
            difference {
                shape(Circle(385.0, height / 2.0 - 80.0, 100.0).shape)
                shape(Circle(385.0, height / 2.0 + 80.0, 100.0).shape)
            }
        }
        assertEquals(
            1, sc.size,
            message = "a difference compound has one shape"
        )
        assertEquals(
            1, sc[0].contours.size,
            message = "a difference compound shape has one contour"
        )
        assertEquals(
            Winding.CLOCKWISE, sc[0].contours[0].winding,
            message = "a difference compound contour has clockwise winding"
        )
        assertEquals(
            YPolarity.CW_NEGATIVE_Y, sc[0].contours[0].polarity,
            message = "a difference compound contour has right polarity"
        )
    }
}

class TestCompoundBuilderIntersection {
    val height = 480

    @Test
    fun `a simple org_openrndr_shape_intersection org_openrndr_shape_compound can be constructed`() {
        val sc = compound {
            intersection {
                shape(Circle(385.0, height / 2.0 - 80.0, 100.0).shape)
                shape(Circle(385.0, height / 2.0 + 80.0, 100.0).shape)
            }
        }
        assertEquals(
            1, sc.size,
            message = "an intersection compound has one shape"
        )
        assertEquals(
            1, sc[0].contours.size,
            message = "an intersection compound shape has one contour"
        )
        assertEquals(
            Winding.CLOCKWISE, sc[0].contours[0].winding,
            message = "an intersection compound contour has clockwise winding"
        )
        assertEquals(
            YPolarity.CW_NEGATIVE_Y, sc[0].contours[0].polarity,
            message = "an intersection compound contour has right polarity"
        )
    }
}

class TestCompoundBuilderDisjunct {
    val height = 480

    @Test
    fun `a disjunct intersection compound can be constructed`() {
        val sc = compound {
            intersection {
                shape(Circle(385.0, height / 2.0 - 80.0, 100.0).shape)
                shape(Rectangle(0.0, 0.0, 20.0, 20.0).shape)
            }
        }
        assertEquals(
            0, sc.size,
            message = "a disjunct intersection compound should have no shapes"
        )
    }
}
