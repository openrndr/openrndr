package org.openrndr.ffmpeg

import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.Drawer
import org.openrndr.ffmpeg.adopted.FFmpegFrameGrabber
import org.openrndr.internal.gl3.ColorBufferGL3
import java.io.File
import java.nio.ByteBuffer

class FFMPEGVideoPlayer private constructor(url: String) {
    companion object {
        fun fromURL(url: String): FFMPEGVideoPlayer {
            return FFMPEGVideoPlayer(url)
        }

        fun fromFile(filename: String): FFMPEGVideoPlayer {
            return FFMPEGVideoPlayer(File(filename).toURI().toURL().toExternalForm())
        }

        fun listDevices():List<String> {
            return FFmpegFrameGrabber.getDeviceDescriptions().toList()
        }

        fun fromDevice(deviceName: String): FFMPEGVideoPlayer {
            val osName = System.getProperty("os.name").toLowerCase()
            val format: String
            format = when {
                "windows" in osName -> {
                    "dshow"
                }
                "mac os x" in osName -> {
                    "avfoundation"
                }
                "linux" in osName -> {
                    "video4linux"
                }
                else -> throw RuntimeException("unsupported os: $osName")
            }

            val player = FFMPEGVideoPlayer(deviceName)
            player.frameGrabber.format = format
            return player
        }
    }

    internal var frameGrabber = FFmpegFrameGrabber(url)
    private var colorBuffer: ColorBuffer? = null

    fun start() {
        frameGrabber.start()
    }


    fun next() {
        val frame = frameGrabber.grabImage()
        if (frame != null) {
            if (colorBuffer == null && frame.imageWidth > 0 && frame.imageHeight > 0) {
                colorBuffer = ColorBuffer.create(frame.imageWidth, frame.imageHeight, format = ColorFormat.RGB).apply {
                    flipV = true
                }
            }
            colorBuffer?.let {
                val cb = colorBuffer as ColorBufferGL3
                cb.write(frame.image[0] as ByteBuffer)
            }
        }
    }

    fun draw(drawer: Drawer) {
        colorBuffer?.let {
            drawer.image(it)
        }
    }
}