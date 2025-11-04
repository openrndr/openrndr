import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.renderTarget
import org.openrndr.ffmpeg.VideoPlayerConfiguration
import org.openrndr.ffmpeg.loadVideo
import org.openrndr.ffmpeg.probeVideo
import java.io.File

fun main() = application {
    configure {
        width = 1600
        height = 720
    }

    program {
        val info = probeVideo(File("data/test.mp4"))
        println(info)

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
                videoPlayer.draw(drawer, blockUntilFinished = true)
            }
            drawer.image(renderTarget.colorBuffer(0))
        }
    }
}