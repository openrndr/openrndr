import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadFont
import org.openrndr.draw.loadFontImageMap
import org.openrndr.ffmpeg.loadVideo
import org.openrndr.math.Polar
import org.openrndr.math.Vector2
import org.openrndr.resourceUrl

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

            var videoSeconds = 0.0
            video.newFrame.listen {
                videoSeconds = it.timeStamp
            }

            mouse.buttonDown.listen {
                video.seek((it.position.x / width) * (video.duration))
            }

            val font = loadFont("https://github.com/IBM/plex/raw/master/packages/plex-mono/fonts/complete/otf/IBMPlexMono-Bold.otf", 24.0)

            extend {
                video.draw(drawer)
                drawer.rectangle(0.0, height - 10.0, (video.position / video.duration) * width, 10.0)

                drawer.fontMap = font
                drawer.text("%.2f".format(videoSeconds), 50.0, 50.0)
            }
        }
    }
}