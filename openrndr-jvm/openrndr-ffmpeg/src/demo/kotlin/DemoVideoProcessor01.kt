import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.renderTarget
import org.openrndr.ffmpeg.VideoPlayerConfiguration
import org.openrndr.ffmpeg.loadVideo

fun main() = application {
    program {
        val videoPlayer = loadVideo("data/test.mp4", configuration = VideoPlayerConfiguration().apply {
            this.allowFrameSkipping = false
            this.synchronizeToClock = false

        })
        val renderTarget = renderTarget(width, height) {
            colorBuffer()
        }
        videoPlayer.play()

        extend {
            drawer.clear(ColorRGBa.BLACK)
            drawer.withTarget(renderTarget) {
                videoPlayer.draw(drawer)
            }
            //
            Thread.sleep(1000)
            drawer.image(renderTarget.colorBuffer(0))
        }
    }
}