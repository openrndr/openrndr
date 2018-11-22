import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it

import org.openrndr.Configuration
import org.openrndr.Program
import org.openrndr.draw.*
import org.openrndr.internal.gl3.ApplicationGLFWGL3
import org.openrndr.internal.gl3.VertexBufferGL3
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.resourceUrl

object TestShadeStylesSuppressOutput : Spek({
    describe("a program") {
        val p = Program()
        val app = ApplicationGLFWGL3(p, Configuration())
        app.setup()
        app.preloop()
        val vbgl3 = VertexBufferGL3.createDynamic(vertexFormat {
            position(3)
        }, 10)

        p.drawer.shadeStyle = shadeStyle {
            suppressDefaultOutput = true
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
            it("should be able to do shadestyles") {
                p.drawer.lineSegment(0.0, 0.0, 100.0, 100.0)
            }
        }

        describe("fast line") {
            it("should be able to do shadestyles") {
                p.drawer.drawStyle.quality = DrawQuality.PERFORMANCE
                p.drawer.lineSegment(0.0, 0.0, 100.0, 100.0)
                p.drawer.drawStyle.quality = DrawQuality.QUALITY
            }
        }

        describe("mesh line") {
            it("should be able to do shadestyles") {
                p.drawer.lineSegment(Vector3(0.0, 0.0,0.0), Vector3(100.0, 0.0,0.0))
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

        describe("font image maps") {
//            val font = FontImageMap.fromUrl(resourceUrl("/fonts/Roboto-Medium.ttf"), 16.0)
//            p.drawer.fontMap = font
//            it("should be able to do shadestyles") {
//                p.drawer.text("this is a test", 0.0, 0.0)
//            }
        }
    }
})