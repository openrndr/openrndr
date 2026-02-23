package org.openrndr.internal.glcommon

import io.github.oshai.kotlinlogging.KotlinLogging
import org.openrndr.draw.*
import org.openrndr.internal.Driver

private val logger = KotlinLogging.logger {  }

fun ComputeStyle.structure(): ComputeStructure {
    return ComputeStructure(
        structDefinitions = structDefinitions(),
        uniforms = uniforms(),
        buffers = listOf(buffers(), images()).joinToString("\n"),
        computeTransform = computeTransform,
        computePreamble = computePreamble,
        workGroupSize = workGroupSize
    )
}

class ComputeStyleManagerGLCommon : ComputeStyleManager(), StyleManagerDispatchUniform,
    StyleManagerDispatchBufferBindings, StyleManagerDispatchImageBindings {

    val shaders = mutableMapOf<ComputeStructure, ComputeShader>()
    override var textureIndex: Int = 2
    override var imageIndex: Int = 0

    override fun shader(style: ComputeStyle, name: String): ComputeShader {
        val structure = style.structure()
        val shader = shaders.getOrPut(structure) {
            val code = """${Driver.instance.shaderConfiguration(ShaderType.COMPUTE)}
layout(local_size_x = ${structure.workGroupSize.x}, local_size_y = ${structure.workGroupSize.y}, local_size_z = ${structure.workGroupSize.z}) in;

${structure.structDefinitions ?: ""}
${structure.uniforms ?: ""}
${structure.buffers ?: ""}

${structure.computePreamble}

void main() {
${structure.computeTransform.prependIndent("    ")}        
}"""
            val computeShader = Driver.instance.createComputeShader(code, name)
            logger.debug { "created compute shader '$name', ${computeShader} " }
            computeShader
        }

//        logger.debug { "dispatching buffer bindings for '$name', '$shader'" }
        dispatchBufferBindings(style, shader)
  //      logger.debug { "dispatched uniforms for shader '$name', '$shader'" }
        dispatchParameters(style, shader, shader.textureBindings)
//        logger.debug { "dispatched image bindings for shader '$name', '$shader'" }
        dispatchImageBindings(style, shader)

        return shader
    }
}