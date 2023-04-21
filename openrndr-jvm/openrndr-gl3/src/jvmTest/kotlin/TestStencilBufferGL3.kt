import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.openrndr.draw.*
import org.openrndr.internal.Driver
import org.openrndr.internal.gl3.DriverGL3
import org.openrndr.internal.gl3.DriverVersionGL
import org.openrndr.internal.gl3.checkGLErrors
import org.openrndr.shape.Circle
import kotlin.test.*


class TestStencilBufferGL3 : AbstractApplicationTestFixture() {

    @Test
    fun `drawing contour on render target without stencil should throw exception`() {
        val formats = listOf(
            //DepthFormat.DEPTH16,
            DepthFormat.DEPTH24,
            DepthFormat.DEPTH32F
        )

        for (format in formats) {
            checkGLErrors { "$format" }
            val rt = renderTarget(600, 600) {
                colorBuffer()
                checkGLErrors { "$format" }
                depthBuffer(format = format)
                checkGLErrors { "$format" }
            }

            assertThrows<IllegalStateException> {
                program.drawer.isolatedWithTarget(rt) {
                    shape(Circle(300.0, 300.0, 100.0).shape)
                    checkGLErrors { "$format" }
                    contour(Circle(300.0, 300.0, 100.0).contour)
                    checkGLErrors { "$format" }
                }
            }
            rt.colorBuffer(0).destroy()
            checkGLErrors { "$format" }
            rt.detachColorAttachments()
            checkGLErrors { "$format" }
            rt.depthBuffer?.destroy()
            checkGLErrors { "$format" }
            rt.detachDepthBuffer()
            checkGLErrors { "$format" }
            rt.destroy()
            checkGLErrors { "$format" }
        }
    }

    @Test
    fun `drawing contour on render target with depth+stencil should pass`() {
        val formats = listOf(DepthFormat.DEPTH_STENCIL, DepthFormat.DEPTH24_STENCIL8, DepthFormat.DEPTH32F_STENCIL8)

        for (format in formats) {
            println(format)
            val rt = renderTarget(600, 600) {
                colorBuffer()
                checkGLErrors()
                depthBuffer(format = format)
                checkGLErrors()
            }

            assertDoesNotThrow {
                program.drawer.isolatedWithTarget(rt) {
                    shape(Circle(300.0, 300.0, 100.0).shape)
                    checkGLErrors()
                    contour(Circle(300.0, 300.0, 100.0).contour)
                    checkGLErrors()
                }
            }
            rt.colorBuffer(0).destroy()
            rt.detachColorAttachments()
            rt.depthBuffer?.destroy()
            rt.detachDepthBuffer()
            rt.destroy()
            checkGLErrors()
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
                    checkGLErrors()
                    depthBuffer(format = format)
                    checkGLErrors()
                }

                assertDoesNotThrow {
                    program.drawer.isolatedWithTarget(rt) {
                        shape(Circle(300.0, 300.0, 100.0).shape)
                        checkGLErrors()
                        contour(Circle(300.0, 300.0, 100.0).contour)
                        checkGLErrors()
                    }
                }
                rt.colorBuffer(0).destroy()
                rt.detachColorAttachments()
                rt.depthBuffer?.destroy()
                rt.detachDepthBuffer()
                rt.destroy()
                checkGLErrors()
            }
        } else {
            println("skipping test because opengl version < 4.4")
        }
    }
}