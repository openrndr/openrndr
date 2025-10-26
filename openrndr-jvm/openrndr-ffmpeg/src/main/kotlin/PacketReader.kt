package org.openrndr.ffmpeg

import io.github.oshai.kotlinlogging.KotlinLogging
import org.bytedeco.ffmpeg.avcodec.AVPacket
import org.bytedeco.ffmpeg.avformat.AVFormatContext
import org.bytedeco.ffmpeg.global.avcodec
import org.bytedeco.ffmpeg.global.avformat
import org.bytedeco.ffmpeg.global.avutil.AVERROR_EOF

private val logger = KotlinLogging.logger {  }

internal class PacketReader(private val configuration: VideoPlayerConfiguration,
                            private val formatContext: AVFormatContext,
                            private val statistics: VideoStatistics
) {

    val queue = Queue<AVPacket>(configuration.packetQueueSize * 2)
    var disposed = false


    var endOfFile = false
    var ready = true
    fun isQueueEmpty():Boolean {
        return queue.size() == 0
    }

    fun start() {
        while (!disposed) {

            if (queue.size() < configuration.packetQueueSize) {
                if (!endOfFile) {
                    val packet = avcodec.av_packet_alloc()
                    val res = avformat.av_read_frame(formatContext, packet)

                    if (res == 0) {
                        queue.push(packet)
                        statistics.packetQueueSize = queue.size()
                    } else {
                        logger.error { "no packet (error)  ${queue.size()}" }
                        avcodec.av_packet_free(packet)
                        if (res == AVERROR_EOF) {
                            logger.debug  { "packet reader; end of file" }
                            endOfFile = true
                        }
                    }
                } else {
                    Thread.sleep(5)
                }
            } else {
                logger.warn { "queue full" }
                Thread.sleep(500)
            }
        }
        logger.warn { "packet reader ended" }
    }

    fun nextPacket(): AVPacket? {
        if (queue.size() > 0)
            statistics.packetQueueSize = queue.size() - 1
        else {
            statistics.packetQueueSize = 0
        }
        return if (ready) queue.popOrNull() else null
    }

    fun dispose() {
        while (!queue.isEmpty())  {
            val packet = queue.pop()
            avcodec.av_packet_free(packet)
        }
        disposed = true
    }

    fun flushQueue() {
        while (!queue.isEmpty())  {
            val packet = queue.pop()
            avcodec.av_packet_free(packet)
        }
        queue.push(flushPacket)
        endOfFile = false

        logger.debug { "flushed reader queue" }
    }
}