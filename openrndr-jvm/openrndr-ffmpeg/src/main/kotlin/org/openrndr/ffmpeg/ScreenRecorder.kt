package org.openrndr.ffmpeg

import mu.KotlinLogging
import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.draw.*
import java.io.File

private val logger = KotlinLogging.logger {  }

/**
 * ScreenRecorder extension can be used to record to contents of a `Program` to a video
 */
class ScreenRecorder : Extension {
    override var enabled: Boolean = true

    private lateinit var videoWriter: VideoWriter
    private lateinit var frame: RenderTarget
    private var resolved: ColorBuffer? = null
    private var frameIndex: Long = 0
    private var framesWritten: Long = 0

    /**
     * optional width, overrides the program width
     */
    var width: Int? = null

    /**
     * optional height, overrides the program height
     */
    var height: Int? = null

    /** the output file, auto-determined if left null */
    var outputFile: String? = null

    /** the framerate of the output video */
    var frameRate = 30

    /**
     * what time the video recorder should start recording at
     */
    var timeOffset = 0.0

    /**
     * how many frames to skip before starting to record
     */
    var frameSkip = 0L

    /**
     * is video recording enabled or paused?
     */
    var outputToVideo: Boolean = true

    /** the profile to use for the output video */
    var profile: VideoWriterProfile = MP4Profile()

    /** should a frameclock be installed, if false system clock is used */
    var frameClock = true

    /** should multisampling be used? */
    var multisample: BufferMultisample? = null

    /** the maximum duration in frames */
    var maximumFrames = Long.MAX_VALUE

    /** the maximum duration in seconds */
    var maximumDuration = Double.POSITIVE_INFINITY

    /** when set to true, `program.application.exit()` will be issued after the maximum duration has been reached */
    var quitAfterMaximum = true

    var contentScale: Double? = null

    override fun setup(program: Program) {
        if (program.window.resizable) {
            logger.warn { "Resizable windows are not supported, disabling window resizing." }
            program.window.resizable = false
        }

        if (frameClock) {
            program.clock = {
                frameIndex / frameRate.toDouble() + timeOffset
            }
        }

        val requestedWidth = ((width ?: program.width) ).toInt()
        val requestedHeight = ((height ?: program.height) ).toInt()

        if (multisample == null) {
            multisample = program.window.multisample.bufferEquivalent()
        }

        if (contentScale == null) {
            contentScale = program.window.contentScale
        }


        frame = renderTarget(requestedWidth, requestedHeight, multisample = multisample!!, contentScale = contentScale!!) {
            colorBuffer()
            depthBuffer()
        }

        if (multisample != BufferMultisample.Disabled) {
            resolved = colorBuffer(requestedWidth, requestedHeight, contentScale = contentScale!!)
        }

        val filename = if (!outputFile.isNullOrBlank()) outputFile!! else {
            "video/${program.assetMetadata().assetBaseName}.${profile.fileExtension}"
        }

        File(filename).parentFile?.let {
            if (!it.exists()) {
                it.mkdirs()
            }
        }

        videoWriter = VideoWriter().profile(profile).output(filename).size(frame.pixelWidth, frame.pixelHeight).frameRate(frameRate)
    }

    override fun beforeDraw(drawer: Drawer, program: Program) {
        if (frameIndex >= frameSkip) {
            frame.bind()
            program.backgroundColor?.let {
                drawer.clear(it)
            }
        }
    }


    override fun afterDraw(drawer: Drawer, program: Program) {
        if (frameIndex >= frameSkip) {
            frame.unbind()
            if (framesWritten < maximumFrames && framesWritten / frameRate.toDouble() < maximumDuration) {
                val lresolved = resolved
                if (lresolved != null) {
                    frame.colorBuffer(0).copyTo(lresolved)
                    writeFrame(lresolved)
                } else {
                    writeFrame(frame.colorBuffer(0))
                }

                drawer.isolated {
                    drawer.defaults()

                    if (lresolved != null) {
                        drawer.image(lresolved)
                    } else {
                        drawer.image(
                            frame.colorBuffer(0),
                            0.0,
                            0.0,
                            frame.width.toDouble(),
                            frame.height.toDouble()
                        )
                    }
                }
            } else {
                if (quitAfterMaximum) {
                    videoWriter.stop()
                    program.application.exit()
                }
            }
        }
        frameIndex++
    }

    private fun writeFrame(colorBuffer: ColorBuffer) {
        if(outputToVideo) {
            if(!videoWriter.started()) {
                videoWriter.start()
            }
            videoWriter.frame(colorBuffer)
            framesWritten++
        }
    }

    override fun shutdown(program: Program) {
        if(videoWriter.started()) {
            videoWriter.stop()
        }
    }
}