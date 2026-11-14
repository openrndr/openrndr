package org.openrndr.internal

import org.openrndr.draw.*
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import kotlin.jvm.JvmRecord
import kotlin.math.floor

@JvmRecord
data class GlyphOutput(val characters: MutableList<Char>, val rectangles: MutableList<Pair<Rectangle, Rectangle>>)

@JvmRecord
data class GlyphRectangle(val character: Char, val x: Double, val y: Double, val width: Double, val height: Double)

/**
 * The `FontImageMapDrawer` class facilitates rendering of text based on a font image map. It handles
 * generating text vertex data, applying draw styles, and efficiently managing resources like vertex buffers
 * for text rendering tasks.
 *
 * This class is designed to support optimized operations such as batching text quads for rendering,
 * efficient memory management, and high-quality glyph rendering with proper kerning and positioning.
 */
class FontImageMapDrawer {

    private val shaderManager: ShadeStyleManager = ShadeStyleManager.fromGenerators(
        "font-image-map",
        vsGenerator = Driver.instance.shaderGenerators::fontImageMapVertexShader,
        fsGenerator = Driver.instance.shaderGenerators::fontImageMapFragmentShader
    )

    private val maxQuads = 20_000

    private val vertexFormat = VertexFormat().apply {
        textureCoordinate(2)
        attribute("bounds", VertexElementType.VECTOR4_FLOAT32)
        position(3)
        attribute("instance", VertexElementType.FLOAT32)
    }

    private val fewQuads = List(DrawerConfiguration.vertexBufferMultiBufferCount) {
        vertexBuffer(vertexFormat, 6 * 128)
    }

    private val manyQuads = VertexBuffer.createDynamic(vertexFormat, 6 * maxQuads)

    private val vertexFormats = listOf(vertexFormat)
    private val fewQuadLists = fewQuads.map { listOf(it) }
    private val manyQuadsList = listOf(manyQuads)

    private var quadCount = 0

    var counter = 0

    /**
     * Retrieves a vertex buffer based on the specified size.
     *
     * If the size is smaller than 128, a buffer from the `fewQuads` array is selected based on the current counter.
     * Otherwise, the `manyQuads` buffer is returned.
     *
     * @param size The desired size of the vertex buffer.
     * @return A `VertexBuffer` that corresponds to the specified size.
     */
    fun getQueue(size: Int): VertexBuffer {
        return if (size < 128) {
            fewQuads[counter.mod(fewQuads.size)]
        } else {
            manyQuads
        }
    }

    /**
     * Draws a single line of text at the specified position using the provided drawing context and style.
     *
     * @param context The drawing context that contains transformation matrices and rendering parameters.
     * @param drawStyle The style to apply when drawing, including attributes like fill color, stroke color, and font map.
     * @param text The string of text to be drawn.
     * @param x The x-coordinate of the position where the text starts.
     * @param y The y-coordinate of the position where the text starts.
     * @param visible The visibility of the text.
     * @param glyphOutput Optional parameter to output glyph data.
     */
    fun drawText(
        context: DrawContext,
        drawStyle: DrawStyle,
        text: String,
        x: Double,
        y: Double,
        visible: Boolean,
        glyphOutput: GlyphOutput?
    ) {
        val vertices = getQueue(text.length)
        val fontMap = drawStyle.fontMap as? FontImageMap
        if (fontMap != null) {
            val bw = vertices.shadow.writer()
            bw.position = vertices.vertexFormat.size * quadCount * 6
            writeText(fontMap, drawStyle, bw, text, x, y, 0, RenderTarget.active.contentScale, visible, glyphOutput)
            flush(context, drawStyle, vertices)
        }
    }

    /** writes the quads of the glyphs of [text] at ([x], [y]) into [bw] */
    private fun writeText(
        fontMap: FontImageMap,
        drawStyle: DrawStyle,
        bw: BufferWriter,
        text: String,
        x: Double,
        y: Double,
        instance: Int,
        targetContentScale: Double,
        visible: Boolean,
        glyphOutput: GlyphOutput?
    ) {
        var cursorX = 0.0
        val cursorY = 0.0
        var hasLastChar = false
        var lastChar = ' '
        for (i in text.indices) {
            val it = text[i]
            var char = it
            val glyphMetrics = fontMap.glyphMetrics[it] ?: run { char = '�'; fontMap.glyphMetrics['�'] }  ?: run { char = '_'; fontMap.glyphMetrics.getValue('_') }

            if (drawStyle.kerning == KernMode.METRIC) {
                cursorX += if (hasLastChar) fontMap.kerning(lastChar, char) else 0.0
            }

            val dx = insertCharacterQuad(
                fontMap,
                bw,
                char,
                x + cursorX,
                y + cursorY,
                instance,
                drawStyle.textSetting,
                targetContentScale,
                visible,
                glyphOutput
            )
            cursorX += glyphMetrics.advanceWidth + dx
            lastChar = char
            hasLastChar = true
        }
    }


    /**
     * Draws multiple text strings at specified positions using the provided drawing context and style.
     * The method aligns text with precise position information and applies kerning and text settings
     * based on the given draw style.
     *
     * @param context The drawing context containing transformation matrices and rendering parameters.
     * @param drawStyle The style to apply when drawing, including attributes such as font map, kerning, and text settings.
     * @param texts A list of strings to be drawn.
     * @param positions A list of positions where each corresponding string in the `texts` list will be drawn.
     * @param visible The visibility of the text.
     * @param glyphOutput Optional parameter to output glyph data.
     * @return A nested list of glyph rectangles representing the dimensions and positioning of the drawn glyphs.
     */
    fun drawTexts(
        context: DrawContext,
        drawStyle: DrawStyle,
        texts: List<String>,
        positions: List<Vector2>,
        visible: Boolean,
        glyphOutput: GlyphOutput?
    ): List<List<GlyphRectangle>> {
        var count = 0
        for (i in texts.indices) count += texts[i].length
        val vertices = getQueue(count)

        val fontMap = drawStyle.fontMap as? FontImageMap
        if (fontMap != null) {
            val targetContentScale = RenderTarget.active.contentScale
            val bw = vertices.shadow.writer()
            // the texts and positions are paired, the shorter list determines how many texts are drawn
            for (instance in 0 until minOf(texts.size, positions.size)) {
                bw.position = vertices.vertexFormat.size * quadCount * 6
                val position = positions[instance]
                writeText(fontMap, drawStyle, bw, texts[instance], position.x, position.y, instance, targetContentScale, visible, glyphOutput)
            }
            flush(context, drawStyle, vertices)
        }
        return emptyList()
    }

    private var queuedInstances = 0

    /**
     * Queues a single line of text for rendering at the specified position with the given font and settings.
     *
     * The method calculates glyph positions and writes vertex data to the provided vertex buffer,
     * handling kerning, tracking, and other text layout properties. This operation does not perform
     * rendering itself but prepares the text data for eventual rendering.
     *
     * @param fontMap The font map containing the metrics and glyph information necessary for text rendering.
     * @param text The string to be queued for rendering.
     * @param x The x-coordinate of the starting position for the text.
     * @param y The y-coordinate of the starting position for the text.
     * @param tracking The additional spacing to apply between characters, in pixels. Defaults to 0.0.
     * @param kerning The kerning mode, determining how character spacing is adjusted based on metrics.
     * @param textSetting The text setting mode, determining the precision of text rendering (e.g., pixel or subpixel).
     * @param vertices The vertex buffer to which the text's vertex data will be written.
     */
    fun queueText(
        fontMap: FontMap,
        text: String,
        x: Double,
        y: Double,
        tracking: Double = 0.0,
        kerning: KernMode, // = KernMode.METRIC,
        textSetting: TextSettingMode,// = TextSettingMode.PIXEL,
        vertices: VertexBuffer,
        visible: Boolean,
        glyphOutput: GlyphOutput?
    ) {
        val bw = vertices.shadow.writer()
        bw.position = vertices.vertexFormat.size * quadCount * 6
        fontMap as FontImageMap
        var cursorX = 0.0
        val cursorY = 0.0
        val targetContentScale = RenderTarget.active.contentScale
        var hasLastChar = false
        var lastChar = ' '
        for (i in text.indices) {
            val it = text[i]
            val metrics = fontMap.glyphMetrics[it]
            if (metrics != null) {
                if (kerning == KernMode.METRIC) {
                    cursorX += if (hasLastChar) fontMap.kerning(lastChar, it) else 0.0
                }
                val dx = insertCharacterQuad(
                    fontMap,
                    bw,
                    it,
                    x + cursorX,
                    y + cursorY,
                    0,
                    textSetting,
                    targetContentScale,
                    visible,
                    glyphOutput
                )
                cursorX += metrics.advanceWidth + tracking + dx
                lastChar = it
                hasLastChar = true
            }
        }
        queuedInstances++
    }


    /**
     * Flushes the currently queued drawing commands and renders them using the specified context,
     * style, and vertex buffer. This method ensures that the queued vertex data is uploaded,
     * the appropriate shader is applied, and the rendering pipeline is executed.
     *
     * @param context The drawing context containing transformation matrices and rendering parameters.
     * @param drawStyle The style to apply when drawing, including properties such as fill color,
     *                  stroke properties, and font settings.
     * @param vertices The vertex buffer containing vertex data to be rendered.
     *                 This buffer is used to store data for the current batch of quads.
     */
    fun flush(context: DrawContext, drawStyle: DrawStyle, vertices: VertexBuffer) {
        if (quadCount > 0) {
            vertices.shadow.uploadElements(0, quadCount * 6)
            val shader = shaderManager.shader(drawStyle.shadeStyle, vertexFormats)
            shader.begin()
            context.applyToShader(shader)

            Driver.instance.setState(drawStyle)
            drawStyle.applyToShader(shader)
            shader.textureBindings[0] = (drawStyle.fontMap as FontImageMap).texture
            Driver.instance.drawVertexBuffer(
                shader,
                if (vertices === manyQuads) manyQuadsList else fewQuadLists[fewQuads.indexOf(vertices)],
                DrawPrimitive.TRIANGLES,
                0,
                quadCount * 6,
                verticesPerPatch = 0
            )
            shader.end()
            quadCount = 0
        }
        queuedInstances = 0
        if (vertices != manyQuads) {
            counter++
        }
    }

    /**
     * Writes the quad of [character] at ([cx], [cy]) into [bw], returns the shift of the quad that [textSetting] makes
     */
    private fun insertCharacterQuad(
        fontMap: FontImageMap,
        bw: BufferWriter,
        character: Char,
        cx: Double,
        cy: Double,
        instance: Int,
        textSetting: TextSettingMode,
        targetContentScale: Double,
        visible: Boolean,
        glyphOutput: GlyphOutput?
    ): Double {

        var character = character
        val rectangle = fontMap.map[character]
            ?: run {  character = '\uFFFD'; fontMap.map['\uFFFD'] }
            ?: run { character = '_'; fontMap.map[' '] }
        val fmcs = fontMap.contentScale.toFloat()

        val metrics =
            fontMap.glyphMetrics[character] ?: error("glyph or space substitute not found for characters '$character'")
        val xshift = (metrics.xBitmapShift / fmcs).toFloat()
        val yshift = (metrics.yBitmapShift / fmcs).toFloat()


        val sx = cx + xshift
        val sy = cy + yshift

        val x = if (textSetting == TextSettingMode.PIXEL) floor(sx * targetContentScale) / targetContentScale else sx
        val y = if (textSetting == TextSettingMode.PIXEL) floor(sy * targetContentScale) / targetContentScale else sy


        if (rectangle != null) {
                glyphOutput?.characters?.add(character)
                val pad = 2.0f
                val u0 = (rectangle.x.toFloat() - pad) / fontMap.texture.effectiveWidth
                val u1 = (rectangle.x.toFloat() + rectangle.width.toFloat() + pad) / fontMap.texture.effectiveWidth
                val v0 = (rectangle.y.toFloat() - pad) / fontMap.texture.effectiveHeight
                val v1 = v0 + (pad * 2 + rectangle.height.toFloat()) / fontMap.texture.effectiveHeight

                val x0 = x.toFloat() - pad / fmcs
                val x1 = x.toFloat() + (rectangle.width.toFloat() / fmcs) + pad / fmcs
                val y0 = y.toFloat() - pad / fmcs
                val y1 = y.toFloat() + rectangle.height.toFloat() / fmcs + pad / fmcs

                val s0 = 0.0f
                val t0 = 0.0f
                val s1 = 1.0f
                val t1 = 1.0f

                val w = (x1 - x0)
                val h = (y1 - y0)
                val z = quadCount.toFloat()

                val floatInstance = instance.toFloat()

                if (visible && quadCount < maxQuads) {
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
                if (glyphOutput != null) {
                    val source = Rectangle(
                        u0 * fontMap.texture.width * 1.0, (1.0 - v0) * fontMap.texture.height * 1.0,
                        (u1 - u0) * fontMap.texture.width * 1.0, (v0 - v1) * fontMap.texture.height * 1.0
                    )
                    val target = Rectangle(x0.toDouble(), y0.toDouble(), w.toDouble(), h.toDouble())
                    glyphOutput.rectangles.add(Pair(source, target))
                }
        }
        return x - sx
    }
}