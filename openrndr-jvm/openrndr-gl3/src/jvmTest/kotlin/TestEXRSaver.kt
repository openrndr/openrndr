import org.openrndr.application
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.colorBuffer
import java.io.File
import kotlin.test.*

class TestEXRSaver {

    @Test
    fun testIssue278() {
        // see https://github.com/openrndr/openrndr/issues/278
        application {
            program {
                val cb = colorBuffer(512, 512, format = ColorFormat.RGBa, type = ColorType.FLOAT16)
                for (i in 0 until 10) {
                    cb.saveToFile(File("exr16a.exr"), async = false)
                    // Attempt to trigger a GC as well as we can. This will likely expose any problems with
                    // doubly freed memory, should any exist.
                    System.gc()
                }
                application.exit()
            }
        }
    }

    @Test
    fun testSaveExrRGBa16() {
        application {
            program {
                val cb = colorBuffer(512, 512, format = ColorFormat.RGBa, type = ColorType.FLOAT16)
                for (i in 0 until 10) {
                    cb.saveToFile(File("exr16a.exr"), async = false)
                }
                application.exit()
            }
        }
    }
    @Test
    fun testSaveExrRGB16() {
        application {
            program {
                val cb = colorBuffer(512, 512, format = ColorFormat.RGB, type = ColorType.FLOAT16)
                cb.saveToFile(File("exr16.exr"), async = false)
                application.exit()
            }
        }
    }

    @Test
    fun testSaveExrRGBa32() {
        application {
            program {
                val cb = colorBuffer(512, 512, format = ColorFormat.RGBa, type = ColorType.FLOAT32)
                cb.saveToFile(File("exr32a.exr"), async = false)
                application.exit()
            }
        }
    }

    @Test
    fun testSaveExrRGB32() {
        application {
            program {
                val cb = colorBuffer(512, 512, format = ColorFormat.RGB, type = ColorType.FLOAT32)
                cb.saveToFile(File("exr32.exr"), async = false)
                application.exit()
            }
        }
    }
}