import org.openrndr.application
import org.openrndr.ffmpeg.PlayMode
import org.openrndr.ffmpeg.loadVideo
import org.openrndr.ffmpeg.loadVideoDevice

fun main() {
    application {
        program {
            val vp = loadVideoDevice()
            vp.play()
            extend {
                vp.draw(drawer)
            }
        }
    }
}