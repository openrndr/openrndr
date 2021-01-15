package org.openrndr.internal

import org.openrndr.draw.*
import org.openrndr.math.Vector2

class CharacterRectangle(val character: Char, val x: Double, val y: Double, val width: Double, val height: Double)

class FontImageMapDrawer {

    private val shaderManager: ShadeStyleManager = ShadeStyleManager.fromGenerators("font-image-map",
            vsGenerator = Driver.instance.shaderGenerators::fontImageMapVertexShader,
            fsGenerator = Driver.instance.shaderGenerators::fontImageMapFragmentShader)

    private val maxQuads = 20000

    private val vertices = VertexBuffer.createDynamic(VertexFormat().apply {
        textureCoordinate(2)
        attribute("bounds", VertexElementType.VECTOR4_FLOAT32)
        position(3)
        attribute("instance", VertexElementType.FLOAT32)
    }, 6 * maxQuads)

    private var quads = 0
    fun drawText(context: DrawContext, drawStyle: DrawStyle, text: String, x: Double, y: Double) {
        (drawStyle.fontMap as? FontImageMap)?.let { fontMap ->
            var cursorX = 0.0
            var cursorY = 0.0

            val bw = vertices.shadow.writer()
            bw.position = vertices.vertexFormat.size * quads * 6

            var lastChar:Char? = null
            text.forEach {
                val lc = lastChar
                val metrics = fontMap.glyphMetrics[it] ?: fontMap.glyphMetrics.getValue(' ')
                if (drawStyle.kerning == KernMode.METRIC) {
                    cursorX += if (lc != null) fontMap.kerning(lc, it) else 0.0
                }
                insertCharacterQuad(fontMap, bw, it, x + cursorX + (metrics.xBitmapShift + metrics.leftSideBearing) / fontMap.contentScale, y + cursorY + metrics.yBitmapShift / fontMap.contentScale, 0)
                cursorX += metrics.advanceWidth
                lastChar = it
            }
            flush(context, drawStyle)
        }
    }



    fun drawTexts(context: DrawContext, drawStyle: DrawStyle, texts: List<String>, positions: List<Vector2>) {
        (drawStyle.fontMap as? FontImageMap)?.let { fontMap ->

            var instance = 0

            for ((text, position) in (texts zip positions)) {
                var cursorX = 0.0
                var cursorY = 0.0

                val bw = vertices.shadow.writer()
                bw.position = vertices.vertexFormat.size * quads * 6

                var lastChar:Char? = null
                text.forEach {
                    val lc = lastChar
                    if (drawStyle.kerning == KernMode.METRIC) {
                       cursorX += if (lc != null) fontMap.kerning(lc, it) else 0.0
                    }
                    val metrics = fontMap.glyphMetrics[it] ?: fontMap.glyphMetrics.getValue(' ')
                    insertCharacterQuad(fontMap, bw, it, position.x + cursorX + (metrics.xBitmapShift + metrics.leftSideBearing) / fontMap.contentScale, position.y + cursorY + metrics.yBitmapShift / fontMap.contentScale, instance)
                    cursorX += metrics.advanceWidth
                    lastChar = it
                }
                instance++
            }
            flush(context, drawStyle)
        }
    }

    var queuedInstances = 0
    fun queueText(fontMap: FontMap, text: String, x: Double, y: Double, tracking: Double = 0.0, kerning:KernMode = KernMode.METRIC) {
        val bw = vertices.shadow.writer()
        bw.position = vertices.vertexFormat.size * quads * 6
        fontMap as FontImageMap
        var cursorX = 0.0
        var cursorY = 0.0
        var lastChar:Char? = null
        text.forEach {
            val lc = lastChar
            val metrics = fontMap.glyphMetrics[it]
            metrics?.let { m ->
                if (kerning == KernMode.METRIC) {
                    cursorX += if (lc != null) fontMap.kerning(lc, it) else 0.0
                }
                insertCharacterQuad(fontMap, bw, it, x + cursorX + 0.0 * m.leftSideBearing / fontMap.contentScale, y + cursorY + m.yBitmapShift / fontMap.contentScale, queuedInstances)
                cursorX += m.advanceWidth + tracking
                lastChar = it
            }
        }
        queuedInstances++
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
        queuedInstances = 0
    }

    private fun insertCharacterQuad(fontMap: FontImageMap, bw: BufferWriter, character: Char, x: Double, y: Double, instance:Int) {
        val rectangle = fontMap.map[character] ?: fontMap.map[' ']

        if (rectangle != null) {

            val pad = 1.0f

            val u0 = (rectangle.x.toFloat() - pad) / fontMap.texture.width
            val u1 = u0 + (pad * 2 + rectangle.width.toFloat()) / (fontMap.texture.width)
            val v0 = (rectangle.y.toFloat() - pad) / fontMap.texture.height
            val v1 = v0 + (pad * 2 + rectangle.height.toFloat()) / (fontMap.texture.height)

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

            val floatInstance = instance.toFloat()

            if (quads < maxQuads) {
                bw.apply {
                    write(u0, v0); write(s0, t0, w, h); write(x0, y0, z); write(floatInstance)
                    write(u1, v0); write(s1, t0, w, h); write(x1, y0, z); write(floatInstance)
                    write(u1, v1); write(s1, t1, w, h); write(x1, y1, z); write(floatInstance)

                    write(u0, v0); write(s0, t0, w, h); write(x0, y0, z); write(floatInstance)
                    write(u0, v1); write(s0, t1, w, h); write(x0, y1, z); write(floatInstance)
                    write(u1, v1); write(s1, t1, w, h); write(x1, y1, z); write(floatInstance)
                }
                quads++
            }
        }
    }
}