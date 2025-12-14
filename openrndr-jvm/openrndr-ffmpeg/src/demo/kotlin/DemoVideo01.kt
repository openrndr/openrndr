import kotlinx.coroutines.delay
import org.openrndr.application
import org.openrndr.ffmpeg.loadVideo
import org.openrndr.launch

/**
 * A simple demo that demonstrates VideoPlayerFFMPEG. This is mostly here to provide a quick in-repo way to test
 * video playback. To run this set the `video` environment variable to the path of the video you want to play.
 */

fun main() {
    application {
        program {

            val videoFile = System.getenv("video")
            require(videoFile != null) {
                "Set the video environment variable to the path of the video you want to play"
            }

            val video = loadVideo(videoFile)
            video.play()

            video.ended.listen {
                launch {
                    delay(100)
                    video.seek(0.0)
                    video.play()
                }
            }

            mouse.buttonDown.listen {
                video.seek((it.position.x / width) * (video.duration))
            }

            extend {
                video.draw(drawer)
                drawer.rectangle(0.0, height-10.0,(video.position / video.duration) * width, 10.0)
            }
        }
    }
}