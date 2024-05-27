import org.openrndr.draw.*
import org.openrndr.internal.Driver
import org.openrndr.internal.gl3.*
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.resourceUrl
import kotlin.test.*

class TestShadeStylesGL3 : AbstractApplicationTestFixture() {
    @BeforeTest
    override fun setup() {
        super.setup()
        program.drawer.shadeStyle = shadeStyle {
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
    }

    @Test
    fun `a circle should be able to do shadestyles`() {
        program.drawer.circle(Vector2(100.0, 100.0), 20.0)
    }

    @Test
    fun `rectangle should be able to do shadestyles`() {
        program.drawer.rectangle(0.0, 0.0, 100.0, 100.0)
    }

    @Test
    fun line() {
        program.drawer.lineSegment(0.0, 0.0, 100.0, 100.0)
    }

    @Test
    fun `fast line`() {
        program.drawer.drawStyle.quality = DrawQuality.PERFORMANCE
        program.drawer.lineSegment(0.0, 0.0, 100.0, 100.0)
        program.drawer.drawStyle.quality = DrawQuality.QUALITY
    }

    @Test
    fun `mesh line`() {
        program.drawer.lineSegment(Vector3(0.0, 0.0, 0.0), Vector3(100.0, 0.0, 0.0))
    }

    @Test
    fun `vertex buffer`() {
        val vbgl3 = VertexBufferGL3.createDynamic(vertexFormat {
            position(3)
        }, 10, null)
        program.drawer.vertexBuffer(vbgl3, DrawPrimitive.TRIANGLES)
        vbgl3.destroy()
    }

    @Test
    fun image() {
        val cb = colorBuffer(640, 640)
        program.drawer.image(cb)
        cb.destroy()
    }

    @Test
    fun `font image maps`() {
        val font = FontImageMap.fromUrl(resourceUrl("/fonts/Roboto-Medium.ttf"), 16.0)
        program.drawer.fontMap = font
        program.drawer.text("this is a test", 0.0, 0.0)
    }

    @Test
    fun ssbo() {
        if ((Driver.instance as DriverGL3).version >= DriverVersionGL.GL_VERSION_4_3 && Driver.glType == DriverTypeGL.GL) {
            val ssbo = shaderStorageBuffer(shaderStorageFormat {
                primitive("floats", BufferPrimitiveType.FLOAT32, 100)
            })
            val ss = shadeStyle {
                fragmentTransform = """float n = b_buffer.floats[0];"""
                buffer("buffer", ssbo)
            }
            program.drawer.isolated {
                program.drawer.shadeStyle = ss
                program.drawer.rectangle(0.0, 0.0, 100.0, 100.0)
            }
        }
    }

    @Test
    fun structuredBuffers() {
        if ((Driver.instance as DriverGL3).version >= DriverVersionGL.GL_VERSION_4_3 && Driver.glType == DriverTypeGL.GL) {
            class CustomStruct: Struct<CustomStruct>() {
                var floats by arrayField<Double>(100)
            }

            val sb = structuredBuffer(CustomStruct())
            val ss = shadeStyle {
                fragmentTransform = """float n = b_buffer.floats[0];"""
                buffer("buffer", sb.ssbo)
            }
            program.drawer.isolated {
                program.drawer.shadeStyle = ss
                program.drawer.rectangle(0.0, 0.0, 100.0, 100.0)
            }
        }
    }

    @Test
    fun unusedStructuredBuffers() {
        if ((Driver.instance as DriverGL3).version >= DriverVersionGL.GL_VERSION_4_3 && Driver.glType == DriverTypeGL.GL) {
            class CustomStruct: Struct<CustomStruct>() {
                var floats by arrayField<Double>(100)
            }

            val sb = structuredBuffer(CustomStruct())
            val ss = shadeStyle {
                buffer("buffer", sb.ssbo)
            }
            program.drawer.isolated {
                program.drawer.shadeStyle = ss
                program.drawer.rectangle(0.0, 0.0, 100.0, 100.0)
            }
        }
    }
}