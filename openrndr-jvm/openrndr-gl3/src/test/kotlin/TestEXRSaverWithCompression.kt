import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.drawImage
import org.openrndr.internal.Driver
import org.openrndr.internal.exr
import org.openrndr.internal.gl3.DriverTypeGL
import org.openrndr.internal.gl3.ExrCompression
import org.openrndr.internal.gl3.glType
import java.io.File
import kotlin.test.Test

class TestEXRSaverWithCompression {

    private fun testSave(colorFormat: ColorFormat, colorType: ColorType, exrCompression: Int) {
        application {
            program {
                val cb = drawImage(512, 512, 1.0, colorFormat, colorType) {
                    clear(ColorRGBa.TRANSPARENT)
                    stroke = null
                    fill = ColorRGBa.RED
                    rectangle(bounds.sub(0.0, 0.0, 0.25, 1.0))
                    fill = ColorRGBa.GREEN
                    rectangle(bounds.sub(0.25, 0.0, 0.5, 1.0))
                    fill = ColorRGBa.BLUE
                    rectangle(bounds.sub(0.5, 0.0, 0.75, 1.0))
                }
                cb.saveToFile(File("exr_${colorFormat.name}_${colorType.name}_$exrCompression.exr"), async = false) {
                    exr {
                        compression = exrCompression
                    }
                }
                cb.destroy()
                application.exit()
            }
        }
    }

    @Test
    fun testSaveExrRGBa16WithNoCompression() {
        testSave(ColorFormat.RGBa, ColorType.FLOAT16, ExrCompression.NONE)
    }

    @Test
    fun testSaveExrRGB16WithNoCompression() {
        if (Driver.glType == DriverTypeGL.GL) {
            testSave(ColorFormat.RGB, ColorType.FLOAT16, ExrCompression.NONE)
        }
    }

    @Test
    fun testSaveExrRGBa32WithNoCompression() {
        testSave(ColorFormat.RGBa, ColorType.FLOAT32, ExrCompression.NONE)
    }

    @Test
    fun testSaveExrRGB32WithNoCompression() {
        if (Driver.glType == DriverTypeGL.GL) {
            testSave(ColorFormat.RGB, ColorType.FLOAT32, ExrCompression.NONE)
        }
    }

    @Test
    fun testSaveExrRGBa16WithRLECompression() {
        testSave(ColorFormat.RGBa, ColorType.FLOAT16, ExrCompression.RLE)
    }

    @Test
    fun testSaveExrRGB16WithRLECompression() {
        if (Driver.glType == DriverTypeGL.GL) {
            testSave(ColorFormat.RGB, ColorType.FLOAT16, ExrCompression.RLE)
        }
    }

    @Test
    fun testSaveExrRGBa32WithRLECompression() {
        testSave(ColorFormat.RGBa, ColorType.FLOAT32, ExrCompression.RLE)
    }

    @Test
    fun testSaveExrRGB32WithRLECompression() {
        if (Driver.glType == DriverTypeGL.GL) {
            testSave(ColorFormat.RGB, ColorType.FLOAT32, ExrCompression.RLE)
        }
    }

    @Test
    fun testSaveExrRGBa16WithZIPCompression() {
        testSave(ColorFormat.RGBa, ColorType.FLOAT16, ExrCompression.ZIP)
    }

    @Test
    fun testSaveExrRGB16WithZIPCompression() {
        if (Driver.glType == DriverTypeGL.GL) {
            testSave(ColorFormat.RGB, ColorType.FLOAT16, ExrCompression.ZIP)
        }
    }

    @Test
    fun testSaveExrRGBa32WithZIPCompression() {
        testSave(ColorFormat.RGBa, ColorType.FLOAT32, ExrCompression.ZIP)
    }

    @Test
    fun testSaveExrRGB32WithZIPCompression() {
        if (Driver.glType == DriverTypeGL.GL) {
            testSave(ColorFormat.RGB, ColorType.FLOAT32, ExrCompression.ZIP)
        }
    }

    @Test
    fun testSaveExrRGBa16WithZIPSCompression() {
        testSave(ColorFormat.RGBa, ColorType.FLOAT16, ExrCompression.ZIPS)
    }

    @Test
    fun testSaveExrRGB16WithZIPSCompression() {
        if (Driver.glType == DriverTypeGL.GL) {
            testSave(ColorFormat.RGB, ColorType.FLOAT16, ExrCompression.ZIPS)
        }
    }

    @Test
    fun testSaveExrRGBa32WithZIPSCompression() {
        testSave(ColorFormat.RGBa, ColorType.FLOAT32, ExrCompression.ZIPS)
    }

    @Test
    fun testSaveExrRGB32WithZIPSCompression() {
        if (Driver.glType == DriverTypeGL.GL) {
            testSave(ColorFormat.RGB, ColorType.FLOAT32, ExrCompression.ZIPS)
        }
    }

    @Test
    fun testSaveExrRGBa16WithPIZCompression() {
        testSave(ColorFormat.RGBa, ColorType.FLOAT16, ExrCompression.PIZ)
    }

    @Test
    fun testSaveExrRGB16WithPIZCompression() {
        if (Driver.glType == DriverTypeGL.GL) {
            testSave(ColorFormat.RGB, ColorType.FLOAT16, ExrCompression.PIZ)
        }
    }

    @Test
    fun testSaveExrRGBa32WithPIZCompression() {
        testSave(ColorFormat.RGBa, ColorType.FLOAT32, ExrCompression.PIZ)
    }

    @Test
    fun testSaveExrRGB32WithPIZCompression() {
        if (Driver.glType == DriverTypeGL.GL) {
            testSave(ColorFormat.RGB, ColorType.FLOAT32, ExrCompression.PIZ)
        }
    }
}