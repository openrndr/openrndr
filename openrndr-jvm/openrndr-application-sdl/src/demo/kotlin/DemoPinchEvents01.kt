import org.openrndr.PointerTracker
import org.openrndr.application

fun main() {
    application {
        program {

            var radius = 200.0
            gestures.pinchStarted.listen {
                println("pinch started")
            }

            gestures.pinchUpdated.listen {
                radius *= it.scale
            }

            gestures.pinchEnded.listen {
                println("pinch ended")
            }

            extend {
                drawer.circle(drawer.bounds.center, radius)
            }
        }
    }
}