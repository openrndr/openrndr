package org.openrndr.ffmpeg

import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.draw.*
import org.openrndr.math.Matrix44
import java.io.File
import java.time.LocalDateTime

/**
 * ScreenRecorder extension can be used to record to contents of a `Program` to a video
 */
class ScreenRecorder : Extension {
    override var enabled: Boolean = true

    private lateinit var videoWriter: VideoWriter
    private lateinit var frame: RenderTarget
    private var resolved: ColorBuffer? = null
    private var frameIndex: Long = 0

    /** the output file, auto-determined if left null */
    var outputFile: String? = null

    /** the framerate of the output video */
    var frameRate = 30

    /** the profile to use for the output video */
    var profile = MP4Profile()

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

    override fun setup(program: Program) {
        if (frameClock) {
            program.clock = {
                frameIndex / frameRate.toDouble()
            }
        }

        fun Int.z(zeroes: Int = 2): String {
            val sv = this.toString()
            var prefix = ""
            for (i in 0 until Math.max(zeroes - sv.length, 0)) {
                prefix += "0"
            }
            return "$prefix$sv"
        }

        frame = renderTarget(program.width, program.height, multisample = multisample) {
            colorBuffer()
            depthBuffer()
        }

        if (multisample != BufferMultisample.Disabled) {
            resolved = colorBuffer(program.width, program.height)
        }

        val dt = LocalDateTime.now()
        val basename = program.javaClass.simpleName.ifBlank { program.window.title.ifBlank { "untitled" } }
        val filename = outputFile?: "$basename-${dt.year.z(4)}-${dt.month.value.z()}-${dt.dayOfMonth.z()}-${dt.hour.z()}.${dt.minute.z()}.${dt.second.z()}.mp4"

        File(filename).parentFile.let {
            if (it.exists()) {
                it.mkdirs()
            }
        }

        videoWriter = VideoWriter().profile(profile).output(filename).size(program.width, program.height).frameRate(frameRate).start()
    }

    override fun beforeDraw(drawer: Drawer, program: Program) {
        frame.bind()
        program.backgroundColor?.let {
            drawer.background(it)
        }
    }

    override fun afterDraw(drawer: Drawer, program: Program) {
        frame.unbind()
        if (frameIndex < maximumFrames && frameIndex / frameRate.toDouble() < maximumDuration) {
            val lresolved = resolved
            if (lresolved != null) {
                frame.colorBuffer(0).resolveTo(lresolved)
                videoWriter.frame(lresolved)
            } else {
                videoWriter.frame(frame.colorBuffer(0))
            }

            drawer.isolated {
                drawer.shadeStyle = null
                drawer.ortho()
                drawer.model = Matrix44.IDENTITY
                drawer.view = Matrix44.IDENTITY

                if (lresolved != null) {
                    drawer.image(lresolved)
                } else {
                    drawer.image(frame.colorBuffer(0))
                }
            }
        } else {
            if (quitAfterMaximum) {
                videoWriter.stop()
                program.application.exit()
            }
        }
        frameIndex++
    }
}