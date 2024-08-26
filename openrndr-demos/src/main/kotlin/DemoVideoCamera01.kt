import org.openrndr.application
import org.openrndr.ffmpeg.loadVideoDevice

fun main() {
    application {
        program {
            val vp = loadVideoDevice(
//                configuration = VideoPlayerConfiguration().also {
//                    it.useHardwareDecoding = false
//                }
            )
            vp.play()
            extend {
                vp.draw(drawer)
            }
        }
    }
}