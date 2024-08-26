import org.openrndr.draw.*
import org.openrndr.draw.font.BufferAccess
import org.openrndr.draw.font.BufferFlag
import org.openrndr.internal.Driver
import org.openrndr.internal.gl3.*
import org.openrndr.math.IntVector2
import org.openrndr.math.IntVector3
import org.openrndr.math.IntVector4
import kotlin.test.Test

class TestComputeStyleGL3 : AbstractApplicationTestFixture() {
    @Test
    fun test() {
        if (Driver.capabilities.compute) {
            val cs = computeStyle {
                computeTransform = ""
            }
            cs.execute(1, 1, 1)
        }
    }

    @Test
    fun testImageBinding() {
        val img = colorBuffer(256, 256, type = ColorType.UINT8)
        if (Driver.capabilities.compute) {
            val cs = computeStyle {
                computeTransform = "p_img;"
                registerImageBinding(
                    "img",
                    BufferAccess.READ,
                    setOf(BufferFlag.COHERENT, BufferFlag.RESTRICT)
                )
                image("img", img, 0)
            }
            cs.execute(1, 1, 1)
        }
        img.destroy()
    }

    @Test
    fun testVolumeImageBinding() {
        if (Driver.capabilities.compute) {
            val img = volumeTexture(16, 16, 16, type = ColorType.UINT8)
            val cs = computeStyle {
                computeTransform = "p_img; p_imgArray[0];"
                registerImageBinding(
                    "img",
                    BufferAccess.READ,
                    setOf(BufferFlag.COHERENT, BufferFlag.RESTRICT)
                )
                registerImageBinding("imgArray", BufferAccess.READ, setOf(BufferFlag.COHERENT), 3)
                image("img", img, 0)
                image("imgArray", arrayOf(img, img, img), arrayOf(0, 0, 0))
            }
            cs.execute(1, 1, 1)
            img.destroy()
        }

    }

    @Test
    fun testIntVectors() {
        if (Driver.capabilities.compute) {
            val cs = computeStyle {
                computeTransform = "p_ivec2; p_ivec3; p_ivec4;"
                parameter("ivec2", IntVector2.ZERO)
                parameter("ivec3", IntVector3.ZERO)
                parameter("ivec4", IntVector4.ZERO)
            }
            cs.execute(1, 1, 1)
        }
    }
}