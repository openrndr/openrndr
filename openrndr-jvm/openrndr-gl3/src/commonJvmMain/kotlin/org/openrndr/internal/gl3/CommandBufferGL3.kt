package org.openrndr.internal.gl3.org.openrndr.internal.gl3

import org.openrndr.draw.BufferPrimitiveType
import org.openrndr.draw.Command
import org.openrndr.draw.CommandBase
import org.openrndr.draw.CommandBuffer
import org.openrndr.draw.IndexedCommand
import org.openrndr.draw.Session
import org.openrndr.draw.ShaderStorageBuffer
import org.openrndr.draw.shaderStorageFormat
import org.openrndr.internal.gl3.ShaderStorageBufferGL43

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
    override val session: Session?
) :
    CommandBuffer<T> {

    override fun write(source: List<T>) {
        ssbo.put {
            for (command in source) {
                if (command is CommandGL3) {
                    write(command.vertexCount)
                    write(command.instanceCount)
                    write(command.baseVertex)
                    write(command.baseInstance)
                } else if (command is IndexedCommandGL3) {
                    write(command.vertexCount)
                    write(command.instanceCount)
                    write(command.firstIndex)
                    write(command.baseVertex)
                    write(command.baseInstance)
                }
            }
        }
    }

    override fun read(): List<T> {
        TODO("Not yet implemented")
    }

    override fun close() {
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
        session
    )
}