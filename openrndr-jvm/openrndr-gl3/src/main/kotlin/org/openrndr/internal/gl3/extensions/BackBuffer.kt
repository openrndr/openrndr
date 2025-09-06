package org.openrndr.internal.gl3.extensions

import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.draw.*

class BackBuffer : Extension {
    override var enabled = true

    val shadeStyle = shadeStyle {
        fragmentTransform = """const float t = 0.00313066844250063;
vec3 del = vec3(
    x_fill.r <= t ? x_fill.r * 12.92 : 1.055 * pow(x_fill.r, 1.0 / 2.4) - 0.055,
    x_fill.g <= t ? x_fill.g * 12.92 : 1.055 * pow(x_fill.g, 1.0 / 2.4) - 0.055,
    x_fill.b <= t ? x_fill.b * 12.92 : 1.055 * pow(x_fill.b, 1.0 / 2.4) - 0.055
);
x_fill.rgb = del;""".trimIndent()
    }

    val rt = run {
        val art = RenderTarget.active
        resizableRenderTarget(art.width, art.height, art.contentScale, art.multisample) {
            colorBuffer(type = ColorType.UINT8)
            depthBuffer()
        }
    }
    override fun beforeDraw(drawer: Drawer, program: Program) {
        rt.resize(RenderTarget.active)
        rt.renderTarget.bind()
        val bc = program.backgroundColor
        if (bc != null)
        drawer.clear(bc)
        super.beforeDraw(drawer, program)
    }

    override fun afterDraw(drawer: Drawer, program: Program) {
        rt.renderTarget.unbind()
        drawer.isolated {
            drawer.defaults()
            drawer.shadeStyle = this@BackBuffer.shadeStyle
            drawer.image(rt.renderTarget.colorBuffer(0))
        }
    }
}