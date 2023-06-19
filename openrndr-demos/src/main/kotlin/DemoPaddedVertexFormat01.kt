import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.shadeStyle
import org.openrndr.draw.vertexBuffer
import org.openrndr.draw.vertexFormat
import org.openrndr.math.Vector3

fun main() = application {
    program {
        val vb = vertexBuffer(vertexFormat {
            padding(1)
            position(3)
            padding(4)
            color(4)
        }, 3)

        vb.put {
            write(0.toByte())
            write(Vector3(10.0, 10.0, 0.0))
            write(0.toByte())
            write(0.toByte())
            write(0.toByte())
            write(0.toByte())
            write(ColorRGBa.RED)

            write(0.toByte())
            write(Vector3(100.0, 10.0, 0.0))
            write(0.toByte())
            write(0.toByte())
            write(0.toByte())
            write(0.toByte())
            write(ColorRGBa.BLUE)

            write(0.toByte())
            write(Vector3(100.0, 100.0, 0.0))
            write(0.toByte())
            write(0.toByte())
            write(0.toByte())
            write(0.toByte())
            write(ColorRGBa.GREEN)
        }
        extend {

            drawer.shadeStyle = shadeStyle {
                fragmentTransform = """
                   x_fill = va_color; 
                """
            }
            drawer.vertexBuffer(vb, DrawPrimitive.TRIANGLES)
        }
    }
}