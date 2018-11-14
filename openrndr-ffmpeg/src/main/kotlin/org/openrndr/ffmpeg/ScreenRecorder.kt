package org.openrndr.ffmpeg

import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.draw.*
import org.openrndr.math.Matrix44
import java.time.LocalDateTime

class ScreenRecorder : Extension {
    override var enabled: Boolean = true

    private lateinit var videoWriter: VideoWriter
    private lateinit var frame: RenderTarget

    var frameRate = 30
    var profile = MP4Profile()
    var frameClock = true
    var multisample:BufferMultisample = BufferMultisample.DISABLED
    var resolved: ColorBuffer? = null
    var maximumFrames = Long.MAX_VALUE
    var maximumDuration = Double.POSITIVE_INFINITY
    var quitAfterMaximum = true

    private var frameIndex: Long = 0
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

        println(frame.colorBuffers.size)

        if (multisample != BufferMultisample.DISABLED) {
            resolved = colorBuffer(program.width, program.height)
        }

        val dt = LocalDateTime.now()
        val basename = program.javaClass.simpleName.ifBlank { program.window.title.ifBlank { "untitled" } }
        val filename = "$basename-${dt.year.z(4)}-${dt.month.value.z()}-${dt.dayOfMonth.z()}-${dt.hour.z()}.${dt.minute.z()}.${dt.second.z()}.mp4"
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
        if (frameIndex < maximumFrames && frameIndex / frameRate.toDouble() < maximumDuration ) {

            val lr = resolved
            if (lr != null) {
                frame.colorBuffer(0).resolveTo(lr)
                videoWriter.frame(lr)
            } else {
                videoWriter.frame(frame.colorBuffer(0))
            }



            drawer.isolated {
                drawer.shadeStyle = null
                drawer.ortho()
                drawer.model = Matrix44.IDENTITY
                drawer.view = Matrix44.IDENTITY

                if (lr != null) {
                    drawer.image(lr)
                } else {
                    drawer.image(frame.colorBuffer(0))
                }
            }
        } else {
            if (quitAfterMaximum) {
                program.application.exit()
            }
        }
        frameIndex++
    }
}