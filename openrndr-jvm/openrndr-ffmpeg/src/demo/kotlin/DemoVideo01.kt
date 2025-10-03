import org.openrndr.application
import org.openrndr.ffmpeg.loadVideo

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
                println("video ended")
                video.seek(0.0)
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