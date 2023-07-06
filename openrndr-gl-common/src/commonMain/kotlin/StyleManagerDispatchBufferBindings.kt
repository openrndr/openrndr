package org.openrndr.internal.glcommon

import org.openrndr.draw.*

interface StyleManagerDispatchBufferBindings {
    fun <T> dispatchBufferBindings(style: StyleBufferBindings, shader: T) where T: ShaderBufferBindings {
        for (it in style.bufferValues.entries) {
            when (val value = it.value) {
                is StructuredBuffer<*> -> {
                    shader.buffer("B_${it.key}", value.ssbo)
                }
                is ShaderStorageBuffer -> {
                    shader.buffer("B_${it.key}", value)
                }
                is AtomicCounterBuffer -> {
                    shader.buffer("b_${it.key}[0]", value)
                }
                else -> error("unsupported buffer type $value")
            }
        }
    }
}