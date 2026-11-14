package org.openrndr.internal.glcommon

import org.openrndr.draw.*

interface StyleManagerDispatchBufferBindings {
    /** the names of the bindings of buffers */
    val uniformNames: UniformNames

    fun <T> dispatchBufferBindings(style: StyleBufferBindings, shader: T) where T: ShaderBufferBindings {
        val bufferValues = style.bufferValues
        if (bufferValues.isEmpty()) return
        for (it in bufferValues.entries) {
            when (val value = it.value) {
                is StructuredBuffer<*> -> {
                    shader.buffer(uniformNames.shaderStorageBlock(it.key), value.ssbo)
                }
                is ShaderStorageBuffer -> {
                    shader.buffer(uniformNames.shaderStorageBlock(it.key), value)
                }
                is AtomicCounterBuffer -> {
                    shader.buffer(uniformNames.atomicCounter(it.key), value)
                }
                is IndexBuffer -> {
                    shader.buffer(uniformNames.shaderStorageBlock(it.key), value)
                }
            }
        }
    }
}