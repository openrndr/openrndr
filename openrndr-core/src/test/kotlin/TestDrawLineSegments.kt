import org.openrndr.application
import org.openrndr.shape.LineSegment
import kotlin.test.Test


class TestDrawLineSegments {
    @Test
    fun `drawing multiple lineSegments using a List of LineSegment should not throw exceptions`() {
        application {
            program {
                val segments = List(10) {
                    LineSegment(
                        (it * 1001.0) % width,
                        (it * 1337.0) % height,
                        (it * 6502.0) % width,
                        (it * 4004.0) % height
                    )
                }
                extend {
                    drawer.lineSegments(segments)
                    application.exit()
                }
            }
        }
    }

    @Test
    fun `drawing a single line segment should not throw exceptions`() {
        application {
            program {
                extend {
                    val ls = LineSegment(0.0, 0.0, 50.0, 50.0)
                    drawer.lineSegment(ls)
                    application.exit()
                }
            }
        }
    }

    @Test
    fun `drawing one lineSegment using a List of LineSegment should not throw exceptions`() {
        application {
            program {
                val segments = List(1) {
                    LineSegment(
                        (it * 1001.0) % width,
                        (it * 1337.0) % height,
                        (it * 6502.0) % width,
                        (it * 4004.0) % height
                    )
                }
                extend {
                    drawer.lineSegments(segments)
                    application.exit()
                }
            }
        }
    }
}