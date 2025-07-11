import org.openrndr.application
import org.openrndr.shape.Circle
import org.openrndr.shape.LineSegment
import kotlin.test.Test

class TestVertexBufferGL3 {
    @Test
    fun `drawing a single contour should not throw exceptions`() {
        application {
            program {
                extend {
                    drawer.contour(Circle(width / 2.0, height / 2.0, 100.0).contour)
                    application.exit()
                }
            }
        }
    }

    @Test
    fun `drawing org_openrndr_shape_contours with a single element should not throw exceptions`() {
        application {
            program {
                extend {
                    drawer.contours(listOf(Circle(width / 2.0, height / 2.0, 100.0).contour))
                    application.exit()
                }
            }
        }
    }

    @Test
    fun `drawing org_openrndr_shape_contours with the same closed contour twice should not throw exceptions`() {
        application {
            program {
                extend {
                    drawer.contours(
                        listOf(
                            Circle(width / 2.0, height / 2.0, 100.0).contour,
                            Circle(width / 2.0, height / 2.0, 100.0).contour
                        )
                    )
                    application.exit()
                }
            }
        }
    }

    @Test
    fun `drawing short open org_openrndr_shape_contours using org_openrndr_shape_contours should not throw exceptions`() {
        application {
            program {
                extend {
                    drawer.contours(
                        listOf(
                            LineSegment(0.0, 0.0, 100.0, 100.0).contour,
                            LineSegment(100.0, 100.0, 0.0, 0.0).contour
                        )
                    )
                    application.exit()
                }
            }
        }
    }
}
