package org.openrndr.internal

import org.openrndr.draw.*

/**
 * A utility class for drawing vertex buffers with specified styles, primitives, and attributes.
 *
 * This class manages the rendering process of vertex buffers using specific shaders
 * generated through the associated `ShadeStyleManager`. It provides methods to handle
 * drawing both regular vertex buffers and indexed vertex buffers, as well as drawing
 * with instanced attributes.
 */
class VertexBufferDrawer {

    private val shaderManager: ShadeStyleManager = ShadeStyleManager.fromGenerators(
        "vertex-buffer",
        vsGenerator = Driver.instance.shaderGenerators::vertexBufferVertexShader,
        fsGenerator = Driver.instance.shaderGenerators::vertexBufferFragmentShader
    )

    fun drawVertexBuffer(
        drawContext: DrawContext,
        drawStyle: DrawStyle,
        primitive: DrawPrimitive,
        vertexBuffers: List<VertexBuffer>,
        offset: Int,
        vertexCount: Int
    ) {
        val shader = shaderManager.shader(drawStyle.shadeStyle, vertexBuffers.map { it.vertexFormat })
        shader.begin()
        drawContext.applyToShader(shader)
        drawStyle.applyToShader(shader)
        Driver.instance.setState(drawStyle)
        Driver.instance.drawVertexBuffer(shader, vertexBuffers, primitive, offset, vertexCount)
        shader.end()
    }

    fun drawVertexBuffer(
        drawContext: DrawContext,
        drawStyle: DrawStyle,
        primitive: DrawPrimitive,
        indexBuffer: IndexBuffer,
        vertexBuffers: List<VertexBuffer>,
        offset: Int,
        indexCount: Int
    ) {
        val shader = shaderManager.shader(drawStyle.shadeStyle, vertexBuffers.map { it.vertexFormat })
        shader.begin()
        drawContext.applyToShader(shader)
        drawStyle.applyToShader(shader)
        Driver.instance.setState(drawStyle)
        Driver.instance.drawIndexedVertexBuffer(shader, indexBuffer, vertexBuffers, primitive, offset, indexCount)
        shader.end()
    }

    /**
     * Renders multiple instances of a vertex buffer with attributes applied per instance.
     *
     * This method executes instance-based rendering using a collection of vertex buffers and instance-specific
     * attributes. The rendering settings and behavior are defined by the provided draw context and draw style.
     *
     * @param drawContext The current drawing context containing transformation matrices and other
     *   parameters necessary for rendering.
     * @param drawStyle The style settings including fill, stroke, and other graphical properties
     *   used for rendering.
     * @param primitive The type of drawing primitive (e.g., TRIANGLES, LINES) to use during rendering.
     * @param vertexBuffers The list of vertex buffers containing vertex data such as position, color, or
     *   texture coordinates.
     * @param instanceAttributes The list of vertex buffers containing attribute data specific to each instance
     *   (e.g., transformation matrices or instance-level properties).
     * @param offset The starting offset in the vertex buffer to begin reading vertex data.
     * @param vertexCount The number of vertices to use from the vertex buffers for rendering each instance.
     * @param instanceCount The number of instances of the specified primitive to render.
     */
    fun drawVertexBufferInstances(
        drawContext: DrawContext,
        drawStyle: DrawStyle,
        primitive: DrawPrimitive,
        vertexBuffers: List<VertexBuffer>,
        instanceAttributes: List<VertexBuffer>,
        offset: Int,
        vertexCount: Int,
        instanceCount: Int
    ) {
        val shader = shaderManager.shader(
            drawStyle.shadeStyle,
            vertexBuffers.map { it.vertexFormat },
            instanceAttributes.map { it.vertexFormat })
        shader.begin()
        drawContext.applyToShader(shader)
        drawStyle.applyToShader(shader)
        Driver.instance.setState(drawStyle)
        Driver.instance.drawInstances(
            shader,
            vertexBuffers,
            instanceAttributes + (drawStyle.shadeStyle?.attributes ?: emptyList()),
            primitive,
            offset,
            vertexCount,
            0,
            instanceCount
        )
        shader.end()
    }


    /**
     * Renders multiple instances of a vertex buffer with attributes applied per instance.
     *
     * This method uses the specified index buffer, vertex buffers, and instance attributes to draw
     * multiple instances of graphical primitives. The rendering settings and styling are determined
     * by the provided draw context and draw style.
     *
     * @param drawContext The current drawing context containing transformation matrices and other
     *   parameters necessary for rendering.
     * @param drawStyle The style settings, including fill, stroke, and other graphical properties
     *   used for rendering.
     * @param primitive The type of drawing primitive (e.g., TRIANGLES, LINES) to use during rendering.
     * @param indexBuffer The index buffer containing indices that define how vertices should be combined
     *   to create the specified primitives.
     * @param vertexBuffers The list of vertex buffers containing vertex data such as position, color, or
     *   texture coordinates.
     * @param instanceAttributes The list of vertex buffers containing per-instance attribute data
     *   (e.g., transformations or instance-specific properties).
     * @param offset The starting offset in the index buffer for rendering.
     * @param indexCount The number of indices from the index buffer to be used for rendering.
     * @param instanceCount The number of instances of the specified primitive to render.
     */
    fun drawVertexBufferInstances(
        drawContext: DrawContext,
        drawStyle: DrawStyle,
        primitive: DrawPrimitive,
        indexBuffer: IndexBuffer,
        vertexBuffers: List<VertexBuffer>,
        instanceAttributes: List<VertexBuffer>,
        offset: Int,
        indexCount: Int,
        instanceCount: Int
    ) {
        val shader = shaderManager.shader(
            drawStyle.shadeStyle,
            vertexBuffers.map { it.vertexFormat },
            instanceAttributes.map { it.vertexFormat })
        shader.begin()
        drawContext.applyToShader(shader)
        drawStyle.applyToShader(shader)
        Driver.instance.setState(drawStyle)
        Driver.instance.drawIndexedInstances(
            shader,
            indexBuffer,
            vertexBuffers,
            instanceAttributes + (drawStyle.shadeStyle?.attributes ?: emptyList()),
            primitive,
            offset,
            indexCount,
            0,
            instanceCount
        )
        shader.end()
    }

    fun drawVertexBufferCommands(
        drawContext: DrawContext,
        drawStyle: DrawStyle,
        primitive: DrawPrimitive,
        vertexBuffers: List<VertexBuffer>,
        instanceAttributes: List<VertexBuffer>,
        commandBuffer: CommandBuffer<Command>,
        commandCount: Int,
        commandOffset: Int
    ) {
        val shader = shaderManager.shader(
            drawStyle.shadeStyle,
            vertexBuffers.map { it.vertexFormat },
            instanceAttributes.map { it.vertexFormat })
        shader.begin()
        drawContext.applyToShader(shader)
        drawStyle.applyToShader(shader)
        Driver.instance.setState(drawStyle)
        Driver.instance.drawCommandBuffer(
            shader,
            commandBuffer,
            vertexBuffers,
            instanceAttributes + (drawStyle.shadeStyle?.attributes ?: emptyList()),
            primitive,
            commandCount,
            0,
        )
        shader.end()
    }
}