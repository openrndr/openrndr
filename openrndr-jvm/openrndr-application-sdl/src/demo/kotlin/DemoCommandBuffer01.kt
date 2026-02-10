import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.IndexType
import org.openrndr.draw.command
import org.openrndr.draw.indexBuffer
import org.openrndr.draw.shadeStyle
import org.openrndr.draw.vertexBuffer
import org.openrndr.draw.vertexFormat
import org.openrndr.internal.Driver
import org.openrndr.math.Vector3
import java.nio.ByteBuffer
import java.nio.ByteOrder
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
            val ib = indexBuffer(6, IndexType.INT32)
            val bb = ByteBuffer.allocateDirect(6*4)
            bb.order(ByteOrder.nativeOrder())

            for (i in 0 until 6) {
                bb.putInt(i.toInt())
            }
            bb.rewind()
            ib.write(bb)


            Thread.sleep(1000)
            extend {

                drawer.clear(ColorRGBa.PINK.shade(cos(seconds)*0.5+0.5))

//                drawer.translate(cos(seconds) * 100.0, 0.0)
//                drawer.circle(width / 2.0, height / 2.0, 100.0)
//                drawer.vertexBuffer(ib, listOf(vb), DrawPrimitive.TRIANGLES, 0 , 3)
//                drawer.shadeStyle = shadeStyle {
//                    vertexPreamble = """"""
//                    vertexTransform = """
//                       float f = float(gl_DrawID);
//                    """
//                }
                drawer.vertexBufferCommands(listOf(vb), emptyList(), DrawPrimitive.TRIANGLES, cb, 2, 0)
            }
        }
    }
}