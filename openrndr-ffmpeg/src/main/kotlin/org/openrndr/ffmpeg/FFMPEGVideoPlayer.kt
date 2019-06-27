package org.openrndr.ffmpeg

import org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_NONE
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.Drawer
import org.openrndr.draw.Session
import org.openrndr.ffmpeg.adopted.FFmpegFrameGrabber
import org.openrndr.draw.colorBuffer as _colorBuffer
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


        fun defaultDevice(): String {
            val osName = System.getProperty("os.name").toLowerCase()
            val device: String
            device = when {
                "windows" in osName -> {
                    "video=Integrated Webcam"
                }
                "mac os x" in osName -> {
                    "0"
                }
                "linux" in osName -> {
                    "/dev/video0"
                }
                else -> throw RuntimeException("unsupported video platform")
            }
            return device
        }

        fun defaultInputFormat(): String? {
            val osName = System.getProperty("os.name").toLowerCase()
            val format: String?
            format = when {
                "windows" in osName -> {
                    null
                }
                "mac os x" in osName -> {
                    null
                }
                "linux" in osName -> {
                    "mjpeg"
                }
                else -> throw RuntimeException("unsupported os: $osName")
            }
            return format
        }

        fun fromDevice(deviceName: String = defaultDevice(), width: Int = -1, height: Int = -1, framerate: Double = -1.0, inputFormat: String? = defaultInputFormat()): FFMPEGVideoPlayer {
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
                    "video4linux2"
                }
                else -> throw RuntimeException("unsupported os: $osName")
            }

            val player = FFMPEGVideoPlayer(deviceName)
            player.frameGrabber.inputFormat = inputFormat
            player.frameGrabber.pixelFormat =  AV_PIX_FMT_NONE
            player.frameGrabber.format = format

            player.frameGrabber.numBuffers = 1
            if (width != -1 && height != -1) {
                player.frameGrabber.imageWidth = width
                player.frameGrabber.imageHeight = height
            }
            if (framerate != -1.0) {
                player.frameGrabber.frameRate = framerate
            }
            return player
        }
    }

    internal var frameGrabber = FFmpegFrameGrabber(url)
    private var colorBuffer: ColorBuffer? = null

    val width
        get() = colorBuffer?.width ?: 0

    val height
        get() = colorBuffer?.height ?: 0

    fun start() {
        frameGrabber.start()
    }

    fun next() {
        val frame = frameGrabber.grabImage()
        if (frame != null) {
            if (colorBuffer == null && frame.imageWidth > 0 && frame.imageHeight > 0) {
                colorBuffer = _colorBuffer(frame.imageWidth, frame.imageHeight, format = ColorFormat.RGB).apply {
                    flipV = true
                }
                val cb = colorBuffer
                if (cb != null)
                    Session.active.untrack(cb)
            }
            colorBuffer?.let {
                val cb = it
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