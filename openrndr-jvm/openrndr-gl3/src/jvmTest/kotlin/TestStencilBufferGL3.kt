import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.openrndr.draw.*
import org.openrndr.internal.Driver
import org.openrndr.internal.gl3.DriverGL3
import org.openrndr.internal.gl3.DriverVersionGL
import org.openrndr.shape.Circle
import kotlin.test.*


class TestStencilBufferGL3 : AbstractApplicationTestFixture() {

    @Test
    fun `drawing contour on render target without stencil should throw exception`() {
        val formats = listOf(
            //DepthFormat.DEPTH16,
            DepthFormat.DEPTH24,
            DepthFormat.DEPTH32F)

        for (format in formats) {
            println(format)
            val rt = renderTarget(600, 600) {
                colorBuffer()
                depthBuffer(format = format)
            }

            assertThrows<IllegalStateException> {
                program.drawer.isolatedWithTarget(rt) {
                    shape(Circle(300.0, 300.0, 100.0).shape)
                    contour(Circle(300.0, 300.0, 100.0).contour)
                }
            }
            rt.colorBuffer(0).destroy()
            rt.detachColorAttachments()
            rt.depthBuffer?.destroy()
            rt.detachDepthBuffer()
            rt.destroy()
        }
    }

    @Test
    fun `drawing contour on render target with depth+stencil should pass`() {
        val formats = listOf(DepthFormat.DEPTH_STENCIL, DepthFormat.DEPTH24_STENCIL8, DepthFormat.DEPTH32F_STENCIL8)

        for (format in formats) {
            println(format)
            val rt = renderTarget(600, 600) {
                colorBuffer()
                depthBuffer(format = format)
            }

            assertDoesNotThrow {
                program.drawer.isolatedWithTarget(rt) {
                    shape(Circle(300.0, 300.0, 100.0).shape)
                    contour(Circle(300.0, 300.0, 100.0).contour)
                }
            }
            rt.colorBuffer(0).destroy()
            rt.detachColorAttachments()
            rt.depthBuffer?.destroy()
            rt.detachDepthBuffer()
            rt.destroy()
        }
    }

    @Test
    fun `drawing contour on render target with stencil should pass`() {
        if ((Driver.instance as DriverGL3).version >= DriverVersionGL.VERSION_4_4) {
            val formats = listOf(DepthFormat.STENCIL8)

            for (format in formats) {
                println(format)
                val rt = renderTarget(600, 600) {
                    colorBuffer()
                    depthBuffer(format = format)
                }

                assertDoesNotThrow {
                    program.drawer.isolatedWithTarget(rt) {
                        shape(Circle(300.0, 300.0, 100.0).shape)
                        contour(Circle(300.0, 300.0, 100.0).contour)
                    }
                }
                rt.colorBuffer(0).destroy()
                rt.detachColorAttachments()
                rt.depthBuffer?.destroy()
                rt.detachDepthBuffer()
                rt.destroy()
            }
        } else {
            println("skipping test because opengl version < 4.4")
        }
    }
}