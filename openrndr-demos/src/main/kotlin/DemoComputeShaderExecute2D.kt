import org.openrndr.application
import org.openrndr.draw.*
import org.openrndr.draw.font.BufferAccess
import org.openrndr.math.IntVector2
import org.openrndr.math.IntVector3

/**
 * A [ComputeShader] producing an image of an arbitrary size. If dimensions of an image are not a multiplicity
 * of a workgroup size dimensions (`local_size_x`, `local_size_y`), then excessive calculations will happen,
 * and they should be discarded with the initial if statement in the compute shader.
 * The [computeShader2DExecuteSize] function will calculate proper `executeSize` for the `ComputeShader`
 * outputting such a 2D image.
 */
fun main() = application {
    program {
        val image = colorBuffer(
            width = width - 1,  // we are changing to image size to uneven on purpose
            height = height -1,
            type = ColorType.FLOAT32
        )
        val shader = computeStyle {
            computeTransform = """
                ivec2 coord = ivec2(gl_GlobalInvocationID.xy);
                if (coord.x >= p_resolution.x || coord.y >= p_resolution.y) {
                    return;
                }
                vec4 color = vec4(
                    float(coord.x) / float(p_resolution.x),
                    float(coord.y) / float(p_resolution.y),
                    sin(p_seconds) * 0.5 + 0.5,
                    1.0
                );
                imageStore(p_image, coord, color);                
            """.trimIndent()
            workGroupSize = IntVector3(8, 8, 1)
        }
        val executeSize = computeShader2DExecuteSize(
            shader.workGroupSize,
            IntVector2(image.width, image.height)
        )
        println("image size = ${image.width} x ${image.height}")
        println("workGroupSize = ${shader.workGroupSize}")
        println("executeSize = $executeSize")

        extend {
            shader.parameter("seconds", seconds)
            shader.parameter("resolution", IntVector2(image.width, image.height))
            shader.image("image", image.imageBinding(0, BufferAccess.WRITE))
            shader.execute(executeSize.x, executeSize.y, executeSize.z)
            drawer.image(image)
        }
    }
}
