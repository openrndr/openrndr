package org.openrndr.ffmpeg

import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.draw.*
import java.io.File

/**
 * ScreenRecorder extension can be used to record to contents of a `Program` to a video
 */
class ScreenRecorder : Extension {
    override var enabled: Boolean = true

    private lateinit var videoWriter: VideoWriter
    private lateinit var frame: RenderTarget
    private var resolved: ColorBuffer? = null
    private var frameIndex: Long = 0

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

    /** the profile to use for the output video */
    var profile: VideoWriterProfile = MP4Profile()

    /** should a frameclock be installed, if false system clock is used */
    var frameClock = true

    /** should multisampling be used? */
    var multisample: BufferMultisample = BufferMultisample.Disabled

    /** the maximum duration in frames */
    var maximumFrames = Long.MAX_VALUE

    /** the maximum duration in seconds */
    var maximumDuration = Double.POSITIVE_INFINITY

    /** when set to true, `program.application.exit()` will be issued after the maximum duration has been reached */
    var quitAfterMaximum = true

    var contentScale: Double = 1.0


    override fun setup(program: Program) {
        if (frameClock) {
            program.clock = {
                frameIndex / frameRate.toDouble() + timeOffset
            }
        }

        val effectiveWidth = ((width ?: program.width) * contentScale).toInt()
        val effectiveHeight = ((height ?: program.height) * contentScale).toInt()

        frame = renderTarget(effectiveWidth, effectiveHeight, multisample = multisample) {
            colorBuffer()
            depthBuffer()
        }

        if (multisample != BufferMultisample.Disabled) {
            resolved = colorBuffer(effectiveWidth, effectiveHeight)
        }

        val filename = if (!outputFile.isNullOrBlank()) outputFile!! else {
            "video/${program.assetMetadata().assetBaseName}.${profile.fileExtension}"
        }

        File(filename).parentFile?.let {
            if (!it.exists()) {
                it.mkdirs()
            }
        }

        videoWriter = VideoWriter().profile(profile).output(filename).size(effectiveWidth, effectiveHeight).frameRate(frameRate)
    }

    override fun beforeDraw(drawer: Drawer, program: Program) {
        if (frameIndex == frameSkip){
            videoWriter.start()
        }
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
            if (frameIndex < maximumFrames + frameSkip && (frameIndex - frameSkip) / frameRate.toDouble() < maximumDuration) {
                val lresolved = resolved
                if (lresolved != null) {
                    frame.colorBuffer(0).copyTo(lresolved)
                    videoWriter.frame(lresolved)
                } else {
                    videoWriter.frame(frame.colorBuffer(0))
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
                            frame.width / contentScale,
                            frame.height / contentScale
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

    override fun shutdown(program: Program) {
        videoWriter.stop()
    }
}