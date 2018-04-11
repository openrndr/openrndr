package org.openrndr.ffmpeg

import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.draw.Drawer
import org.openrndr.draw.RenderTarget
import org.openrndr.draw.isolated
import org.openrndr.draw.renderTarget
import org.openrndr.math.Matrix44
import java.time.LocalDateTime

class ScreenRecorder:Extension {

    override var enabled: Boolean = true

    lateinit var videoWriter: VideoWriter
    lateinit var frame: RenderTarget
    override fun setup(program: Program) {

        fun Int.z(zeroes:Int=2):String {
            val sv = this.toString()
            var prefix = ""
            for (i in 0 until Math.max(zeroes-sv.length, 0)) {
                prefix += "0"
            }
            return "$prefix$sv"
        }

        frame = renderTarget(program.width, program.height) {
            colorBuffer()
            depthBuffer()
        }
        val dt = LocalDateTime.now()
        videoWriter = VideoWriter().output("${program.javaClass.simpleName}-${dt.year.z(4)}-${dt.month.value.z()}-${dt.dayOfMonth.z()}-${dt.hour.z()}.${dt.minute.z()}.${dt.second.z()}.mp4").size(program.width, program.height).frameRate(30).start()
    }

    override fun beforeDraw(drawer: Drawer, program: Program) {
        frame.bind()
        program.backgroundColor?.let {
            drawer.background(it)
        }
    }

    override fun afterDraw(drawer: Drawer, program: Program) {
        frame.unbind()
        videoWriter.frame(frame.colorBuffer(0))
        drawer.isolated {
            drawer.ortho()
            drawer.model = Matrix44.IDENTITY
            drawer.view = Matrix44.IDENTITY
            drawer.image(frame.colorBuffer(0))
        }
    }
}