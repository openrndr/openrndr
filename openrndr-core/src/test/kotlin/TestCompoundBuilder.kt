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
    }

    val sc = compound {
        union {
            shape(Circle(185.0, height / 2.0 - 80.0, 100.0).shape)
            shape(Circle(185.0, height / 2.0 + 80.0, 100.0).shape)
        }
    }

    @Test
    fun `a union compound has one shape`() {
        assertEquals(1, sc.size)
    }

    @Test
    fun `a union compound shape has one contour`() {
        assertEquals(1, sc[0].contours.size)
    }

    @Test
    fun `a union compound contour has clockwise winding`() {
        assertEquals(Winding.CLOCKWISE, sc[0].contours[0].winding)
    }

    @Test
    fun `a union compound contour has right polarity`() {
        assertEquals(YPolarity.CW_NEGATIVE_Y, sc[0].contours[0].polarity)
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
    }

    val sc = compound {
        difference {
            shape(Circle(385.0, height / 2.0 - 80.0, 100.0).shape)
            shape(Circle(385.0, height / 2.0 + 80.0, 100.0).shape)
        }
    }

    @Test
    fun `a difference compound has one shape`() {
        assertEquals(1, sc.size)
    }

    @Test
    fun `a difference compound shape has one contour`() {
        assertEquals(1, sc[0].contours.size)
    }

    @Test
    fun `a difference compound contour has clockwise winding`() {
        assertEquals(Winding.CLOCKWISE, sc[0].contours[0].winding)
    }

    @Test
    fun `a difference compound contour has right polarity`() {
        assertEquals(YPolarity.CW_NEGATIVE_Y, sc[0].contours[0].polarity)
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
    }

    val sc = compound {
        intersection {
            shape(Circle(385.0, height / 2.0 - 80.0, 100.0).shape)
            shape(Circle(385.0, height / 2.0 + 80.0, 100.0).shape)
        }
    }

    @Test
    fun `an intersection compound has one shape`() {
        assertEquals(1, sc.size)
    }

    @Test
    fun `an intersection compound shape has one contour`() {
        assertEquals(1, sc[0].contours.size)
    }

    @Test
    fun `an intersection compound contour has clockwise winding`() {
        assertEquals(Winding.CLOCKWISE, sc[0].contours[0].winding)
    }

    @Test
    fun `an intersection compound contour has right polarity`() {
        assertEquals(YPolarity.CW_NEGATIVE_Y, sc[0].contours[0].polarity)
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
    }

    val sc = compound {
        intersection {
            shape(Circle(385.0, height / 2.0 - 80.0, 100.0).shape)
            shape(Rectangle(0.0, 0.0, 20.0, 20.0).shape)
        }
    }

    @Test
    fun `a disjunct intersection compound should have no shapes`() {
        assertEquals(0, sc.size)
    }
}
