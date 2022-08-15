import org.openrndr.Program
import org.openrndr.draw.*
import org.openrndr.internal.gl3.VertexBufferGL3
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TestShadeStylesSuppressOutput : Spek({
    describe("a program") {
        val program = Program().initializeGLFWGL3Application()
        val vbgl3 = VertexBufferGL3.createDynamic(vertexFormat {
            position(3)
        }, 10, null)

        program.drawer.shadeStyle = shadeStyle {
            suppressDefaultOutput = true
        }

        describe("circle") {
            it("should be able to do shadestyles") {
                program.drawer.circle(Vector2(100.0, 100.0), 20.0)
            }
        }

        it("rectangle should be able to do shadestyles") {
            program.drawer.rectangle(0.0, 0.0, 100.0, 100.0)
        }

        describe("line") {
            it("should be able to do shadestyles") {
                program.drawer.lineSegment(0.0, 0.0, 100.0, 100.0)
            }
        }

        describe("fast line") {
            it("should be able to do shadestyles") {
                program.drawer.drawStyle.quality = DrawQuality.PERFORMANCE
                program.drawer.lineSegment(0.0, 0.0, 100.0, 100.0)
                program.drawer.drawStyle.quality = DrawQuality.QUALITY
            }
        }

        describe("mesh line") {
            it("should be able to do shadestyles") {
                program.drawer.lineSegment(Vector3(0.0, 0.0,0.0), Vector3(100.0, 0.0,0.0))
            }
        }

        describe("vertex buffer") {
            it("should be able to do shadestyles") {
                program.drawer.vertexBuffer(vbgl3, DrawPrimitive.TRIANGLES)
            }
        }

        describe("image") {
            val cb = colorBuffer(640, 640)
            it("should be able to do shadestyles") {
                program.drawer.image(cb)
            }
        }

        describe("font image maps") {
//            val font = FontImageMaprogram.fromUrl(resourceUrl("/fonts/Roboto-Medium.ttf"), 16.0)
//            program.drawer.fontMap = font
//            it("should be able to do shadestyles") {
//                program.drawer.text("this is a test", 0.0, 0.0)
//            }
        }
    }
})