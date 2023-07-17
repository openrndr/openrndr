import org.openrndr.draw.colorBuffer
import org.openrndr.draw.computeStyle
import org.openrndr.draw.execute
import org.openrndr.draw.font.BufferAccess
import org.openrndr.draw.font.BufferFlag
import org.openrndr.internal.Driver
import org.openrndr.internal.gl3.DriverGL3
import org.openrndr.internal.gl3.DriverVersionGL
import kotlin.test.Test

class TestComputeStyleGL3 : AbstractApplicationTestFixture() {
    @Test
    fun test() {
        if ((Driver.instance as DriverGL3).version >= DriverVersionGL.VERSION_4_3) {
            val cs = computeStyle {
                computeTransform = ""
            }
            cs.execute(1, 1, 1)
        }
    }

    @Test
    fun testImageBinding() {
        val img = colorBuffer(256, 256)
        if ((Driver.instance as DriverGL3).version >= DriverVersionGL.VERSION_4_3) {
            val cs = computeStyle {
                computeTransform = "p_img;"
                registerImageBinding("img", BufferAccess.READ_WRITE, setOf(BufferFlag.COHERENT, BufferFlag.RESTRICT))
                image("img", img, 0)
            }
            cs.execute(1, 1, 1)
        }
    }
}