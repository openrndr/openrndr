import org.openrndr.draw.computeStyle
import org.openrndr.draw.execute
import org.openrndr.draw.vertexFormat
import org.openrndr.internal.gl3.VertexBufferGL3
import java.nio.ByteBuffer
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class TestVertexBufferToSSBO : AbstractApplicationTestFixture() {
    lateinit var vb0: VertexBufferGL3
    lateinit var vb1: VertexBufferGL3

    @BeforeTest
    override fun setup() {
        super.setup()
        vb0 = VertexBufferGL3.createDynamic(vertexFormat {
            position(3)
        }, 10, null)
        vb1 = VertexBufferGL3.createDynamic(vertexFormat {
            position(3)
        }, 10, null)
    }

    @Test
    fun `can convert to ssbo`() {
        val ssbo = vb0.shaderStorageBufferView()
        val ssbo2 = vb1.shaderStorageBufferView()
        val cs = computeStyle {
            computeTransform = """
                b_output_vertices.vertex[0] = b_input_vertices.vertex[0];
                
            """.trimIndent()
            buffer("input_vertices", ssbo)
            buffer("output_vertices", ssbo2)
        }
        cs.execute(1,1,1)
    }


    @AfterTest
    override fun teardown() {
        vb0.destroy()
        vb1.destroy()
        super.teardown()
    }
}
