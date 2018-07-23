package org.openrndr.internal

import org.openrndr.draw.*

class CharacterRectangle(val character: Char, val x: Double, val y: Double, val width: Double, val height: Double)

class FontImageMapDrawer {

    private val shaderManager: ShadeStyleManager = ShadeStyleManager.fromGenerators(Driver.instance.shaderGenerators::fontImageMapVertexShader,
            Driver.instance.shaderGenerators::fontImageMapFragmentShader)

    private val vertices = VertexBuffer.createDynamic(VertexFormat().apply {
        textureCoordinate(2)
        attribute("bounds", 4, VertexElementType.FLOAT32)
        position(3)
    }, 6 * 8000)

    private var quads = 0
    fun drawText(context: DrawContext, drawStyle: DrawStyle, text: String, x: Double, y: Double) {
        (drawStyle.fontMap as? FontImageMap)?.let { fontMap ->
            var cursorX = 0.0
            var cursorY = 0.0

            val bw = vertices.shadow.writer()
            bw.position = vertices.vertexFormat.size * quads * 6

            text.forEach {
                val metrics = fontMap.glyphMetrics[it] ?: fontMap.glyphMetrics[' ']!!
                insertCharacterQuad(fontMap, bw, it, x + cursorX + metrics.leftSideBearing , y + cursorY + metrics.yBitmapShift / fontMap.contentScale)
                cursorX += metrics.advanceWidth
            }
            flush(context, drawStyle)
        }
    }

    fun queueText(fontMap: FontMap, text: String, x: Double, y: Double, tracking: Double = 0.0) {
        val bw = vertices.shadow.writer()
        bw.position = vertices.vertexFormat.size * quads * 6
        fontMap as FontImageMap

        var cursorX = 0.0
        var cursorY = 0.0
        text.forEach {
            val metrics = fontMap.glyphMetrics[it]
            metrics?.let { m ->
                insertCharacterQuad(fontMap, bw, it, x + cursorX + m.leftSideBearing / fontMap.contentScale, y + cursorY + m.yBitmapShift / fontMap.contentScale)
                cursorX += m.advanceWidth + tracking
            }
        }
    }


    fun flush(context: DrawContext, drawStyle: DrawStyle) {
        if (quads > 0) {
            vertices.shadow.uploadElements(0, quads * 6)
            val shader = shaderManager.shader(drawStyle.shadeStyle, vertices.vertexFormat)
            shader.begin()
            context.applyToShader(shader)

            Driver.instance.setState(drawStyle)
            drawStyle.applyToShader(shader)
            (drawStyle.fontMap as FontImageMap).texture.bind(0)
            Driver.instance.drawVertexBuffer(shader, listOf(vertices), DrawPrimitive.TRIANGLES, 0, quads * 6)
            shader.end()
            quads = 0
        }
    }

    private fun insertCharacterQuad(fontMap: FontImageMap, bw: BufferWriter, character: Char, x: Double, y: Double) {
        val rectangle = fontMap.map[character]!!

        val pad = 1.0f

        val u0 = (rectangle.x.toFloat() - pad) / fontMap.texture.width
        val u1 = u0 + (1 + pad * 2 + rectangle.width.toFloat()) / (fontMap.texture.width)
        val v0 = (rectangle.y.toFloat() - pad) / fontMap.texture.height
        val v1 = v0 + (1 + pad * 2 + rectangle.height.toFloat()) / (fontMap.texture.height)

        val x0 = x.toFloat() - pad
        val x1 = x0 + rectangle.width.toFloat() / fontMap.contentScale.toFloat() + pad * 2
        val y0 = y.toFloat() - pad
        val y1 = y0 + rectangle.height.toFloat() / fontMap.contentScale.toFloat() + pad * 2

        val s0 = 0.0f
        val t0 = 0.0f
        val s1 = 1.0f
        val t1 = 1.0f

        val w = (x1 - x0)
        val h = (y1 - y0)


        val z = quads.toFloat()

        bw.apply {
            write(u0, v0); write(s0, t0, w, h); write(x0, y0, z)
            write(u1, v0); write(s1, t0, w, h); write(x1, y0, z)
            write(u1, v1); write(s1, t1, w, h); write(x1, y1, z)

            write(u0, v0); write(s0, t0, w, h); write(x0, y0, z)
            write(u0, v1); write(s0, t1, w, h); write(x0, y1, z)
            write(u1, v1); write(s1, t1, w, h); write(x1, y1, z)
        }
        quads++
    }
}