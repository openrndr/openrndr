import org.openrndr.draw.BufferAlignment
import org.openrndr.draw.computeStyle
import org.openrndr.draw.execute
import org.openrndr.draw.vertexFormat
import org.openrndr.internal.Driver
import org.openrndr.internal.gl3.DriverVersionGL
import org.openrndr.internal.gl3.VertexBufferGL3
import org.openrndr.internal.gl3.glVersion
import org.openrndr.math.Vector3
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class TestVertexBufferToSSBO : AbstractApplicationTestFixture() {
    lateinit var vb0: VertexBufferGL3
    lateinit var vb1: VertexBufferGL3

    @BeforeTest
    override fun setup() {
        super.setup()
        vb0 = VertexBufferGL3.createDynamic(vertexFormat(BufferAlignment.STD430) {
            position(3)
            normal(3)
        }, 10, null)
        vb1 = VertexBufferGL3.createDynamic(vertexFormat(BufferAlignment.STD430) {
            position(3)
            normal(3)
        }, 10, null)
    }

    @Test
    fun canConvertToSSBOAndCopyInCompute() {
        if (Driver.glVersion.isAtLeast(DriverVersionGL.GL_VERSION_4_3, DriverVersionGL.GLES_VERSION_3_1)) {
            val ssbo = vb0.shaderStorageBufferView()
            val shadow0 = vb0.shadow
            val w = shadow0.writer()
            w.rewind()
            w.write(Vector3(1.0, 2.0, 3.0))
            w.write(Vector3(4.0, 5.0, 6.0))
            shadow0.upload()

            val ssbo2 = vb1.shaderStorageBufferView()
            val cs = computeStyle {
                computeTransform = """
                b_output_vertices.vertex[0] = b_input_vertices.vertex[0];
                
            """.trimIndent()
                buffer("input_vertices", ssbo)
                buffer("output_vertices", ssbo2)
            }
            cs.execute(1, 1, 1)
            val shadow1 = vb1.shadow
            shadow1.download()
            val r = shadow1.reader()
            r.rewind()

            val v = r.readVector3()
            val n = r.readVector3()
            assertEquals(1.0, v.x)
            assertEquals(2.0, v.y)
            assertEquals(3.0, v.z)
            assertEquals(4.0, n.x)
            assertEquals(5.0, n.y)
            assertEquals(6.0, n.z)
        }
    }


    @AfterTest
    override fun teardown() {
        vb0.destroy()
        vb1.destroy()
        super.teardown()
    }
}
