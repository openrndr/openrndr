package org.openrndr.draw

import org.openrndr.draw.font.BufferAccess
import org.openrndr.draw.font.BufferFlag
import kotlin.jvm.JvmName

interface StyleBufferBindings {

    var bufferValues: MutableMap<String, Any>
    val buffers: MutableMap<String, String>
    val bufferTypes: MutableMap<String, String>
    val bufferFlags: MutableMap<String, Set<BufferFlag>>
    val bufferAccess: MutableMap<String, BufferAccess>

    fun buffer(name: String, buffer: IndexBuffer) {
        bufferValues[name] = buffer
        buffers[name] = "IndexBuffer${buffer.type.name}"
    }

//    fun buffer(name: String, buffer: VertexBuffer) {
//
//    }


    @Suppress("INAPPLICABLE_JVM_NAME") // https://youtrack.jetbrains.com/issue/KT-31420
    @JvmName("bufferCommand")
    fun buffer(name: String, buffer: CommandBuffer<Command>) {
        bufferValues[name] = buffer
        buffers[name] = "CommandBuffer"
    }

    @Suppress("INAPPLICABLE_JVM_NAME") // https://youtrack.jetbrains.com/issue/KT-31420
    @JvmName("bufferIndexedCommand")
    fun buffer(name: String, buffer: CommandBuffer<IndexedCommand>) {
        bufferValues[name] = buffer
        buffers[name] = "IndexedCommandBuffer"
    }

    fun buffer(name: String, buffer: ShaderStorageBuffer) {
        bufferValues[name] = buffer
        buffers[name] = buffer.format.hashCode().toString()
    }

    fun buffer(name: String, buffer: AtomicCounterBuffer) {
        bufferValues[name] = buffer
        buffers[name] = "AtomicCounterBuffer"
    }
}

inline fun <reified T : Struct<T>> StyleBufferBindings.registerStructuredBuffer(
    name: String,
    access: BufferAccess = BufferAccess.READ_WRITE,
    flags: Set<BufferFlag> = emptySet()
) {
    bufferTypes[name] = "struct ${T::class.simpleName}"
    bufferFlags[name] = flags
    bufferAccess[name] = access
}

inline fun <reified T : Struct<T>> StyleBufferBindings.structuredBuffer(name: String, buffer: StructuredBuffer<T>) {
    bufferValues[name] = buffer
    buffers[name] = buffer.ssbo.format.hashCode().toString()
    bufferTypes[name] = "struct ${T::class.simpleName}"
}
