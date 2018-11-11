
import org.amshove.kluent.`should equal`
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.lwjgl.BufferUtils
import org.openrndr.Configuration
import org.openrndr.Program
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.internal.gl3.ApplicationGLFWGL3
import org.openrndr.internal.gl3.VertexBufferGL3
import java.nio.ByteBuffer

object TestColorBufferShadowGL3 : Spek({

    describe("a program") {
        val program = Program()
        val app = ApplicationGLFWGL3(program, Configuration())
        app.setup()
        app.preloop()

        describe("a UINT8/RGBA color buffer shadow") {
            val cb = colorBuffer(256, 256)
            cb.shadow.download()
            it("should be able to read all pixels ") {
                for (y in 0 until cb.height) {
                    for (x in 0 until cb.width) {
                        val c = cb.shadow[x,y]
                    }
                }
            }
        }
        describe("a UINT8/RGB color buffer shadow") {
            val cb = colorBuffer(256, 256, format = ColorFormat.RGB)
            cb.shadow.download()
            it("should be able to read all pixels ") {
                for (y in 0 until cb.height) {
                    for (x in 0 until cb.width) {
                        val c = cb.shadow[x,y]
                    }
                }
            }
        }

        describe("a UINT8/RG color buffer shadow") {
            val cb = colorBuffer(256, 256, format = ColorFormat.RG)
            cb.shadow.download()
            it("should be able to read all pixels ") {
                for (y in 0 until cb.height) {
                    for (x in 0 until cb.width) {
                        val c = cb.shadow[x,y]
                    }
                }
            }
        }

        describe("a UINT8/R color buffer shadow") {
            val cb = colorBuffer(256, 256, format = ColorFormat.R)
            cb.shadow.download()
            it("should be able to read all pixels ") {
                for (y in 0 until cb.height) {
                    for (x in 0 until cb.width) {
                        val c = cb.shadow[x,y]
                    }
                }
            }
            val rt = renderTarget(256, 256) {
                colorBuffer(cb)
            }
            program.drawer.withTarget(rt) {
                background(ColorRGBa(127/256.0, 0.0, 0.0, 1.0))
            }
            cb.shadow.download()
            it("should be able to read all pixels correctly ") {
                for (y in 0 until cb.height) {
                    for (x in 0 until cb.width) {
                        val c = cb.shadow[x,y]
                        c.r `should equal` 127/255.0
                        c.g `should equal` 0.0
                        c.b `should equal` 0.0
                        c.a `should equal` 1.0
                    }
                }
            }

        }

        describe("a UINT16/RGBA color buffer shadow") {
            val cb = colorBuffer(256, 256, type= ColorType.UINT16)
            cb.shadow.download()
            it("should be able to read all pixels ") {
                for (y in 0 until cb.height) {
                    for (x in 0 until cb.width) {
                        val c = cb.shadow[x,y]
                    }
                }
            }
        }
        describe("a UINT16/RGB color buffer shadow") {
            val cb = colorBuffer(256, 256, format = ColorFormat.RGB, type= ColorType.UINT16)
            cb.shadow.download()
            it("should be able to read all pixels ") {
                for (y in 0 until cb.height) {
                    for (x in 0 until cb.width) {
                        val c = cb.shadow[x,y]
                    }
                }
            }
        }

        describe("a UINT16/RG color buffer shadow") {
            val cb = colorBuffer(256, 256, format = ColorFormat.RG, type= ColorType.UINT16)
            cb.shadow.download()
            it("should be able to read all pixels ") {
                for (y in 0 until cb.height) {
                    for (x in 0 until cb.width) {
                        val c = cb.shadow[x,y]
                    }
                }
            }
        }

        describe("a UINT16/R color buffer shadow") {
            val cb = colorBuffer(256, 256, format = ColorFormat.R, type= ColorType.UINT16)
            cb.shadow.download()
            it("should be able to read all pixels ") {
                for (y in 0 until cb.height) {
                    for (x in 0 until cb.width) {
                        val c = cb.shadow[x,y]
                    }
                }
            }
        }
    }

})