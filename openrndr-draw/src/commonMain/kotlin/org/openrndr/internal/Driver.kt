@file:JvmName("DriverJVM")

package org.openrndr.internal

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import kotlin.jvm.JvmName


sealed class ShaderLanguage
class GLSL(val version: String) : ShaderLanguage()
class WebGLSL(val version: String) : ShaderLanguage()

/**
 * Represents the properties and capabilities of a graphics driver.
 *
 * @property maxRenderTargetSamples The maximum number of samples supported for render targets.
 * @property maxTextureSamples The maximum number of samples supported for textures.
 * @property maxTextureSize The maximum size (in pixels) supported for textures, typically referring to the largest dimension.
 */
data class DriverProperties(
    val maxRenderTargetSamples: Int,
    val maxTextureSamples: Int,
    val maxTextureSize: Int
)


/**
 * Driver interface. This is the internal interface
 */
expect interface Driver {

    /**
     * Represents the configuration properties for a driver.
     * This object encapsulates various settings and preferences
     * related to the driver's behavior, connection settings,
     * and other operational parameters.
     */
    val properties: DriverProperties

    val contextID: Long
    /**
     * Create a shader from code
     * @param vsCode vertex shader code
     * @param gsCode optional geometry shader code
     * @param fsCode fragment shader code
     */

    fun createShader(
        vsCode: String,
        tcsCode: String?,
        tesCode: String?,
        gsCode: String?,
        fsCode: String,
        name: String,
        session: Session? = Session.active
    ): Shader

    /**
     * Creates a compute shader from the provided source code.
     *
     * @param code The GLSL source code for the compute shader.
     * @param name A name used to identify the compute shader, primarily for debugging or logging purposes.
     * @param session The session associated with this compute shader. Defaults to the currently active session if not provided.
     * @return A ComputeShader instance created from the provided source code.
     */
    fun createComputeShader(code: String, name: String, session: Session? = Session.active): ComputeShader

    /**
     * Creates and returns a new instance of ComputeStyleManager. This manager is used to handle styles
     * for compute operations.
     *
     * @param session The session associated with the ComputeStyleManager. Defaults to the root session if not provided.
     * @return A new instance of ComputeStyleManager.
     */
    fun createComputeStyleManager(session: Session? = Session.root) : ComputeStyleManager

    /**
     * Creates a new instance of `ShadeStyleManager` used to manage shade styles.
     *
     * @param name The name of the `ShadeStyleManager`.
     * @param vsGenerator A function generating the vertex shader code from a `ShadeStructure`.
     * @param tcsGenerator An optional function generating the tessellation control shader code from a `ShadeStructure`.
     * @param tesGenerator An optional function generating the tessellation evaluation shader code from a `ShadeStructure`.
     * @param gsGenerator An optional function generating the geometry shader code from a `ShadeStructure`.
     * @param fsGenerator A function generating the fragment shader code from a `ShadeStructure`.
     * @param session The session associated with the `ShadeStyleManager`. Defaults to the root session if not provided.
     * @return A new instance of `ShadeStyleManager`.
     */
    fun createShadeStyleManager(
        name: String,
        vsGenerator: (ShadeStructure) -> String,
        tcsGenerator: ((ShadeStructure) -> String)? = null,
        tesGenerator: ((ShadeStructure) -> String)? = null,
        gsGenerator: ((ShadeStructure) -> String)? = null,
        fsGenerator: (ShadeStructure) -> String,
        session: Session? = Session.root
    ): ShadeStyleManager

    /**
     * Creates a render target with the specified dimensions, content scale, and multisampling options.
     *
     * @param width The width of the render target in pixels.
     * @param height The height of the render target in pixels.
     * @param contentScale The content scale factor, used to scale the render target resolution. Defaults to 1.0.
     * @param multisample The multisampling configuration for the render target. Defaults to `BufferMultisample.Disabled`.
     * @param session The session associated with this render target. Defaults to the currently active session.
     * @return A new instance of `RenderTarget` configured with the specified parameters.
     */
    fun createRenderTarget(
        width: Int,
        height: Int,
        contentScale: Double = 1.0,
        multisample: BufferMultisample = BufferMultisample.Disabled,
        session: Session? = Session.active
    ): RenderTarget

    /**
     * Creates an array cubemap texture with the specified dimensions, format, type, and number of mipmap levels.
     *
     * @param width The width of each cubemap face in pixels.
     * @param layers The number of layers in the array cubemap.
     * @param format The color format of the texture (e.g., RGB, RGBA).
     * @param type The color type of the texture (e.g., unsigned byte, float).
     * @param levels The number of mipmap levels for the texture. Defaults to 1.
     * @param session The session associated with this texture. Defaults to the currently active session.
     * @return A new instance of `ArrayCubemap` configured with the specified parameters.
     */
    fun createArrayCubemap(
        width: Int,
        layers: Int,
        format: ColorFormat,
        type: ColorType,
        levels: Int = 1,
        session: Session? = Session.active
    ): ArrayCubemap

    /**
     * Creates a 2D array texture with the specified dimensions, format, type, and number of mipmap levels.
     *
     * @param width The width of the texture in pixels.
     * @param height The height of the texture in pixels.
     * @param layers The number of layers in the array texture.
     * @param format The color format of the texture (e.g., RGB, RGBA).
     * @param type The color type of the texture (e.g., unsigned byte, float).
     * @param levels The number of mipmap levels for the texture. Defaults to 1.
     * @param session The session associated with this texture. Defaults to the currently active session.
     * @return A new instance of `ArrayTexture` configured with the specified parameters.
     */
    fun createArrayTexture(
        width: Int,
        height: Int,
        layers: Int,
        format: ColorFormat,
        type: ColorType,
        levels: Int = 1,
        session: Session? = Session.active
    ): ArrayTexture

    /**
     * Creates an atomic counter buffer with the specified number of counters.
     *
     * @param counterCount The number of atomic counters to include in the buffer.
     * @param session The session associated with this atomic counter buffer. Defaults to the currently active session if not provided.
     * @return A new instance of `AtomicCounterBuffer` configured with the specified number of counters.
     */
    fun createAtomicCounterBuffer(counterCount: Int, session: Session? = Session.active): AtomicCounterBuffer

    /**
     * Creates a color buffer with the specified dimensions, format, and other parameters.
     *
     * @param width The width of the color buffer in pixels.
     * @param height The height of the color buffer in pixels.
     * @param contentScale The scale factor for DPI adjustments.
     * @param format The color format of the buffer (e.g., RGB, RGBA).
     * @param type The color type that defines the data type for each component (e.g., UINT8, FLOAT).
     * @param multisample The multisampling configuration for the buffer, default is no multisampling.
     * @param levels The number of mipmap levels to create, default is 1.
     * @param session The session to which this buffer belongs, default is the currently active session.
     * @return A newly created ColorBuffer instance with the defined specifications.
     */
    fun createColorBuffer(
        width: Int,
        height: Int,
        contentScale: Double,
        format: ColorFormat,
        type: ColorType,
        multisample: BufferMultisample = BufferMultisample.Disabled,
        levels: Int = 1,
        session: Session? = Session.active
    ): ColorBuffer

    /**
     * Creates a depth buffer with the specified dimensions, format, and additional configuration.
     *
     * @param width The width of the depth buffer in pixels.
     * @param height The height of the depth buffer in pixels.
     * @param format The format of the depth buffer, defining its precision and characteristics.
     * @param multisample An optional setting to enable or disable multisampling for anti-aliasing. Defaults to no multisampling.
     * @param session The session associated with this depth buffer. Defaults to the active session if not provided.
     * @return A newly created DepthBuffer instance configured with the provided parameters.
     */
    fun createDepthBuffer(
        width: Int,
        height: Int,
        format: DepthFormat,
        multisample: BufferMultisample = BufferMultisample.Disabled,
        session: Session? = Session.active
    ): DepthBuffer

    /**
     * Creates a buffer texture for storing and operating on image or pixel data.
     *
     * @param elementCount The number of elements in the buffer texture.
     * @param format The color format of the buffer texture.
     * @param type The color type of the buffer texture.
     * @param session The session associated with this buffer texture. Defaults to the active session.
     * @return A newly created buffer texture with the specified properties.
     */
    fun createBufferTexture(
        elementCount: Int,
        format: ColorFormat,
        type: ColorType,
        session: Session? = Session.active
    ): BufferTexture

    /**
     * Creates a cubemap with the specified dimensions, format, type, mipmap levels, and session.
     *
     * @param width The width of each face of the cubemap in pixels.
     * @param format The color format of the cubemap.
     * @param type The data type of the cubemap's color information.
     * @param levels The number of mipmap levels to generate for the cubemap.
     * @param session The session within which the cubemap is created. Defaults to the active session if not specified.
     * @return A new instance of a cubemap with the specified properties.
     */
    fun createCubemap(
        width: Int,
        format: ColorFormat,
        type: ColorType,
        levels: Int,
        session: Session? = Session.active
    ): Cubemap

    /**
     * Creates a 3D volume texture with the specified dimensions, format, and type.
     *
     * @param width The width of the volume texture in pixels.
     * @param height The height of the volume texture in pixels.
     * @param depth The depth of the volume texture in layers.
     * @param format The color format of the texture.
     * @param type The color type of the texture.
     * @param levels The number of mipmap levels to allocate for the texture.
     * @param session The session to associate this texture with, defaults to the active session if not specified.
     * @return The created volume texture object.
     */
    fun createVolumeTexture(
        width: Int,
        height: Int,
        depth: Int,
        format: ColorFormat,
        type: ColorType,
        levels: Int,
        session: Session? = Session.active
    ): VolumeTexture


    /**
     * Clears the current graphical context with the specified color.
     *
     * @param color The color value of type ColorRGBa used to clear the graphical context.
     */
    fun clear(color: ColorRGBa)


    /**
     * Creates a dynamic vertex buffer with the specified format and vertex count.
     *
     * @param format The vertex format that defines the layout and attributes of the vertices in the buffer.
     * @param vertexCount The number of vertices the buffer will hold.
     * @param session The session to associate with the buffer. Defaults to the currently active session if not specified.
     * @return A dynamic vertex buffer initialized with the specified format and vertex count.
     */
    fun createDynamicVertexBuffer(
        format: VertexFormat,
        vertexCount: Int,
        session: Session? = Session.active
    ): VertexBuffer

//    fun createStaticVertexBuffer(format: VertexFormat, buffer: Buffer, session: Session? = Session.active): VertexBuffer

    /**
     * Creates a dynamic index buffer with the specified number of elements and index type, optionally associated with a session.
     *
     * @param elementCount The number of indices to be stored in the buffer.
     * @param type The data type of the indices in the buffer (e.g., unsigned short, unsigned int).
     * @param session An optional session to associate the buffer with. Defaults to the currently active session if not provided.
     * @return An instance of [IndexBuffer] representing the created dynamic index buffer.
     */
    fun createDynamicIndexBuffer(elementCount: Int, type: IndexType, session: Session? = Session.active): IndexBuffer

    /**
     * Creates a shader storage buffer with the specified format and session.
     *
     * @param format The format of the shader storage buffer, defining its structure and layout.
     * @param session The session to which the buffer will be bound. If not provided, the active session is used.
     * @return A new instance of ShaderStorageBuffer configured with the given format and session.
     */
    fun createShaderStorageBuffer(format: ShaderStorageFormat, session: Session? = Session.active): ShaderStorageBuffer

    /**
     * Draws content from multiple vertex buffers using the specified shader and draw primitive.
     *
     * @param shader The shader to be used for rendering.
     * @param vertexBuffers A list of vertex buffers to source vertex data from.
     * @param drawPrimitive The primitive type that determines how the vertices are rendered (e.g., triangle, line, etc.).
     * @param vertexOffset The starting offset in the vertex buffer from which to begin rendering.
     * @param vertexCount The number of vertices to render.
     * @param verticesPerPatch The number of vertices per patch, used in patch-based rendering. Defaults to 0.
     */
    fun drawVertexBuffer(
        shader: Shader, vertexBuffers: List<VertexBuffer>,
        drawPrimitive: DrawPrimitive,
        vertexOffset: Int, vertexCount: Int, verticesPerPatch: Int = 0
    )

    /**
     * Renders geometry from the provided indexed vertex buffer using the specified shader and draw primitive.
     *
     * @param shader The shader program to use for rendering.
     * @param indexBuffer The index buffer that specifies the order of vertices for drawing.
     * @param vertexBuffers A list of vertex buffers containing vertex data used for rendering.
     * @param drawPrimitive The type of primitive (e.g., triangles, lines) to render.
     * @param indexOffset The starting index in the index buffer for rendering.
     * @param indexCount The number of indices to be rendered.
     * @param verticesPerPatch The number of vertices in each patch when rendering with tessellation. Defaults to 0.
     */
    fun drawIndexedVertexBuffer(
        shader: Shader, indexBuffer: IndexBuffer, vertexBuffers: List<VertexBuffer>,
        drawPrimitive: DrawPrimitive,
        indexOffset: Int, indexCount: Int, verticesPerPatch: Int = 0
    )

    /**
     * Renders multiple instances of a set of primitives using the provided shader and vertex data.
     *
     * @param shader The Shader object used to render the instances.
     * @param vertexBuffers A list of VertexBuffer objects containing vertex data for the primitives.
     * @param instanceAttributes A list of VertexBuffer objects containing per-instance attributes.
     * @param drawPrimitive The type of primitive to draw (e.g., triangles, lines, etc.).
     * @param vertexOffset The offset into the vertex buffer to start drawing.
     * @param vertexCount The number of vertices to draw.
     * @param instanceOffset The offset into the instance buffer to start rendering instances.
     * @param instanceCount The number of instances to render.
     * @param verticesPerPatch The number of vertices per patch, used for tessellation. Defaults to 0.
     */
    fun drawInstances(
        shader: Shader, vertexBuffers: List<VertexBuffer>,
        instanceAttributes: List<VertexBuffer>,
        drawPrimitive: DrawPrimitive, vertexOffset: Int, vertexCount: Int,
        instanceOffset: Int, instanceCount: Int, verticesPerPatch: Int = 0
    )

    /**
     * Renders instances of geometry using indexed drawing.
     *
     * @param shader The shader program used to render the instances.
     * @param indexBuffer The index buffer containing the indices of the vertices to be used for drawing.
     * @param vertexBuffers A list of vertex buffers providing vertex attributes for the geometry.
     * @param instanceAttributes A list of vertex buffers providing attributes specific to each instance.
     * @param drawPrimitive The type of primitive to be drawn (e.g., triangles, lines, or patches).
     * @param indexOffset The starting offset in the index buffer for drawing.
     * @param indexCount The number of indices to be read from the index buffer for drawing.
     * @param instanceOffset The starting offset for the instance data.
     * @param instanceCount The number of instances to be drawn.
     * @param verticesPerPatch The number of vertices per patch, applicable if using patch-based primitives.
     */
    fun drawIndexedInstances(
        shader: Shader, indexBuffer: IndexBuffer, vertexBuffers: List<VertexBuffer>,
        instanceAttributes: List<VertexBuffer>,
        drawPrimitive: DrawPrimitive, indexOffset: Int, indexCount: Int,
        instanceOffset: Int, instanceCount: Int, verticesPerPatch: Int = 0
    )

    /**
     * Updates the current state with the provided drawing style.
     *
     * @param drawStyle The drawing style to set as the current state.
     */
    fun setState(drawStyle: DrawStyle)

    /**
     * Destroys the specified context, releasing any resources associated with it.
     *
     * @param context The identifier of the context to be destroyed.
     */
    fun destroyContext(context: Long)

    val fontImageMapManager: FontMapManager
    val fontVectorMapManager: FontMapManager
    val shaderGenerators: ShaderGenerators
    val activeRenderTarget: RenderTarget

    /**
     * waits for all drawing to complete
     */
    fun finish()

    fun internalShaderResource(resourceId: String): String

    /**
     * Configures and returns the appropriate shader settings based on the specified shader type.
     *
     * @param type The type of shader for which the configuration is required.
     * @return A string representing the configured shader settings for the given shader type.
     */
    fun shaderConfiguration(type: ShaderType): String

    /**
     * Represents a shader language used in graphical programming or rendering pipelines.
     * This variable holds the specific type or version of the shader language being utilized.
     * This variable plays a crucial role in identifying the type of shader syntax to use for compiling and running shaders.
     */
    val shaderLanguage: ShaderLanguage

    companion object {
        var driver: Driver?
        val instance: Driver
    }
}