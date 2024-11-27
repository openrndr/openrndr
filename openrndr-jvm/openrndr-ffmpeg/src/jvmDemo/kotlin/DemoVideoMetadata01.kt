import org.openrndr.application
import org.openrndr.ffmpeg.VideoQuery

fun main() = application {
    program {
        val videoFile = System.getenv("video")
        require(videoFile != null) {
            "Set the video environment variable to the path of the video you want to query"
        }

        val videoMetadata = VideoQuery()

        println(videoMetadata.query(videoFile))
    }
}