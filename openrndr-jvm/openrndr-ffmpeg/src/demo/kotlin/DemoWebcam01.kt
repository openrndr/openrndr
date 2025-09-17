import org.openrndr.application
import org.openrndr.ffmpeg.loadVideo
import org.openrndr.ffmpeg.loadVideoDevice

/**
 * A simple demo that demonstrates VideoPlayerFFMPEG. This is mostly here to provide a quick in-repo way to test
 * video playback. To run this set the `video` environment variable to the path of the video you want to play.
 */

fun main() {
    application {
        program {


            val video = loadVideoDevice()
            video.play()


            extend {
                video.draw(drawer)
            }
        }
    }
}