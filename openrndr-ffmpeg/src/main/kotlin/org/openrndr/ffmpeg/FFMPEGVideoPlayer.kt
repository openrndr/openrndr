package org.openrndr.ffmpeg

import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.Drawer
import org.openrndr.ffmpeg.adopted.FFmpegFrameGrabber
import org.openrndr.internal.gl3.ColorBufferGL3
import java.nio.ByteBuffer

class FFMPEGVideoPlayer(url:String) {

    companion object {
        fun fromURL(url:String):FFMPEGVideoPlayer {
            return FFMPEGVideoPlayer(url)

        }
    }

    internal var frameGrabber = FFmpegFrameGrabber(url)

    var colorBuffer:ColorBuffer? = null

    fun start() {
        frameGrabber.start()
    }

    fun next() {
        frameGrabber.grab()
        val frame = frameGrabber.grabImage()

        if (frame != null) {
            if (colorBuffer == null && frame.imageWidth > 0 && frame.imageHeight > 0) {
                println("creating texture ${frame.imageWidth}x${frame.imageHeight}")
                colorBuffer = ColorBuffer.create(frame.imageWidth, frame.imageHeight, ColorFormat.RGB).apply {
                    flipV = true
                }

            }
            colorBuffer?.let {
                val cb = colorBuffer as ColorBufferGL3

                cb.write(frame.image[0] as ByteBuffer)
            }
        } else {
     //       println("null")
        }
    }

    fun draw(drawer: Drawer) {
        colorBuffer?.let {
            drawer.image(it)
        }
    }
}