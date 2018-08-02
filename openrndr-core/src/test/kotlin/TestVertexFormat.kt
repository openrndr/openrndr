import org.amshove.kluent.`should equal`
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.openrndr.draw.VertexElementType
import org.openrndr.draw.vertexFormat

object TestVertexFormat : Spek({

    describe("a vertex format") {
        val vf = vertexFormat {
            position(3)
        }

        it("should have a size of 12 bytes") {
            vf.size `should equal` 12
        }
    }

    describe("a vertex format containing arrays") {
        val vf = vertexFormat {
            attribute("someArray", VertexElementType.VECTOR4_FLOAT32, 2)
        }

        it("should have a size of 36 bytes") {
            vf.size `should equal` 32
        }
    }

})

