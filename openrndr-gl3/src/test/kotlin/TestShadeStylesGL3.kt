import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.lwjgl.BufferUtils
import org.openrndr.Configuration
import org.openrndr.Program
import org.openrndr.draw.*
import org.openrndr.internal.gl3.ApplicationGL3
import org.openrndr.internal.gl3.VertexBufferGL3
import org.openrndr.math.Vector2
import java.nio.ByteBuffer

object TestShadeStylesGL3 : Spek({
    describe("a vertexbuffer") {
        val p = Program()
        val app = ApplicationGL3(p, Configuration())
        app.setup()
        app.preloop()
        val vbgl3 = VertexBufferGL3.createDynamic(vertexFormat {
            position(3)
        }, 10)

        p.drawer.shadeStyle = shadeStyle {
            fragmentTransform = """x_fill.xy = c_screenPosition.xy;
                |x_fill.rg = va_position.xy;
                |x_fill.b = c_contourPosition;
            """.trimMargin()
        }

        describe("circle") {
            it("should be able to do shadestyles") {
                p.drawer.circle(Vector2(100.0, 100.0), 20.0)
            }
        }

        it("rectangle should be able to do shadestyles") {
            p.drawer.rectangle(0.0, 0.0, 100.0, 100.0)
        }

        describe("line") {
            it("hould be able to do shadestyles") {
                p.drawer.lineSegment(0.0, 0.0, 100.0, 100.0)
            }
        }

        describe("vertex buffer") {
            it("should be able to do shadestyles") {
                p.drawer.vertexBuffer(vbgl3, DrawPrimitive.TRIANGLES)
            }
        }
        describe("image") {
            val cb = colorBuffer(640, 640)
            it("should be able to do shadestyles") {
                p.drawer.image(cb)
            }
        }

    }
})