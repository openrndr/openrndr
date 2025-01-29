package org.openrndr.draw

/**
 * Defines an interface for binding various types of GPU buffers to a shader by name.
 *
 * This interface enables the attachment of different buffer types, such as vertex buffers,
 * shader storage buffers, or atomic counter buffers, to a shader program. The bindings are
 * identified by a string name corresponding to the resources declared in the shader.
 */
interface ShaderBufferBindings {
    /**
     * Binds a vertex buffer to a shader using the specified name.
     *
     * This method attaches a vertex buffer to the specified binding point in a shader program,
     * identified by a unique name. The vertex buffer must conform to the format and attributes
     * expected by the shader.
     *
     * @param name The name of the binding point in the shader to which the vertex buffer will be attached.
     *             This should match the resource name declared in the shader code.
     * @param vertexBuffer The vertex buffer to bind to the shader. It contains vertex data
     *                     and conforms to a specific vertex format required for rendering or processing.
     */
    fun buffer(name:String, vertexBuffer: VertexBuffer)


    /**
     * Binds a shader storage buffer to a shader using the specified name.
     *
     * This method attaches a shader storage buffer to the specified binding point in a shader program,
     * identified by a unique name. The shader storage buffer enables large amounts of mutable data
     * to be shared between the host application and the shader.
     *
     * @param name The name of the binding point in the shader to which the shader storage buffer will be attached.
     *             This should match the resource name declared in the shader code.
     * @param shaderStorageBuffer The shader storage buffer to bind to the shader. It represents
     *                            a GPU buffer that allows shaders to read and write data.
     */
    fun buffer(name:String, shaderStorageBuffer: ShaderStorageBuffer)


    /**
     * Binds an atomic counter buffer to a shader using the specified name.
     *
     * This method attaches an atomic counter buffer to the specified binding point in a shader program,
     * identified by a unique name. Atomic counter buffers allow shaders to perform atomic operations
     * on counters stored in the buffer, enabling efficient inter-thread communication within the shader.
     *
     * @param name The name of the binding point in the shader to which the atomic counter buffer will be attached.
     *             This should match the resource name declared in the shader code.
     * @param counterBuffer The atomic counter buffer to bind to the shader. It contains counters that
     *                      support atomic operations for use in shader programs.
     */
    fun buffer(name:String, counterBuffer: AtomicCounterBuffer)
}