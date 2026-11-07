package org.openrndr.internal

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.math.Vector3

/**
 * Draws lines in 3D space as screen space meshes, a quad per segment.
 *
 * The vertex shader projects the end points and the neighbours of every segment and expands the segment in
 * device pixels, the fragment shader cuts out the joins ([DrawStyle.lineJoin], [DrawStyle.miterLimit]) and caps
 * ([DrawStyle.lineCap]) and anti-aliases the outline. The joins between segments are split along their bisector, so
 * no pixel of a join is drawn twice. Segments that cross the near plane are clipped by it.
 *
 * With [LineWidthMode.PERSPECTIVE] a stroke weight of 1 is 1 pixel wide at w = 1, lines get thinner with distance,
 * with [LineWidthMode.SCREEN] it is 1 pixel wide at any distance.
 * With [DrawStyle.lineBlendOnce] every pixel of a draw is blended once, also where lines overlap.
 */
class MeshLineDrawer {
    private val shaderManager: ShadeStyleManager = ShadeStyleManager.fromGenerators(
        "mesh-line",
        vsGenerator = Driver.instance.shaderGenerators::meshLineVertexShader,
        fsGenerator = Driver.instance.shaderGenerators::meshLineFragmentShader
    )

    /** every vertex holds its whole segment: the start is the position attribute */
    private val vertexFormat = vertexFormat {
        attribute("previous", VertexElementType.VECTOR3_FLOAT32)
        position(3)
        attribute("end", VertexElementType.VECTOR3_FLOAT32)
        attribute("next", VertexElementType.VECTOR3_FLOAT32)
        attribute("corner", VertexElementType.VECTOR2_FLOAT32)
        attribute("width", VertexElementType.FLOAT32)
        textureCoordinate(2)
        attribute("element", VertexElementType.FLOAT32)
        color(4)
    }

    private val manyVertices = vertexBuffer(vertexFormat, 1024 * 1024 - 4, Session.root)
    private val fewVerticesCount = 1020
    private val fewVertices = List(DrawerConfiguration.vertexBufferMultiBufferCount) {
        vertexBuffer(vertexFormat, fewVerticesCount, Session.root)
    }
    private var counter = 0

    private fun vertices(count: Int): VertexBuffer {
        return if (count <= fewVerticesCount) {
            counter++
            fewVertices[counter.mod(fewVertices.size)]
        } else {
            manyVertices
        }
    }

    /**
     * Writes the segments of a draw, the vertices are drawn when the buffer is full and when the writing ends
     */
    private inner class SegmentWriter(
        val drawStyle: DrawStyle,
        val shader: Shader,
        val vertices: VertexBuffer
    ) {
        private val w = vertices.shadow.writer().also { it.rewind() }
        private var vertexCount = 0

        /**
         * Writes the segment from [start] to [end], [previous] equals [start] for a segment without a join at its
         * start, [next] equals [end] for a segment without a join at its end
         */
        fun segment(
            previous: Vector3, start: Vector3, end: Vector3, next: Vector3,
            width: Float, u0: Double, u1: Double, element: Float, color: ColorRGBa
        ) {
            if (vertexCount + 6 > vertices.vertexCount) {
                flush()
            }
            for (corner in corners) {
                w.write(previous)
                w.write(start)
                w.write(end)
                w.write(next)
                w.write(corner.first, corner.second)
                w.write(width)
                w.write(u0.toFloat(), u1.toFloat())
                w.write(element)
                w.write(color)
            }
            vertexCount += 6
        }

        fun flush() {
            if (vertexCount > 0) {
                vertices.shadow.uploadElements(0, vertexCount)
                drawVertices(drawStyle, shader, vertices, vertexCount)
            }
            w.rewind()
            vertexCount = 0
        }
    }

    /** screen widths are not scaled by the model and view transforms */
    private fun widthScale(drawContext: DrawContext, drawStyle: DrawStyle) =
        if (drawStyle.lineWidthMode == LineWidthMode.SCREEN) 1.0f else drawContext.modelViewScalingFactor.toFloat()

    private fun drawVertices(drawStyle: DrawStyle, shader: Shader, vertices: VertexBuffer, vertexCount: Int) {
        fun drawCall() = Driver.instance.drawVertexBuffer(
            shader, listOf(vertices), DrawPrimitive.TRIANGLES, 0, vertexCount, verticesPerPatch = 0
        )
        if (!drawStyle.lineBlendOnce || !RenderTarget.active.hasStencilBuffer) {
            shader.uniform("u_meshLineCoverageThreshold", -1.0f)
            drawCall()
            return
        }
        // like the strokes of the expansion drawer: the covered pixels are drawn where the stencil holds 0 and counted,
        // then the anti-aliased edges where it still holds 0, the stencil is reset to 0 afterwards
        val stencil = StencilStyle()
        val localStyle = drawStyle.copy(stencil = stencil, frontStencil = stencil, backStencil = stencil)
        shader.uniform("u_meshLineCoverageThreshold", 1.0f - 0.5f / 255.0f)
        stencil.stencilFunc(StencilTest.EQUAL, 0x00, 0xff)
        stencil.stencilOp(StencilOperation.KEEP, StencilOperation.KEEP, StencilOperation.INCREASE)
        Driver.instance.setState(localStyle)
        drawCall()

        shader.uniform("u_meshLineCoverageThreshold", -1.0f)
        stencil.stencilOp(StencilOperation.KEEP, StencilOperation.KEEP, StencilOperation.KEEP)
        Driver.instance.setState(localStyle)
        drawCall()

        localStyle.channelWriteMask = ChannelMask(red = false, green = false, blue = false, alpha = false)
        stencil.stencilFunc(StencilTest.ALWAYS, 0x00, 0xff)
        stencil.stencilOp(StencilOperation.ZERO, StencilOperation.ZERO, StencilOperation.ZERO)
        Driver.instance.setState(localStyle)
        drawCall()
        Driver.instance.setState(drawStyle)
    }

    private inline fun draw(
        drawContext: DrawContext,
        drawStyle: DrawStyle,
        segmentCount: Int,
        write: SegmentWriter.() -> Unit
    ) {
        if (segmentCount == 0) return
        val vertices = vertices(segmentCount * 6)
        val shader = shaderManager.shader(drawStyle.shadeStyle, vertices.vertexFormat)
        shader.begin()
        drawContext.applyToShader(shader)
        drawStyle.applyToShader(shader)
        shader.uniform("u_meshLineJoin", drawStyle.lineJoin.ordinal)
        shader.uniform("u_meshLineCap", drawStyle.lineCap.ordinal)
        shader.uniform("u_meshLineWidthMode", drawStyle.lineWidthMode.ordinal)
        Driver.instance.setState(drawStyle)
        val writer = SegmentWriter(drawStyle, shader, vertices)
        writer.write()
        writer.flush()
        shader.end()
    }

    /**
     * Draws line segments, every two consecutive points in [segments] are a segment
     * @param weights the stroke weights per segment, [DrawStyle.strokeWeight] for segments without a weight
     * @param colors the colors per segment, the last color or [DrawStyle.stroke] for segments without a color
     */
    fun drawLineSegments(
        drawContext: DrawContext, drawStyle: DrawStyle, segments: List<Vector3>,
        weights: List<Double> = emptyList(),
        colors: List<ColorRGBa> = emptyList()
    ) {
        val defaultColor = colors.lastOrNull()?.toLinear() ?: drawStyle.stroke?.toLinear() ?: ColorRGBa.TRANSPARENT
        draw(drawContext, drawStyle, segments.size / 2) {
            for (i in 0 until segments.size / 2) {
                val start = segments[2 * i]
                val end = segments[2 * i + 1]
                val width = weights.getOrElse(i) { drawStyle.strokeWeight }.toFloat() * widthScale(drawContext, drawStyle)
                val color = colors.getOrNull(i)?.toLinear() ?: defaultColor
                segment(start, start, end, end, width, 0.0, 1.0, i.toFloat(), color)
            }
        }
    }

    /**
     * Draws line strips, a strip is closed when its entry in [closed] is true
     * @param weights the stroke weights per strip, [DrawStyle.strokeWeight] for strips without a weight
     * @param colors the colors per strip, the last color or [DrawStyle.stroke] for strips without a color
     */
    fun drawLineStrips(
        drawContext: DrawContext,
        drawStyle: DrawStyle,
        strips: List<List<Vector3>>,
        weights: List<Double> = emptyList(),
        colors: List<ColorRGBa> = emptyList(),
        closed: List<Boolean> = emptyList()
    ) {
        val defaultColor = colors.lastOrNull()?.toLinear() ?: drawStyle.stroke?.toLinear() ?: ColorRGBa.TRANSPARENT
        // repeated points have no direction, they would break the joins
        val points = strips.mapIndexed { index, strip ->
            val distinct = strip.filterIndexed { i, p -> i == 0 || p != strip[i - 1] }
            if (closed.getOrElse(index) { false } && distinct.size > 1 && distinct.first() == distinct.last()) {
                distinct.dropLast(1)
            } else distinct
        }
        fun segmentCount(index: Int): Int {
            val n = points[index].size
            return if (n < 2) 0 else if (closed.getOrElse(index) { false } && n > 2) n else n - 1
        }

        draw(drawContext, drawStyle, points.indices.sumOf { segmentCount(it) }) {
            for ((element, strip) in points.withIndex()) {
                val segmentCount = segmentCount(element)
                if (segmentCount == 0) continue
                val stripClosed = segmentCount == strip.size
                val n = strip.size
                val width = weights.getOrElse(element) { drawStyle.strokeWeight }.toFloat() * widthScale(drawContext, drawStyle)
                val color = colors.getOrNull(element)?.toLinear() ?: defaultColor

                var length = 0.0
                for (i in 0 until segmentCount) {
                    length += strip[i].distanceTo(strip[(i + 1) % n])
                }
                if (length == 0.0) length = 1.0

                var offset = 0.0
                for (i in 0 until segmentCount) {
                    val start = strip[i]
                    val end = strip[(i + 1) % n]
                    val previous = if (stripClosed) strip[(i - 1).mod(n)] else if (i > 0) strip[i - 1] else start
                    val next = if (stripClosed) strip[(i + 2) % n] else if (i + 2 < n) strip[i + 2] else end
                    val segmentLength = start.distanceTo(end)
                    segment(
                        previous, start, end, next, width,
                        offset / length, (offset + segmentLength) / length, element.toFloat(), color
                    )
                    offset += segmentLength
                }
            }
        }
    }

    private companion object {
        /** the corners of the two triangles of a segment: along the segment (0 or 1), across the segment (-1 or 1) */
        val corners = listOf(0f to -1f, 1f to -1f, 1f to 1f, 0f to -1f, 1f to 1f, 0f to 1f)
    }
}
