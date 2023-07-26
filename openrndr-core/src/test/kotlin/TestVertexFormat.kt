import org.amshove.kluent.`should be equal to`
import org.openrndr.draw.VertexElementType
import org.openrndr.draw.vertexFormat
import io.kotest.core.spec.style.DescribeSpec

object TestVertexFormat : DescribeSpec({

    describe("a vertex format") {
        val vf = vertexFormat {
            position(3)
        }

        it("should have a size of 12 bytes") {
            vf.size `should be equal to` 12
        }
    }

    describe("a vertex format containing arrays") {
        val vf = vertexFormat {
            attribute("someArray", VertexElementType.VECTOR4_FLOAT32, 2)
        }

        it("should have a size of 36 bytes") {
            vf.size `should be equal to` 32
        }
    }

})

