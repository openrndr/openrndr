package org.openrndr.draw

import org.openrndr.internal.Driver
import org.openrndr.math.*
import kotlin.math.ceil

interface ComputeShader : ShaderImageBindings, ShaderBufferBindings, ShaderUniforms {
    companion object {
        /**
         * Create a compute shader from (GLSL) code as a String
         */
        fun fromCode(code: String, name: String): ComputeShader = Driver.instance.createComputeShader(code, name)
    }


    /**
     * Execute the compute shader
     * @param width the global width
     * @param height the global height
     * @param depth the global depth
     */
    fun execute(width: Int = 1, height: Int = 1, depth: Int = 1)

    fun execute(dimensions: IntVector3) = execute(dimensions.x, dimensions.y, dimensions.z)

    /**
     * Destroy the compute shader
     */
    fun destroy()

}

/**
 * Determines optimal size of [ComputeShader] execution for 1-dimensional data of variable size.
 *
 * Note: if the data size is not dividable by the work group size, the excessive invocations will happen
 * and must be discarded in the compute shader:
 *
 * ```
 * #version 430
 * layout (local_size_x = 64) in;
 *
 * uniform int p_dataSize;
 *
 * void main() {
 *     int x = int(gl_GlobalInvocationID.x);
 *     if (x >= p_dataSize) {
 *         return;
 *     }
 *     // shader code
 * }
 * ```
 *
 * @param workGroupSize the work group size, where the `x` component should be the same as
 *          `local_size_x` specified in the shader.
 * @param dataSize the size of the data to process.
 */
fun computeShader1DExecuteSize(
    workGroupSize: IntVector3,
    dataSize: Int
): IntVector3 {
    require(workGroupSize.y == 1) { "workGroupSize.y must be 1" }
    require(workGroupSize.z == 1) { "workGroupSize.z must be 1" }
    return computeShaderExecuteSize(
        workGroupSize,
        IntVector3(dataSize, 1, 1)
    )
}

/**
 * Determines optimal size of [ComputeShader] execution for 2-dimensional data of variable size.
 *
 * Note: if the data size is not dividable by the work group size, the excessive invocations will happen
 * and must be discarded in the compute shader:
 *
 * ```
 * #version 430
 * layout (local_size_x = 8, local_size_y = 8) in;
 *
 * uniform ivec2 p_dataSize;
 *
 * void main() {
 *     ivec2 coord = int(gl_GlobalInvocationID.xy);
 *     if (coord.x >= p_dataSize.x || coord.y >= p_dataSize.y) {
 *         return;
 *     }
 *     // shader code
 * }
 * ```
 *
 * @param workGroupSize the work group size, where the `x` and `y` components should be the same as
 *          `local_size_x` and `local_size_y` respectively, as specified in the shader.
 * @param dataSize the size of the data to process.
 */
fun computeShader2DExecuteSize(
    workGroupSize: IntVector3,
    dataSize: IntVector2
): IntVector3 {
    require(workGroupSize.z == 1) { "workGroupSize.z must be 1" }
    return computeShaderExecuteSize(
        workGroupSize,
        IntVector3(dataSize.x, dataSize.y, 1)
    )
}

/**
 * Determines optimal size of [ComputeShader] execution for 3-dimensional data of variable size.
 *
 * Note: if the data size is not dividable by the work group size, the excessive invocations will happen
 * and must be discarded in the compute shader:
 *
 * ```
 * #version 430
 * layout (local_size_x = 8, local_size_y = 8, local_size_z = 2) in;
 *
 * uniform ivec3 p_dataSize;
 *
 * void main() {
 *     ivec3 coord = int(gl_GlobalInvocationID.xyz);
 *     if (coord.x >= p_dataSize.x || coord.y >= p_dataSize.y || coord.z >= p_dataSize.z) {
 *         return;
 *     }
 *     // shader code
 * }
 * ```
 *
 * @param workGroupSize the work group size, where the `x`, `y` and `z` components should be the same as
 *          `local_size_x`, `local_size_y` and `local_size_y`respectively, as specified in the shader.
 * @param dataSize the size of the data to process.
 */
fun computeShaderExecuteSize(
    workGroupSize: IntVector3,
    dataSize: IntVector3
): IntVector3 = IntVector3(
    workGroupDimensionSize(workGroupSize.x, dataSize.x),
    workGroupDimensionSize(workGroupSize.y, dataSize.y),
    workGroupDimensionSize(workGroupSize.z, dataSize.z)
)

private fun workGroupDimensionSize(
    layout: Int,
    size: Int
) = ceil(size.toDouble() / layout.toDouble()).toInt()

