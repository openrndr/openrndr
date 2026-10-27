import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.ffmpeg.PlayMode
import org.openrndr.ffmpeg.loadVideo

/**
 * Plays an audio file (WAV, MP3, AAC, ...) with VideoPlayerFFMPEG. Click to seek, the bar shows the play position.
 * To run this set the `audio` environment variable to the path of the audio file you want to play.
 */
fun main() {
    application {
        program {
            val audioFile = System.getenv("audio")
            require(audioFile != null) {
                "Set the audio environment variable to the path of the audio file you want to play"
            }

            val audio = loadVideo(audioFile, mode = PlayMode.AUDIO)
            audio.play()

            audio.ended.listen {
                audio.restart()
            }

            mouse.buttonDown.listen {
                audio.seek((it.position.x / width) * audio.duration)
            }

            extend {
                // draw updates the player, it also emits the ended event for audio only playback
                audio.draw(drawer)
                drawer.fill = ColorRGBa.PINK
                drawer.rectangle(0.0, height - 10.0, (audio.position / audio.duration) * width, 10.0)
            }
        }
    }
}
