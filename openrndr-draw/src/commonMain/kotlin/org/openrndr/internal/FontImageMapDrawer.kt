package org.openrndr.internal

import org.openrndr.draw.*
import org.openrndr.math.Vector2
import kotlin.math.round

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

    private var quadCount = 0

    fun drawText(
        context: DrawContext,
        drawStyle: DrawStyle,
        text: String,
        x: Double,
        y: Double
    )= drawTexts(context, drawStyle, listOf(text), listOf(Vector2(x, y)))


    fun drawTexts(
        context: DrawContext,
        drawStyle: DrawStyle,
        texts: List<String>,
        positions: List<Vector2>
    ) {
        val fontMap = drawStyle.fontMap as? FontImageMap

        if (fontMap!= null) {

            var instance = 0

            val textAndPositionPairs = texts.zip(positions)
            for ((text, position) in textAndPositionPairs) {
                var cursorX = 0.0
                val cursorY = 0.0

                val bw = vertices.shadow.writer()
                bw.position = vertices.vertexFormat.size * quadCount * 6

                var lastChar:Char? = null
                text.forEach {
                    val lc = lastChar
                    if (drawStyle.kerning == KernMode.METRIC) {
                       cursorX += if (lc != null) fontMap.kerning(lc, it) else 0.0
                    }
                    val metrics = fontMap.glyphMetrics[it] ?: fontMap.glyphMetrics.getValue(' ')
                    val dx = insertCharacterQuad(
                        fontMap,
                        bw,
                        it,
                        position.x + cursorX,
                        position.y + cursorY +
                                metrics.yBitmapShift / fontMap.contentScale,
                        instance,
                        drawStyle.textSetting
                    )
                    cursorX += metrics.advanceWidth + dx
                    lastChar = it
                }
                instance++
            }
            flush(context, drawStyle)
        }
    }

    var queuedInstances = 0
    fun queueText(
        fontMap: FontMap,
        text: String,
        x: Double,
        y: Double,
        tracking: Double = 0.0,
        kerning: KernMode, // = KernMode.METRIC,
        textSetting: TextSettingMode,// = TextSettingMode.PIXEL,
    ) {
        val bw = vertices.shadow.writer()
        bw.position = vertices.vertexFormat.size * quadCount * 6
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
                val dx = insertCharacterQuad(
                    fontMap,
                    bw,
                    it,
                    x + cursorX,
                    y + cursorY + metrics.yBitmapShift / fontMap.contentScale,
                    0,
                    textSetting
                )
                cursorX += m.advanceWidth + tracking +dx
                lastChar = it
            }
        }
        queuedInstances++
    }


    fun flush(context: DrawContext, drawStyle: DrawStyle) {
        if (quadCount > 0) {
            vertices.shadow.uploadElements(0, quadCount * 6)
            val shader = shaderManager.shader(drawStyle.shadeStyle, vertices.vertexFormat)
            shader.begin()
            context.applyToShader(shader)

            Driver.instance.setState(drawStyle)
            drawStyle.applyToShader(shader)
            (drawStyle.fontMap as FontImageMap).texture.bind(0)
            Driver.instance.drawVertexBuffer(shader, listOf(vertices), DrawPrimitive.TRIANGLES, 0, quadCount * 6, verticesPerPatch = 0)
            shader.end()
            quadCount = 0
        }
        queuedInstances = 0
    }

    private fun insertCharacterQuad(
        fontMap: FontImageMap,
        bw: BufferWriter,
        character: Char,
        cx: Double,
        cy: Double,
        instance: Int,
        textSetting: TextSettingMode
    ) : Double {
        val rectangle = fontMap.map[character] ?: fontMap.map[' ']
        val targetContentScale = RenderTarget.active.contentScale

        val x = if (textSetting == TextSettingMode.PIXEL) round(cx * targetContentScale) / targetContentScale else cx
        val y = if (textSetting == TextSettingMode.PIXEL) round(cy * targetContentScale) / targetContentScale else cy

        val metrics = fontMap.glyphMetrics[character] ?: fontMap.glyphMetrics[' '] ?: error("glyph or space substitute not found")

        if (rectangle != null) {
            val pad = 2.0f
            val ushift = if (metrics.xBitmapShift <= pad) -(metrics.xBitmapShift/fontMap.texture.effectiveWidth).toFloat() else 0.0f
            val xshift = if (metrics.xBitmapShift > pad) (metrics.xBitmapShift/fontMap.contentScale).toFloat() else 0.0f
            val u0 = (rectangle.x.toFloat() - pad) / fontMap.texture.effectiveWidth + ushift
            val u1 = (rectangle.x.toFloat() + rectangle.width.toFloat() + pad) / fontMap.texture.effectiveWidth + ushift
            val v0 = (rectangle.y.toFloat() - pad) / fontMap.texture.effectiveHeight
            val v1 = v0 + (pad * 2 + rectangle.height.toFloat()) / fontMap.texture.effectiveHeight

            val x0 = x.toFloat() - pad / fontMap.contentScale.toFloat() + xshift
            val x1 = x.toFloat() + (rectangle.width.toFloat() / fontMap.contentScale.toFloat()) + pad / fontMap.contentScale.toFloat() + xshift
            val y0 = y.toFloat() - pad / fontMap.contentScale.toFloat()
            val y1 = y.toFloat() + rectangle.height.toFloat() / fontMap.contentScale.toFloat() + pad / fontMap.contentScale.toFloat()

            val s0 = 0.0f
            val t0 = 0.0f
            val s1 = 1.0f
            val t1 = 1.0f

            val w = (x1 - x0)
            val h = (y1 - y0)
            val z = quadCount.toFloat()

            val floatInstance = instance.toFloat()

            if (quadCount < maxQuads) {
                bw.apply {
                    write(u0, v0); write(s0, t0, w, h); write(x0, y0, z); write(floatInstance)
                    write(u1, v0); write(s1, t0, w, h); write(x1, y0, z); write(floatInstance)
                    write(u1, v1); write(s1, t1, w, h); write(x1, y1, z); write(floatInstance)

                    write(u0, v0); write(s0, t0, w, h); write(x0, y0, z); write(floatInstance)
                    write(u0, v1); write(s0, t1, w, h); write(x0, y1, z); write(floatInstance)
                    write(u1, v1); write(s1, t1, w, h); write(x1, y1, z); write(floatInstance)
                }
                quadCount++
            }
        }
        return x - cx
    }
}