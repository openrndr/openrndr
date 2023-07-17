package org.openrndr.draw

import org.openrndr.internal.Driver
import org.openrndr.math.*

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