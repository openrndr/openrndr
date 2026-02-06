import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.internal.Driver
import org.openrndr.math.Vector3
import kotlin.math.cos

fun main() {
    application {
        program {

            val vb = vertexBuffer(vertexFormat { position(3) }, 6)
            vb.put {
                // triangle 1
                write(Vector3(0.0, 0.0, 0.0))
                write(Vector3(0.0, 100.0, 0.0))
                write(Vector3(100.0, 100.0, 0.0))

                // triangle 2
                write(Vector3(200.0, 0.0, 0.0))
                write(Vector3(200.0, 100.0, 0.0))
                write(Vector3(300.0, 100.0, 0.0))
            }


            val colors = vertexBuffer(vertexFormat { color(4) }, 4)

            colors.put {
                write(ColorRGBa.PINK)
                write(ColorRGBa.RED)
                write(ColorRGBa.GREEN)
                write(ColorRGBa.BLUE)
            }

            val cb = Driver.instance.createCommandBuffer(2u)
            cb.write(listOf(
                // draw first triangle
                command(3u),
                // draw second triangle, instance 3 times, set baseInstance to 1
                command(3u, baseVertex = 3, baseInstance = 1u, instanceCount = 3u))
            )


            /*
            val ib = indexBuffer(6, IndexType.INT32)
            val bb = ByteBuffer.allocateDirect(6*4)
            bb.order(ByteOrder.nativeOrder())

            for (i in 0 until 6) {
                bb.putInt(i.toInt())
            }
            bb.rewind()
            ib.write(bb)
            */

            Thread.sleep(1000)
            extend {

                drawer.clear(ColorRGBa.PINK.shade(cos(seconds)*0.5+0.5))

//                drawer.translate(cos(seconds) * 100.0, 0.0)
//                drawer.circle(width / 2.0, height / 2.0, 100.0)
//                drawer.vertexBuffer(ib, listOf(vb), DrawPrimitive.TRIANGLES, 0 , 3)
                drawer.shadeStyle = shadeStyle {
                    vertexTransform = """
                       float fi = float(gl_InstanceID);
                       x_position.xy += vec2(0.0, 100.0 * fi);
                       
                    """

                    fragmentTransform = """
                       x_fill = vi_color; 
                    """.trimIndent()
                }
                drawer.vertexBufferCommands(listOf(vb), listOf(colors), DrawPrimitive.TRIANGLES, cb, 2, 0)
            }
        }
    }
}