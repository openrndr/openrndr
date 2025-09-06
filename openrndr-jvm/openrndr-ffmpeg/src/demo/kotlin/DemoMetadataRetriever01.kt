import org.openrndr.application
import org.openrndr.ffmpeg.MetadataRetriever

/**
 * Demonstrate how to use [MetadataRetriever] to query metadata from a media file.
 * typically used with video files, but works also with audio files or images.
 *
 */
fun main() = application {
    program {
        // During development, we pass the path to a video file as an env variable in the IDE.
        val videoFile: String? = System.getenv("video")

        require(videoFile != null) {
            "Set the video environment variable to the path of the video you want to query"
        }

        val metadataRetriever = MetadataRetriever()

        val metadata = metadataRetriever.query(videoFile)

        // Print available metadata
        println("Parsed video metadata: $metadata")

        // Access specific metadata
        val duration = metadata.format.duration

        // For convenience. Note that there may be more than one stream of each type.
        val audioStream = metadata.streams.firstOrNull { it.codec_type == "audio" }
        val videoStream = metadata.streams.firstOrNull { it.codec_type == "video" }

        println("""
            duration: $duration
            sampleRate: ${audioStream?.sample_rate}
            dimensions: ${videoStream?.width} x ${videoStream?.height}
        """.trimIndent())
    }
}