import org.junit.jupiter.api.assertThrows
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.renderTarget
import org.openrndr.internal.Driver
import org.openrndr.internal.gl3.DriverTypeGL
import org.openrndr.internal.gl3.glType
import kotlin.test.*

class TestColorBufferShadowGL3 : AbstractApplicationTestFixture() {
    @Test
    fun `a UINT8RGBA color buffer shadow`() {
        val cb = colorBuffer(256, 256)
        cb.shadow.download()
        for (y in 0 until cb.height) {
            for (x in 0 until cb.width) {
                cb.shadow[x, y]
            }
        }
        cb.destroy()
    }

    @Test
    fun `a UINT8RGB color buffer shadow`() {
        val cb = colorBuffer(256, 256, format = ColorFormat.RGB, type = ColorType.UINT8)
        if (Driver.glType == DriverTypeGL.GL) {
            cb.shadow.download()
        } else {
            assertThrows<IllegalArgumentException> {
                cb.shadow.download()
            }
        }
        for (y in 0 until cb.height) {
            for (x in 0 until cb.width) {
                cb.shadow[x, y]
            }
        }
        cb.destroy()
    }

    @Test
    fun `a UINT8RG color buffer shadow`() {
        val cb = colorBuffer(256, 256, format = ColorFormat.RG)
        cb.shadow.download()
        for (y in 0 until cb.height) {
            for (x in 0 until cb.width) {
                cb.shadow[x, y]
            }
        }
        cb.destroy()
    }

    @Test
    fun `a UINT8R color buffer shadow`() {
        val cb = colorBuffer(256, 256, format = ColorFormat.R)
        cb.shadow.download()
        for (y in 0 until cb.height) {
            for (x in 0 until cb.width) {
                cb.shadow[x, y]
            }
        }
        val rt = renderTarget(256, 256) {
            colorBuffer(cb)
        }
        program.drawer.withTarget(rt) {
            clear(ColorRGBa(127 / 256.0, 0.0, 0.0, 1.0))
        }
        cb.shadow.download()
        for (y in 0 until cb.height) {
            for (x in 0 until cb.width) {
                val c = cb.shadow[x, y]
                assertEquals(c.r, 127.0 / 255.0)
                assertEquals(c.g, 0.0)
                assertEquals(c.b, 0.0)
                assertEquals(c.alpha, 1.0)
            }
        }
        cb.destroy()
    }

    @Test
    fun `a UINT16RGBA color buffer shadow`() {
        if (Driver.glType == DriverTypeGL.GL ) {
            val cb = colorBuffer(256, 256, type = ColorType.UINT16)
            cb.shadow.download()
            for (y in 0 until cb.height) {
                for (x in 0 until cb.width) {
                    cb.shadow[x, y]
                }
            }
            cb.destroy()
        }
    }

    @Test
    fun `a UINT16RGB color buffer shadow`() {
        if (Driver.glType == DriverTypeGL.GL ) {
            val cb = colorBuffer(256, 256, format = ColorFormat.RGB, type = ColorType.UINT16)
            cb.shadow.download()
            for (y in 0 until cb.height) {
                for (x in 0 until cb.width) {
                    cb.shadow[x, y]
                }
            }
            cb.destroy()
        }
    }

    @Test
    fun `a UINT16RG color buffer shadow`() {
        if (Driver.glType == DriverTypeGL.GL ) {
            val cb = colorBuffer(256, 256, format = ColorFormat.RG, type = ColorType.UINT16)
            cb.shadow.download()
            for (y in 0 until cb.height) {
                for (x in 0 until cb.width) {
                    cb.shadow[x, y]
                }
            }
            cb.destroy()
        }
    }

    @Test
    fun `a UINT16R color buffer shadow`() {
        if (Driver.glType == DriverTypeGL.GL ) {
            val cb = colorBuffer(256, 256, format = ColorFormat.R, type = ColorType.UINT16)
            cb.shadow.download()
            for (y in 0 until cb.height) {
                for (x in 0 until cb.width) {
                    cb.shadow[x, y]
                }
            }
            cb.destroy()
        }
    }
}