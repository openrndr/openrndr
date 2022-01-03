import kotlin.test.Test

import org.openrndr.application
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.colorBuffer
import org.openrndr.internal.gl3.ColorBufferGL3
import org.openrndr.internal.gl3.ExrCompression
import java.io.File
import kotlin.test.Ignore

class TestEXRSaverWithCompression {

    @Test
    fun testSaveExrRGBa16WithRLECompression() {
        application {
            program {
                val cb = colorBuffer(512, 512, format = ColorFormat.RGBa, type = ColorType.FLOAT16)
                (cb as ColorBufferGL3).exrCompression = ExrCompression.RLE
                cb.saveToFile(File("exr16a.exr"), async = false)
                application.exit()
            }
        }
    }

    @Test
    fun testSaveExrRGB16WithRLECompression() {
        application {
            program {
                val cb = colorBuffer(512, 512, format = ColorFormat.RGB, type = ColorType.FLOAT16)
                (cb as ColorBufferGL3).exrCompression = ExrCompression.RLE
                cb.saveToFile(File("exr16.exr"), async = false)
                application.exit()
            }
        }
    }

    @Test
    fun testSaveExrRGB32WithRLECompression() {
        application {
            program {
                val cb = colorBuffer(512, 512, format = ColorFormat.RGB, type = ColorType.FLOAT32)
                (cb as ColorBufferGL3).exrCompression = ExrCompression.RLE
                cb.saveToFile(File("exr32.exr"), async = false)
                application.exit()
            }
        }
    }

    @Test
    fun testSaveExrRGBa32WithRLECompression() {
        application {
            program {
                val cb = colorBuffer(512, 512, format = ColorFormat.RGBa, type = ColorType.FLOAT32)
                (cb as ColorBufferGL3).exrCompression = ExrCompression.RLE
                cb.saveToFile(File("exr32a.exr"), async = false)
                application.exit()
            }
        }
    }

    @Test
    fun testSaveExrRGBa16WithZIPCompression() {
        application {
            program {
                val cb = colorBuffer(512, 512, format = ColorFormat.RGBa, type = ColorType.FLOAT16)
                (cb as ColorBufferGL3).exrCompression = ExrCompression.ZIP
                cb.saveToFile(File("exr16a.exr"), async = false)
                application.exit()
            }
        }
    }

    @Test
    fun testSaveExrRGB16WithZIPCompression() {
        application {
            program {
                val cb = colorBuffer(512, 512, format = ColorFormat.RGB, type = ColorType.FLOAT16)
                (cb as ColorBufferGL3).exrCompression = ExrCompression.ZIP
                cb.saveToFile(File("exr16.exr"), async = false)
                application.exit()
            }
        }
    }

    @Test
    fun testSaveExrRGBa32WithZIPCompression() {
        application {
            program {
                val cb = colorBuffer(512, 512, format = ColorFormat.RGBa, type = ColorType.FLOAT32)
                (cb as ColorBufferGL3).exrCompression = ExrCompression.ZIP
                cb.saveToFile(File("exr32a.exr"), async = false)
                application.exit()
            }
        }
    }

    @Test
    fun testSaveExrRGB32WithZIPCompression() {
        application {
            program {
                val cb = colorBuffer(512, 512, format = ColorFormat.RGB, type = ColorType.FLOAT32)
                (cb as ColorBufferGL3).exrCompression = ExrCompression.ZIP
                cb.saveToFile(File("exr32.exr"), async = false)
                application.exit()
            }
        }
    }

    @Test
    fun testSaveExrRGBa16WithZIPSCompression() {
        application {
            program {
                val cb = colorBuffer(512, 512, format = ColorFormat.RGBa, type = ColorType.FLOAT16)
                (cb as ColorBufferGL3).exrCompression = ExrCompression.ZIPS
                cb.saveToFile(File("exr16a.exr"), async = false)
                application.exit()
            }
        }
    }

    @Test
    fun testSaveExrRGB16WithZIPSCompression() {
        application {
            program {
                val cb = colorBuffer(512, 512, format = ColorFormat.RGB, type = ColorType.FLOAT16)
                (cb as ColorBufferGL3).exrCompression = ExrCompression.ZIPS
                cb.saveToFile(File("exr16.exr"), async = false)
                application.exit()
            }
        }
    }

    @Test
    fun testSaveExrRGBa32WithZIPSCompression() {
        application {
            program {
                val cb = colorBuffer(512, 512, format = ColorFormat.RGBa, type = ColorType.FLOAT32)
                (cb as ColorBufferGL3).exrCompression = ExrCompression.ZIPS
                cb.saveToFile(File("exr32a.exr"), async = false)
                application.exit()
            }
        }
    }

    @Test
    fun testSaveExrRGB32WithZIPSCompression() {
        application {
            program {
                val cb = colorBuffer(512, 512, format = ColorFormat.RGB, type = ColorType.FLOAT32)
                (cb as ColorBufferGL3).exrCompression = ExrCompression.ZIPS
                cb.saveToFile(File("exr32.exr"), async = false)
                application.exit()
            }
        }
    }

    @Test
    fun testSaveExrRGBa16WithPIZCompression() {
        application {
            program {
                val cb = colorBuffer(512, 512, format = ColorFormat.RGBa, type = ColorType.FLOAT16)
                (cb as ColorBufferGL3).exrCompression = ExrCompression.PIZ
                cb.saveToFile(File("exr16a.exr"), async = false)
                application.exit()
            }
        }
    }

    @Test
    fun testSaveExrRGB16WithPIZCompression() {
        application {
            program {
                val cb = colorBuffer(512, 512, format = ColorFormat.RGB, type = ColorType.FLOAT16)
                (cb as ColorBufferGL3).exrCompression = ExrCompression.PIZ
                cb.saveToFile(File("exr16.exr"), async = false)
                application.exit()
            }
        }
    }

    @Test
    fun testSaveExrRGBa32WithPIZCompression() {
        application {
            program {
                val cb = colorBuffer(512, 512, format = ColorFormat.RGBa, type = ColorType.FLOAT32)
                (cb as ColorBufferGL3).exrCompression = ExrCompression.PIZ
                cb.saveToFile(File("exr32a.exr"), async = false)
                application.exit()
            }
        }
    }

    @Test
    fun testSaveExrRGB32WithPIZCompression() {
        application {
            program {
                val cb = colorBuffer(512, 512, format = ColorFormat.RGB, type = ColorType.FLOAT32)
                (cb as ColorBufferGL3).exrCompression = ExrCompression.PIZ
                cb.saveToFile(File("exr32.exr"), async = false)
                application.exit()
            }
        }
    }

}