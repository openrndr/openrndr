package org.openrndr.internal

import org.openrndr.math.Vector2
import org.openrndr.draw.*
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4
import org.openrndr.platform.Platform
import org.openrndr.platform.PlatformType
import org.openrndr.shape.Rectangle

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

        val instanceAttributes = if (rectangles.size == 1) {
            singleInstanceAttributes[count.mod(singleInstanceAttributes.size)]
        } else {
            instanceAttributes
        }

        val shader = shaderManager.shader(
            drawStyle.shadeStyle,
            listOf(vertices.vertexFormat),
            listOf(instanceAttributes.vertexFormat)
        )

        val iw = instanceAttributes.shadow.writer()
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
        instanceAttributes.shadow.uploadElements(0, rectangles.size)

        colorBuffer.bind(0)
        shader.begin()
        drawContext.applyToShader(shader)
        shader.uniform("u_flipV", if (colorBuffer.flipV) 1 else 0)
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
        count++
    }

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

    fun drawImage(
        drawContext: DrawContext,
        drawStyle: DrawStyle, colorBuffer: ColorBuffer, x: Double, y: Double, width: Double, height: Double
    ) {
        drawImage(drawContext, drawStyle, colorBuffer, listOf(colorBuffer.bounds to Rectangle(x, y, width, height)))
    }

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
