package org.openrndr.ffmpeg

import org.bytedeco.ffmpeg.avcodec.AVPacket
import org.bytedeco.ffmpeg.avformat.AVFormatContext
import org.bytedeco.ffmpeg.global.avcodec
import org.bytedeco.ffmpeg.global.avformat

internal class PacketReader(val formatContext: AVFormatContext, val statistics: VideoStatistics) {

    val queue = Queue<AVPacket>(2000)
    var disposed = false

    var ready = true
    fun start() {
        while (!disposed) {

            if (queue.size() > 500) {
                ready = true
            }

            if (queue.size() < 1990) {
                val packet = avcodec.av_packet_alloc()
                val res = avformat.av_read_frame(formatContext, packet)

                if (res == 0) {
                    queue.push(packet)
                    statistics.packetQueueSize = queue.size()
                }
            } else {
                Thread.sleep(1)
            }
        }
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
        disposed = true
    }

    fun flushQueue() {
        while (!queue.isEmpty())  {
            val packet = queue.pop()
            avcodec.av_packet_unref(packet)
        }
    }

}