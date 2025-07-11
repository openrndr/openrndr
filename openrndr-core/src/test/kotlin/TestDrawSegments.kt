import org.openrndr.application
import org.openrndr.math.Vector2
import org.openrndr.shape.Segment2D
import kotlin.test.Test

class TestDrawSegments {
    @Test
    fun `drawing multiple segments using a List of Segment should not throw exceptions`() {
        application {
            program {
                val segments = List(10) {
                    Segment2D(
                        Vector2(
                            (it * 1001.0) % width,
                            (it * 1337.0) % height
                        ),
                        Vector2(
                            (it * 3333.0) % width,
                            (it * 5555.0) % height
                        ),
                        Vector2(
                            (it * 6502.0) % width,
                            (it * 4004.0) % height
                        )
                    )
                }
                extend {
                    drawer.segments(segments)
                    application.exit()
                }
            }
        }
    }

    @Test
    fun `drawing one segment using a List of Segment should not throw exceptions`() {
        application {
            program {
                val segments = List(1) {
                    Segment2D(
                        Vector2(
                            (it * 1001.0) % width,
                            (it * 1337.0) % height
                        ),
                        Vector2(
                            (it * 3333.0) % width,
                            (it * 5555.0) % height
                        ),
                        Vector2(
                            (it * 6502.0) % width,
                            (it * 4004.0) % height
                        )
                    )
                }
                extend {
                    drawer.segments(segments)
                    application.exit()
                }
            }
        }
    }
}