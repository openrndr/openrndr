package org.openrndr.internal

import org.openrndr.math.Vector2
import org.openrndr.draw.*
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4
import org.openrndr.shape.Rectangle

/**
 * The ImageDrawer class is responsible for rendering 2D images and texture layers onto a drawing surface.
 * It supports rendering images using vertex and instance attributes, enabling efficient handling
 * of multiple render operations. The class utilizes vertex and instance buffers, shaders, and configurable
 * parameters for rendering images within specific geometric regions.
 *
 * The class integrates with the rendering system and operates on `ColorBuffer` and `ArrayTexture` data
 * sources. It supports rendering multiple rectangles or single instances with dynamic resizing of instance
 * attributes based on the number of render requests.
 */
class ImageDrawer {
    private val vertices: VertexBuffer = VertexBuffer.createDynamic(vertexFormat {
        position(3)
        normal(3)
        textureCoordinate(2)
    }, 6, Session.root)

    private var count = 0
    private val instanceFormat = vertexFormat {
        attribute("source", VertexElementType.VECTOR4_FLOAT32)
        attribute("target", VertexElementType.VECTOR4_FLOAT32)
        attribute("layer", VertexElementType.FLOAT32)
    }


    private val singleInstanceAttributes = List(DrawerConfiguration.vertexBufferMultiBufferCount) {
        vertexBuffer(instanceFormat, 1, Session.root)
    }

    private var instanceAttributes = vertexBuffer(instanceFormat, 10, Session.root)

    private val shaderManager: ShadeStyleManager = ShadeStyleManager.fromGenerators(
        "image",
        vsGenerator = Driver.instance.shaderGenerators::imageVertexShader,
        fsGenerator = Driver.instance.shaderGenerators::imageFragmentShader
    )

    private val arrayTextureShaderManager: ShadeStyleManager = ShadeStyleManager.fromGenerators(
        "array-texture",
        vsGenerator = Driver.instance.shaderGenerators::imageArrayTextureVertexShader,
        fsGenerator = Driver.instance.shaderGenerators::imageArrayTextureFragmentShader
    )

    init {
        val w = vertices.shadow.writer()

        w.rewind()
        val pa = Vector3(0.0, 0.0, 0.0)
        val pb = Vector3(1.0, 0.0, 0.0)
        val pc = Vector3(1.0, 1.0, 0.0)
        val pd = Vector3(0.0, 1.0, 0.0)

        val u0 = 0.0
        val u1 = 1.0
        val v0 = 1.0
        val v1 = 0.0

        val ta = Vector2(u0, v1)
        val tb = Vector2(u1, v1)
        val tc = Vector2(u1, v0)
        val td = Vector2(u0, v0)

        val n = Vector3(0.0, 0.0, -1.0)
        w.apply {
            write(pa); write(n); write(ta)
            write(pd); write(n); write(td)
            write(pc); write(n); write(tc)

            write(pc); write(n); write(tc)
            write(pb); write(n); write(tb)
            write(pa); write(n); write(ta)
        }
        vertices.shadow.upload()
    }

    private fun assertInstanceSize(size: Int) {
        if (instanceAttributes.vertexCount < size) {
            Session.root.untrack(instanceAttributes)
            instanceAttributes.destroy()
            instanceAttributes = vertexBuffer(instanceFormat, size, Session.root)
        }
    }

    fun drawImage(
        drawContext: DrawContext, drawStyle: DrawStyle, colorBuffer: ColorBuffer,
        rectangles: List<Pair<Rectangle, Rectangle>>
    ) {
        require(colorBuffer.multisample == BufferMultisample.Disabled) {
            """multisample color buffer $colorBuffer needs to be resolved first"""
        }

        assertInstanceSize(rectangles.size)

        val localInstanceAttributes = if (rectangles.size == 1) {
            singleInstanceAttributes[count.mod(singleInstanceAttributes.size)]
        } else {
            instanceAttributes
        }

        val shader = shaderManager.shader(
            drawStyle.shadeStyle,
            listOf(vertices.vertexFormat),
            listOf(localInstanceAttributes.vertexFormat)
        )

        val iw = localInstanceAttributes.shadow.writer()
        iw.rewind()

        rectangles.forEach {
            val (source, target) = it
            iw.write(
                Vector4(
                    source.corner.x / colorBuffer.width,
                    source.corner.y / colorBuffer.height,
                    source.width / colorBuffer.width,
                    source.height / colorBuffer.height
                )
            )
            iw.write(Vector4(target.corner.x, target.corner.y, target.width, target.height))
            iw.write(0.0f)
        }
        localInstanceAttributes.shadow.uploadElements(0, rectangles.size)

        colorBuffer.bind(0)
        shader.begin()
        drawContext.applyToShader(shader)
        shader.uniform("u_flipV", if (colorBuffer.flipV) 1 else 0)
        drawStyle.applyToShader(shader)
        Driver.instance.setState(drawStyle)
        Driver.instance.drawInstances(
            shader,
            listOf(vertices),
            listOf(localInstanceAttributes) + (drawStyle.shadeStyle?.attributes ?: emptyList()),
            DrawPrimitive.TRIANGLES,
            0,
            6,
            0,
            rectangles.size,
            verticesPerPatch = 0
        )
        shader.end()
        count++
    }

    /**
     * Draws an image using a supplied array texture, associated layer indices, and a list of source and target rectangles.
     *
     * This method utilizes the specified drawing context and style to render the image data from the array texture
     * to specific locations and sizes defined by the target rectangles. It assumes a one-to-one mapping between layers and
     * the source-target rectangles to properly process each layer.
     *
     * @param drawContext The drawing context containing matrices and parameters necessary for rendering.
     * @param drawStyle The style specifications to apply during rendering, including shading and state settings.
     * @param arrayTexture The array texture containing the image data to be drawn.
     * @param layers A list of layer indices corresponding to the layers of the array texture to render.
     * @param rectangles A list of pairs of source rectangles (within the array texture) and target rectangles (on the canvas) used during rendering.
     */
    fun drawImage(
        drawContext: DrawContext, drawStyle: DrawStyle, arrayTexture: ArrayTexture,
        layers: List<Int>, rectangles: List<Pair<Rectangle, Rectangle>>
    ) {

        assertInstanceSize(rectangles.size)
        val shader = arrayTextureShaderManager.shader(
            drawStyle.shadeStyle,
            listOf(vertices.vertexFormat),
            listOf(instanceAttributes.vertexFormat)
        )

        val iw = instanceAttributes.shadow.writer()
        iw.rewind()

        rectangles.forEachIndexed { index, it ->
            val (source, target) = it
            iw.write(
                Vector4(
                    source.corner.x / arrayTexture.width,
                    source.corner.y / arrayTexture.height,
                    source.width / arrayTexture.width,
                    source.height / arrayTexture.height
                )
            )
            iw.write(Vector4(target.corner.x, target.corner.y, target.width, target.height))
            iw.write(layers[index].toFloat())
        }
        instanceAttributes.shadow.uploadElements(0, rectangles.size)

        arrayTexture.bind(0)
        shader.begin()
        drawContext.applyToShader(shader)
        shader.uniform("u_flipV", if (arrayTexture.flipV) 1 else 0)
        drawStyle.applyToShader(shader)
        Driver.instance.setState(drawStyle)
        Driver.instance.drawInstances(
            shader,
            listOf(vertices),
            listOf(instanceAttributes) + (drawStyle.shadeStyle?.attributes ?: emptyList()),
            DrawPrimitive.TRIANGLES,
            0,
            6,
            0,
            rectangles.size,
            verticesPerPatch = 0
        )
        shader.end()
    }

    /**
     * Draws an image from a `ColorBuffer` onto a specified rectangular area on the canvas.
     *
     * @param drawContext The drawing context containing transformation matrices and rendering parameters.
     * @param drawStyle The style settings to apply during rendering, such as shading and state configurations.
     * @param colorBuffer The `ColorBuffer` that contains the image data to be drawn.
     * @param x The x-coordinate of the top-left corner of the target rectangle on the canvas.
     * @param y The y-coordinate of the top-left corner of the target rectangle on the canvas.
     * @param width The width of the target rectangle where the image will be drawn.
     * @param height The height of the target rectangle where the image will be drawn.
     */
    fun drawImage(
        drawContext: DrawContext,
        drawStyle: DrawStyle, colorBuffer: ColorBuffer, x: Double, y: Double, width: Double, height: Double
    ) {
        drawImage(drawContext, drawStyle, colorBuffer, listOf(colorBuffer.bounds to Rectangle(x, y, width, height)))
    }

    /**
     * Draws an image from an `ArrayTexture` onto a specified rectangular area on the canvas.
     *
     * @param drawContext The drawing context containing transformation matrices and rendering parameters.
     * @param drawStyle The style settings to apply during rendering, such as shading and state configurations.
     * @param arrayTexture The array texture that contains the image data to be drawn.
     * @param layer The index of the layer in the array texture to render.
     * @param x The x-coordinate of the top-left corner of the target rectangle on the canvas.
     * @param y The y-coordinate of the top-left corner of the target rectangle on the canvas.
     * @param width The width of the target rectangle where the image will be drawn.
     * @param height The height of the target rectangle where the image will be drawn.
     */
    fun drawImage(
        drawContext: DrawContext,
        drawStyle: DrawStyle,
        arrayTexture: ArrayTexture,
        layer: Int,
        x: Double,
        y: Double,
        width: Double,
        height: Double
    ) {
        drawImage(
            drawContext, drawStyle, arrayTexture, listOf(layer), listOf(
                arrayTexture.bounds to Rectangle(
                    x,
                    y,
                    width,
                    height
                )
            )
        )
    }
}
