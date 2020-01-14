
import org.openrndr.Configuration
import org.openrndr.Program
import org.openrndr.draw.*
import org.openrndr.internal.gl3.ApplicationGLFWGL3
import org.openrndr.internal.gl3.VertexBufferGL3
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TestShadeStylesGL3 : Spek({
    describe("a program") {
        val p = Program()
        val app = ApplicationGLFWGL3(p, Configuration())
        app.setup()
        app.preloop()
        val vbgl3 = VertexBufferGL3.createDynamic(vertexFormat {
            position(3)
        }, 10, null)

        p.drawer.shadeStyle = shadeStyle {
            vertexTransform = """
                |int k = c_element;
                |if (d_primitive == d_image) {}
                |if (d_primitive == d_vertex_buffer) {}
                |if (d_primitive == d_circle) {}
                |if (d_primitive == d_rectangle) {}
                |if (d_primitive == d_font_image_map) {}
                |if (d_primitive == d_fast_line) {}
                |if (d_primitive == d_expansion) {}
            """.trimMargin()

            fragmentTransform = """x_fill.xy = c_screenPosition.xy;
                |x_fill.rg = va_position.xy;
                |x_fill.b = c_contourPosition;
                |x_stroke.rg = c_boundsPosition.rg;
                |int k = c_element;
                |if (d_primitive == d_image) {}
                |if (d_primitive == d_vertex_buffer) {}
                |if (d_primitive == d_circle) {}
                |if (d_primitive == d_rectangle) {}
                |if (d_primitive == d_font_image_map) {}
                |if (d_primitive == d_fast_line) {}
                |if (d_primitive == d_expansion) {}
            """.trimMargin()
        }


            it("a circle should be able to do shadestyles") {
                p.drawer.circle(Vector2(100.0, 100.0), 20.0)
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
                p.drawer.lineSegment(Vector3(0.0, 0.0,0.0),Vector3(100.0, 0.0,0.0))
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