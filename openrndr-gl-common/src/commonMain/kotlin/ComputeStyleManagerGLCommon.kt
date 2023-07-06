package org.openrndr.internal.glcommon

import org.openrndr.draw.*
import org.openrndr.internal.Driver

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
            val code = """#version 450 core
layout(local_size_x = ${structure.workGroupSize.x}, local_size_y = ${structure.workGroupSize.y}, local_size_z = ${structure.workGroupSize.z}) in;

${structure.structDefinitions ?: ""}
${structure.uniforms ?: ""}
${structure.buffers ?: ""}

${structure.computePreamble}

void main() {
${structure.computeTransform.prependIndent("    ")}        
}"""
            Driver.instance.createComputeShader(code, name)
        }

        dispatchBufferBindings(style, shader)
        dispatchParameters(style, shader)
        dispatchImageBindings(style, shader)

        return shader
    }
}