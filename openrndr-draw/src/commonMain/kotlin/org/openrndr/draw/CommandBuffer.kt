package org.openrndr.draw

import org.openrndr.internal.Driver

/**
 * Represents a generic command that contains parameters typically used for rendering operations.
 *
 * @property vertexCount The number of vertices to be rendered.
 * @property instanceCount The number of instances to be rendered.
 * @property firstIndex The starting index for indexed rendering.
 * @property baseVertex The offset added to each vertex index during rendering.
 * @property baseInstance The offset added to each instance index during rendering.
 */
sealed interface CommandBase

interface Command: CommandBase {
    val vertexCount: UInt
    val instanceCount: UInt
    val baseVertex: Int
    val baseInstance: UInt
}
interface IndexedCommand: CommandBase {
    val vertexCount: UInt
    val instanceCount: UInt
    val firstIndex: UInt
    val baseVertex: Int
    val baseInstance: UInt
}


fun command(vertexCount: UInt, instanceCount: UInt = 1u,  baseVertex: Int = 0, baseInstance: UInt = 0u) : Command {
    return Driver.instance.createCommand(vertexCount, instanceCount, baseVertex, baseInstance)
}


fun indexedCommand(vertexCount: UInt, instanceCount: UInt = 1u, baseVertex: Int = 0, firstIndex: UInt, baseInstance: UInt = 0u): IndexedCommand {
    return Driver.instance.createIndexedCommand(
        vertexCount,
        instanceCount,
        firstIndex,
        baseVertex,
        baseInstance)
}

/**
 * Represents a buffer for storing and managing graphical commands. A `CommandBuffer`
 * provides functionality for writing commands to the buffer and reading them in bulk.
 */
interface CommandBuffer<T: CommandBase>: AutoCloseable {
    val size: UInt
    fun write(source: List<T>)
    fun read(): List<T>
    val session: Session?
}