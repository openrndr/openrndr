import org.openrndr.application
import org.openrndr.ffmpeg.PlayMode
import org.openrndr.ffmpeg.loadVideo

fun main() {
    application {
        program {
            val vp = loadVideo("C:\\Users\\edmin\\git\\one-offs\\freqmod\\data\\video\\launch-fix.mp4",
                mode = PlayMode.VIDEO
            )
            vp.play()
            extend {
                vp.draw(drawer)

            }
        }
    }
}