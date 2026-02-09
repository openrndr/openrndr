import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.command
import org.openrndr.draw.shadeStyle
import org.openrndr.draw.vertexBuffer
import org.openrndr.draw.vertexFormat
import org.openrndr.internal.Driver
import org.openrndr.math.Vector3
import kotlin.math.cos

fun main() {
    application {
        program {
            val cb = Driver.instance.createCommandBuffer(2u)
            cb.write(listOf(
                command(3u),
                command(3u, baseVertex = 3, baseInstance = 0u, instanceCount = 3u)))

            val vb = vertexBuffer(vertexFormat { position(3) }, 6)
            vb.put {
                write(Vector3(0.0, 0.0, 0.0))
                write(Vector3(0.0, 100.0, 0.0))
                write(Vector3(100.0, 100.0, 0.0))

                write(Vector3(200.0, 0.0, 0.0))
                write(Vector3(200.0, 100.0, 0.0))
                write(Vector3(300.0, 100.0, 0.0))
            }
            extend {
                drawer.clear(ColorRGBa.PINK.shade(cos(seconds)*0.5+0.5))
                drawer.translate(cos(seconds) * 100.0, 0.0)
                drawer.circle(width / 2.0, height / 2.0, 100.0)
//                drawer.vertexBuffer(vb, DrawPrimitive.TRIANGLES)
                drawer.shadeStyle = shadeStyle {
                    vertexPreamble = """"""
                    vertexTransform = """
                       float f = float(gl_DrawID); 
                    """
                }
                drawer.vertexBufferCommands(listOf(vb), emptyList(), DrawPrimitive.TRIANGLES, cb, 2, 0)
//                Driver.instance.finish()
            }
        }
    }
}