import org.junit.jupiter.api.assertThrows
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.internal.Driver
import org.openrndr.internal.gl3.DriverTypeGL
import org.openrndr.internal.gl3.glType
import kotlin.test.Test

class TestRenderTargetGL3 : AbstractApplicationTestFixture() {

    @Test
    fun testRenderTargetRGB() {

        if (Driver.glType == DriverTypeGL.GL) {
            val rt = renderTarget(640, 480) {
                colorBuffer(format = ColorFormat.RGB)
            }

            rt.close()
        } else {
            assertThrows<IllegalArgumentException> {
                renderTarget(640, 480) {
                    colorBuffer(format = ColorFormat.RGB)
                }
            }
        }
    }

    @Test
    fun testRenderTargetMostCommon() {
        val rt = renderTarget(640, 480) {
            colorBuffer()
            depthBuffer()
        }
        program.drawer.withTarget(rt) {
            clear(ColorRGBa.PINK)
        }
        rt.close()
    }

    @Test
    fun testRenderTargetMostCommonWithMultisample() {
        val rt = renderTarget(640, 480, multisample = BufferMultisample.SampleCount(4)) {
            colorBuffer()
            depthBuffer()
        }
        program.drawer.withTarget(rt) {
            clear(ColorRGBa.PINK)
        }
        rt.close()
    }

    @Test
    fun testRenderTargetCubemap() {
        val cm = cubemap(256, format = ColorFormat.RGBa)
        val rt = renderTarget(256, 256) {
            cubemap(cm, CubemapSide.POSITIVE_X)
            depthBuffer()
        }
        program.drawer.withTarget(rt) {
            clear(ColorRGBa.PINK)
        }
        cm.close()
        rt.close()
    }

    @Test
    fun testRenderTargetVolumeTexture() {
        if (Driver.glType == DriverTypeGL.GL) {
            val vt = volumeTexture(256, 256, 256)
            val rt = renderTarget(256, 256) {
                volumeTexture(vt, 0)
                depthBuffer()
            }
            program.drawer.withTarget(rt) {
                clear(ColorRGBa.PINK)
            }
            rt.close()
            vt.close()
        }
    }

    @Test
    fun testRenderTargetArrayCubemap() {
        val cm = arrayCubemap(256, 10, format = ColorFormat.RGBa)
        val rt = renderTarget(256, 256) {
            arrayCubemap(cm, CubemapSide.POSITIVE_X, 0)
            depthBuffer()
        }
        program.drawer.withTarget(rt) {
            clear(ColorRGBa.PINK)
        }
        cm.close()
        rt.close()
    }
}
