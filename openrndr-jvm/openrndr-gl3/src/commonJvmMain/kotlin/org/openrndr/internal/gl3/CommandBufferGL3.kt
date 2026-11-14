package org.openrndr.internal.gl3.org.openrndr.internal.gl3

import org.openrndr.draw.*
import org.openrndr.internal.gl3.ShaderStorageBufferGL43
import org.openrndr.internal.gl3.ShaderStorageBufferShadowGL3
import java.nio.Buffer
import kotlin.reflect.KClass

@JvmRecord
data class CommandGL3(
    override val vertexCount: UInt,
    override val instanceCount: UInt,
    override val baseVertex: Int,
    override val baseInstance: UInt
) : Command

@JvmRecord
data class IndexedCommandGL3(
    override val vertexCount: UInt,
    override val instanceCount: UInt,
    override val firstIndex: UInt,
    override val baseVertex: Int,
    override val baseInstance: UInt
) : IndexedCommand


class CommandBufferGL3<T : CommandBase>(
    val ssbo: ShaderStorageBuffer,
    override val size: UInt,
    override val session: Session?,
    override val type: KClass<T>
) :
    CommandBuffer<T> {

    /**
     * Writes the commands of [source] from the start of the buffer. The layout of the commands is fixed, 4 or 5
     * 32-bit integers, they are written into the shadow directly instead of through a writer that checks the format.
     */
    override fun write(source: List<T>) {
        val shadow = ssbo.shadow as ShaderStorageBufferShadowGL3
        val buffer = shadow.buffer
        (buffer as Buffer).rewind()
        for (i in source.indices) {
            val command = source[i]
            if (command is CommandGL3) {
                buffer.putInt(command.vertexCount.toInt())
                buffer.putInt(command.instanceCount.toInt())
                buffer.putInt(command.baseVertex)
                buffer.putInt(command.baseInstance.toInt())
            } else if (command is IndexedCommandGL3) {
                buffer.putInt(command.vertexCount.toInt())
                buffer.putInt(command.instanceCount.toInt())
                buffer.putInt(command.firstIndex.toInt())
                buffer.putInt(command.baseVertex)
                buffer.putInt(command.baseInstance.toInt())
            }
        }
        val bytes = buffer.position()
        if (bytes > 0) {
            shadow.upload(0, bytes)
        }
    }

    override fun read(): List<T> {
        TODO("Not yet implemented")
    }

    override fun close() {
        session?.untrack(this)
        ssbo.close()
    }
}

inline fun <reified T : CommandBase> CommandBufferGL3(size: UInt, session: Session? = Session.active): CommandBufferGL3<T> {
    val format = shaderStorageFormat {
        struct("Command", "command", size.toInt()) {
            this.primitive("vertexCount", BufferPrimitiveType.UINT32)
            this.primitive("instanceCount", BufferPrimitiveType.UINT32)
            if (T::class == IndexedCommandGL3::class) {
                this.primitive("firstIndex", BufferPrimitiveType.UINT32)
            }
            this.primitive("baseVertex", BufferPrimitiveType.INT32)
            this.primitive("baseInstance", BufferPrimitiveType.UINT32)
        }
    }

    val ssbo = ShaderStorageBufferGL43.create(format, session)
    return CommandBufferGL3(
        ssbo,
        size,
        session,
        T::class
    )
}