package org.openrndr.draw

import org.openrndr.internal.Driver
import kotlin.reflect.KClass

/**
 * A sealed interface that serves as a base for all command types within the system.
 *
 * Implementations of this interface represent distinct commands that can be processed
 * or executed by the application.
 */
sealed interface CommandBase

/**
 * Represents a command with specific parameters that can be processed or executed.
 *
 * This interface inherits from CommandBase and provides additional properties
 * defining key attributes of the command.
 *
 * Implementations of this interface represent commands that operate based on vertex and
 * instance data, providing control over rendering or computational parameters.
 *
 * @property vertexCount The number of vertices to be processed by the command.
 * @property instanceCount The number of instances to be processed by the command.
 * @property baseVertex The starting vertex index for the command.
 * @property baseInstance The starting instance index for the command.
 */
interface Command: CommandBase {
    val vertexCount: UInt
    val instanceCount: UInt
    val baseVertex: Int
    val baseInstance: UInt
}

/**
 * Represents a command with indexed geometry data, allowing the application to process or execute
 * multi-instance rendering commands efficiently.
 *
 * This interface defines the necessary properties required to handle indexed drawing commands,
 * typically used in graphics or rendering pipelines. Implementers can utilize these properties
 * to specify the parameters for an indexed drawing operation.
 *
 * - `vertexCount`: The total number of vertices to be processed.
 * - `instanceCount`: The number of instances for instanced rendering.
 * - `firstIndex`: The starting index within the index buffer.
 * - `baseVertex`: The base offset for vertex fetching.
 * - `baseInstance`: The base instance ID used for instanced rendering.
 */
interface IndexedCommand: CommandBase {
    val vertexCount: UInt
    val instanceCount: UInt
    val firstIndex: UInt
    val baseVertex: Int
    val baseInstance: UInt
}

/**
 * Creates a new command with the specified vertex and instance parameters.
 *
 * @param vertexCount The number of vertices to be processed by the command.
 * @param instanceCount The number of instances to be processed by the command. Defaults to 1.
 * @param baseVertex The starting vertex index for the command. Defaults to 0.
 * @param baseInstance The starting instance index for the command. Defaults to 0.
 * @return A new command instance configured with the provided parameters.
 */
fun command(vertexCount: UInt, instanceCount: UInt = 1u,  baseVertex: Int = 0, baseInstance: UInt = 0u) : Command {
    return Driver.instance.createCommand(vertexCount, instanceCount, baseVertex, baseInstance)
}

/**
 * Creates an indexed command for processing or executing multi-instance rendering commands efficiently.
 *
 * @param vertexCount The total number of vertices to be processed.
 * @param instanceCount The number of instances for instanced rendering. Defaults to 1.
 * @param baseVertex The base offset for vertex fetching. Defaults to 0.
 * @param firstIndex The starting index within the index buffer.
 * @param baseInstance The base instance ID used for instanced rendering. Defaults to 0.
 * @return An instance of [IndexedCommand] containing the specified rendering parameters.
 */

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
    val type: KClass<T>
}

/**
 * Creates and returns a new instance of a command buffer with the specified size and session.
 *
 * @param size The size of the command buffer, specified as an unsigned integer.
 * @param session The session to associate with the command buffer. If no session is provided, it defaults to the active session.
 * @return A new instance of `CommandBuffer<Command>` configured with the provided parameters.
 */
fun commandBuffer(size: UInt, session: Session? = Session.active): CommandBuffer<Command> = Driver.instance.createCommandBuffer(2u, session)

/**
 * Creates an indexed command buffer with the specified size and session.
 *
 * @param size The size of the command buffer to be created, expressed as an unsigned integer.
 * @param session An optional session context in which the command buffer will be created. Defaults to the active session.
 * @return A `CommandBuffer` containing `IndexedCommand` objects.
 */
fun indexedCommandBuffer(size: UInt, session: Session? = Session.active): CommandBuffer<IndexedCommand> = Driver.instance.createIndexedCommandBuffer(2u, session)