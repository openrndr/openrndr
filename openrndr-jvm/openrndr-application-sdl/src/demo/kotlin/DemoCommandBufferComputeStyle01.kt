import org.openrndr.application
import org.openrndr.draw.IndexType
import org.openrndr.draw.computeStyle
import org.openrndr.draw.execute
import org.openrndr.draw.indexBuffer
import org.openrndr.internal.Driver
import org.openrndr.math.IntVector3

fun main() {
    application {
        program {

            val ib = indexBuffer(10, IndexType.INT32)
            val cb = Driver.instance.createCommandBuffer(2u)

            val cs = computeStyle {
                computeTransform = """
                    for (int i = 0; i < 10; ++i) {
                        b_indexBuffer.indices[0] = i;
                    }
                    b_commandBuffer.commands[0].instanceCount = 1u;
                """
                workGroupSize = IntVector3(1, 1, 1)
                buffer("commandBuffer", cb)
                buffer("indexBuffer", ib)
            }

            cs.execute(1, 1, 1)

            extend {

            }
        }
    }
}