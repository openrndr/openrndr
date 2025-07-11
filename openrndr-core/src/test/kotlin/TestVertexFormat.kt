import org.openrndr.draw.VertexElementType
import org.openrndr.draw.vertexFormat
import kotlin.test.Test
import kotlin.test.assertEquals

class TestVertexFormat {
    @Test
    fun `a vertex format should have a size of 12 bytes`() {
        val vf = vertexFormat {
            position(3)
        }

        assertEquals(12, vf.size)
    }

    @Test
    fun `a vertex format containing arrays should have a size of 32 bytes`() {
        val vf = vertexFormat {
            attribute("someArray", VertexElementType.VECTOR4_FLOAT32, 2)
        }

        assertEquals(32, vf.size)
    }
}