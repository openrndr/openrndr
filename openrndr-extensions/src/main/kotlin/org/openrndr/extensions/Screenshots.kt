package org.openrndr.extensions

import org.openrndr.Extension
import org.openrndr.KEY_SPACEBAR
import org.openrndr.Program
import org.openrndr.draw.Drawer
import org.openrndr.draw.RenderTarget
import org.openrndr.draw.renderTarget
import org.openrndr.math.Matrix44
import java.io.File
import java.time.LocalDateTime

class Screenshots: Extension {
    override var enabled: Boolean = true

    var scale = 1.0
    private var createScreenshot = false

    var target: RenderTarget? = null


    override fun setup(program: Program) {
        program.keyboard.keyDown.listen {
            if (it.key == KEY_SPACEBAR) {
                createScreenshot = true
            }
        }
    }

    override fun beforeDraw(drawer: Drawer, program: Program) {
        if (createScreenshot) {
            target = renderTarget((program.width*scale).toInt(), (program.height*scale).toInt()) {
                colorBuffer()
                depthBuffer()
            }
            target?.bind()
            program.backgroundColor?.let {
                drawer.background(it)
            }
        }
    }

    override fun afterDraw(drawer: Drawer, program: Program) {
        fun Int.z(zeroes: Int = 2): String {
            val sv = this.toString()
            var prefix = ""
            for (i in 0 until Math.max(zeroes - sv.length, 0)) {
                prefix += "0"
            }
            return "$prefix$sv"
        }
        if (createScreenshot) {
            drawer.shadeStyle = null
            target?.let {
                it.unbind()
            }
            target?.let {
                drawer.view = Matrix44.IDENTITY
                drawer.model = Matrix44.IDENTITY
                drawer.image(it.colorBuffer(0))
                val dt = LocalDateTime.now()
                it.colorBuffer(0).saveToFile(File("${program.javaClass.simpleName}-${dt.year.z(4)}-${dt.month.value.z()}-${dt.dayOfMonth.z()}-${dt.hour.z()}.${dt.minute.z()}.${dt.second.z()}.png"))
            }
            target?.destroy()
            createScreenshot = false
        }
    }
}