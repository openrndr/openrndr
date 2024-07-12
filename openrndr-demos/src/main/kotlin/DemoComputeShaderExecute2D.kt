import org.openrndr.application
import org.openrndr.draw.ComputeShader
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.computeShader2DExecuteSize
import org.openrndr.draw.font.BufferAccess
import org.openrndr.draw.imageBinding
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
            height = height -1
        )
        val resolution = IntVector2(image.width, image.height)
        val shader = ComputeShader.fromCode(
            code = """
                |#version 430
                |layout (local_size_x = 8, local_size_y = 8) in;
                |uniform writeonly restrict image2D image;
                |uniform ivec2 resolution;
                |uniform float seconds;
                |
                |void main() {
                |    ivec2 coord = ivec2(gl_GlobalInvocationID.xy);
                |    if (coord.x >= resolution.x || coord.y >= resolution.y) {
                |        return;
                |    }
                |    vec4 color = vec4(
                |        coord.x / float(resolution.x),
                |        coord.y / float(resolution.y),
                |        sin(seconds) * .5 + .5,
                |        1.0
                |    );
                |    imageStore(image, coord, color);
                |}
                """.trimMargin(),
            name = "image-computer"
        ).apply {
            uniform("resolution", IntVector2(image.width, image.height))
            image("image", 0, image.imageBinding(0, BufferAccess.WRITE))
        }
        val executeSize = computeShader2DExecuteSize(
            workGroupSize = IntVector3(x = 8, y = 8, z = 1),
            dataSize = resolution
        )
        extend {
            shader.uniform("seconds", seconds)
            shader.execute(executeSize)
            drawer.image(image)
        }
    }
}
