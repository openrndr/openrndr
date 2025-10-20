package org.openrndr.ffmpeg

import org.bytedeco.ffmpeg.avcodec.AVCodec
import org.bytedeco.ffmpeg.avformat.AVFormatContext
import org.bytedeco.ffmpeg.global.avformat.av_find_best_stream
import org.bytedeco.ffmpeg.global.avformat.av_guess_frame_rate
import org.bytedeco.ffmpeg.global.avformat.avformat_alloc_context
import org.bytedeco.ffmpeg.global.avformat.avformat_close_input
import org.bytedeco.ffmpeg.global.avformat.avformat_find_stream_info
import org.bytedeco.ffmpeg.global.avformat.avformat_free_context
import org.bytedeco.ffmpeg.global.avformat.avformat_open_input
import org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_AUDIO
import org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_VIDEO
import org.bytedeco.ffmpeg.global.avutil.av_q2d
import org.bytedeco.javacpp.PointerPointer
import java.io.File

data class VideoDetails(val width: Int, val height: Int, val framerate: Double, val duration: Double)

/**
 * Probes a video file by its filename to extract details such as width, height, framerate, and duration.
 *
 * @param filename the name of the video file to be probed
 * @return an instance of [VideoDetails] containing video information, or null if the file does not exist
 */
fun probeVideo(filename: String): VideoDetails? = probeVideo(File(filename))

/**
 * Probes a video file to extract its details such as width, height, framerate, and duration.
 *
 * @param videoFile the video file to be probed
 * @return an instance of [VideoDetails] containing video information, or null if the file does not exist
 */
fun probeVideo(videoFile: File): VideoDetails? {
    if (!videoFile.exists()) {
        return null
    } else {
        val context: AVFormatContext = avformat_alloc_context()
        avformat_open_input(context, videoFile.absolutePath, null, null).checkAVError()
        avformat_find_stream_info(context, null as PointerPointer<*>?).checkAVError()

        val videoStreamIndex =
            av_find_best_stream(context, AVMEDIA_TYPE_VIDEO, -1, -1, null as AVCodec?, 0)

        val audioStreamIndex =
            av_find_best_stream(context, AVMEDIA_TYPE_AUDIO, -1, -1, null as AVCodec?, 0)

        val videoStream = if (videoStreamIndex >= 0) context.streams(videoStreamIndex) else null
        val audioStream = if (audioStreamIndex >= 0) context.streams(audioStreamIndex) else null

        val videoCodecParameters = videoStream?.codecpar()
        val audioCodecParameters = audioStream?.codecpar()

        val width = videoCodecParameters?.width() ?: 0
        val height = videoCodecParameters?.height() ?: 0

        val framerateRational = if (videoStream != null) { av_guess_frame_rate(context, videoStream, null) } else { null}
        val framerate = if (framerateRational != null) { framerateRational.num().toDouble() / framerateRational.den().toDouble() } else { -1.0 }

        val duration = if (videoStream != null) { videoStream.duration() * av_q2d(videoStream?.time_base()) } else -1.0

        avformat_close_input(context)
        avformat_free_context(context)

        return VideoDetails(width, height, framerate, duration)
    }
}