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
    lateinit var videoWriter: VideoWriter
    lateinit var frame: RenderTarget
    override fun setup(program: Program) {
        frame = renderTarget(program.width, program.height) {
            colorBuffer()
            depthBuffer()
        }
        val dt = LocalDateTime.now()
        videoWriter = VideoWriter().output("${program.javaClass.simpleName}-${dt.year}-${dt.month.value}-${dt.dayOfMonth}-${dt.hour}-${dt.minute}-${dt.second}.mp4").size(program.width, program.height).frameRate(30).start()
    }

    override fun beforeDraw(drawer: Drawer, program: Program) {
        frame.bind()
    }

    override fun afterDraw(drawer: Drawer, program: Program) {
        frame.unbind()
        videoWriter.frame(frame.colorBuffer(0))
        drawer.isolated {
            drawer.ortho()
            drawer.view = Matrix44.IDENTITY
            drawer.image(frame.colorBuffer(0))
        }
    }
}