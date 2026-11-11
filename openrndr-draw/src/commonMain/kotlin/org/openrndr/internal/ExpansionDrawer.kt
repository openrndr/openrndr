package org.openrndr.internal

import org.openrndr.draw.*
import org.openrndr.math.Vector2
import org.openrndr.math.Vector4
import org.openrndr.shape.FillRule
import kotlin.jvm.JvmRecord
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 * The linear part of the map from positions in the plane z = 0 to device pixels: (a, b, c, d), x' = a x + b y,
 * y' = c x + d y, when the map is affine, as it is with orthographic projections, null otherwise
 */
internal fun devicePixelMap(drawContext: DrawContext): DoubleArray? {
    val m = drawContext.projection * drawContext.view * drawContext.model
    // the w of positions in the plane z = 0 must not depend on the position
    if (abs(m.c0r3) > 1E-12 || abs(m.c1r3) > 1E-12 || abs(m.c3r3) < 1E-12) {
        return null
    }
    val sx = 0.5 * drawContext.width * drawContext.contentScale / m.c3r3
    val sy = 0.5 * drawContext.height * drawContext.contentScale / m.c3r3
    val map = doubleArrayOf(m.c0r0 * sx, m.c1r0 * sx, m.c0r1 * sy, m.c1r1 * sy)
    val determinant = map[0] * map[3] - map[1] * map[2]
    return if (abs(determinant) > 1E-12 && map.all { it.isFinite() }) map else null
}

/** the smallest scale of the linear [map] (a, b, c, d) in any direction, its smallest singular value */
internal fun minimumScale(map: DoubleArray): Double {
    val (a, b, c, d) = map
    val sum = a * a + b * b + c * c + d * d
    val determinant = a * d - b * c
    return sqrt(max(0.0, 0.5 * (sum - sqrt(max(0.0, sum * sum - 4.0 * determinant * determinant)))))
}

/**
 * Maps [expansions] made in device pixels by [map] back: the inverse of the linear map (a, b, c, d)
 */
internal fun mapFromDevicePixels(expansions: List<Expansion>, map: DoubleArray) {
    val (a, b, c, d) = map
    val determinant = a * d - b * c
    for (expansion in expansions) {
        expansion.mapPositions(d / determinant, -b / determinant, -c / determinant, a / determinant)
    }
}

/**
 * The bounds of [loops] as the `bounds` uniform of the expansion shaders: the position of the top-left corner and the
 * size, over all points of all loops. Shade styles read positions in them as `c_boundsPosition`. They are the bounds
 * of the geometry, strokes have the bounds of the geometry they stroke, without the stroke weight.
 */
internal fun expansionBounds(loops: List<List<Vector2>>): Vector4 {
    var minX = Double.POSITIVE_INFINITY
    var minY = Double.POSITIVE_INFINITY
    var maxX = Double.NEGATIVE_INFINITY
    var maxY = Double.NEGATIVE_INFINITY
    for (loop in loops) {
        for (point in loop) {
            if (point.x < minX) minX = point.x
            if (point.x > maxX) maxX = point.x
            if (point.y < minY) minY = point.y
            if (point.y > maxY) maxY = point.y
        }
    }
    return if (minX > maxX) Vector4.ZERO else Vector4(minX, minY, maxX - minX, maxY - minY)
}

/**
 * Clips strokes to the inside ([inside] is true) or the outside of [polygons], [fringes] draw the anti-aliased edge
 * of the stroke on the outline.
 */
internal class StrokeClip(val polygons: List<Expansion>, val fringes: List<Expansion>, val inside: Boolean)

@JvmRecord
internal data class ExpansionCommand(
    val vertexBuffer: VertexBuffer, val type: ExpansionType, val vertexOffset: Int, val vertexCount: Int,
    val minX: Double, val minY: Double, val maxX: Double, val maxY: Double
)

/**
 * A utility class responsible for rendering various types of geometric expansions and fills
 * utilizing GPU-based rendering techniques. This class provides methods to render both convex
 * and non-convex shapes, as well as strokes with adjustable fringe scaling and widths.
 *
 * @property shaderManager Manages the shaders required for rendering graphical expansions.
 * @property vertexFormat Specifies the structure of vertex data used for rendering operations.
 * @property manyVertices A preallocated vertex buffer for handling large vertex sets.
 * @property fewVertices A preallocated vertex buffer for handling small vertex sets.
 * @property quads Stores vertex data specifically for rendering quads.
 * @property counter Tracks the number of handled vertices for rendering.
 * @property quadCounter Tracks the number of handled quads for rendering.
 */
internal class ExpansionDrawer {

    private companion object {
        /** the stencil bit used to clip strokes, the lower bits count the stroke coverage */
        const val CLIP_BIT = 0x80
    }

    private val shaderManager = ShadeStyleManager.fromGenerators(
        "expansion",
        vsGenerator = Driver.instance.shaderGenerators::expansionVertexShader,
        fsGenerator = Driver.instance.shaderGenerators::expansionFragmentShader
    )

    val vertexFormat = vertexFormat {
        position(2)
        textureCoordinate(2)
        attribute("vertexOffset", VertexElementType.FLOAT32)
    }

    val manyVertices = VertexBuffer.createDynamic(vertexFormat, 4 * 1024 * 1024, Session.root)
    val fewVertices =
        List(DrawerConfiguration.vertexBufferMultiBufferCount) { vertexBuffer(vertexFormat, 4 * 128, Session.root) }

    val quads = List(DrawerConfiguration.vertexBufferMultiBufferCount) {
        VertexBuffer.createDynamic(
            vertexFormat,
            6,
            Session.root
        )
    }

    /**
     * Measures the coverage of strokes in device pixels, by the change of their across-stroke coordinate, when they
     * have a fringe of [fringeWidth]: strokes are anti-aliased a pixel wide however they are scaled
     */
    private fun strokeCoverage(shader: Shader, drawStyle: DrawStyle, fringeWidth: Double) {
        if (fringeWidth > 0.0) {
            shader.uniform("strokeCoverage", 1)
            shader.uniform("strokeEdge", drawStyle.strokeWeight / (drawStyle.strokeWeight + fringeWidth))
        } else {
            shader.uniform("strokeCoverage", 0)
        }
    }

    private fun renderStrokeCommands(
        drawContext: DrawContext,
        drawStyle: DrawStyle,
        commands: List<ExpansionCommand>,
        fringeWidth: Double,
        bounds: Vector4
    ) {

        val shader = shaderManager.shader(drawStyle.shadeStyle, listOf(vertexFormat), emptyList())
        shader.begin()
        drawContext.applyToShader(shader)
        drawStyle.applyToShader(shader)
        Driver.instance.setState(drawStyle)
        shader.uniform("bounds", bounds)
        shader.uniform("fringeMode", 0)
        shader.uniform("strokeCoverage", 0)

        val localStyle = drawStyle.copy()
        val fs = fringeWidth
        shader.uniform("strokeMult", (drawStyle.strokeWeight * 0.5 + fs * 0.5) / (fs))
        shader.uniform("strokeFillFactor", 0.0)
        strokeCoverage(shader, drawStyle, fs)
        commands.forEach { command ->

            localStyle.channelWriteMask = ChannelMask(red = true, green = true, blue = true, alpha = true)
            // -- pre
            shader.uniform("strokeThr", 1.0f - 0.5f / 255.0f)
            localStyle.stencil.stencilFunc(StencilTest.EQUAL, 0x00, 0xff)
            localStyle.stencil.stencilOp(StencilOperation.KEEP, StencilOperation.KEEP, StencilOperation.INCREASE)
            Driver.instance.setState(localStyle)
            Driver.instance.drawVertexBuffer(
                shader,
                listOf(command.vertexBuffer),
                DrawPrimitive.TRIANGLE_STRIP,
                command.vertexOffset,
                command.vertexCount,
                verticesPerPatch = 0
            )

            // -- anti-aliased
            shader.uniform("strokeThr", -1.0f)
            localStyle.stencil.stencilFunc(StencilTest.EQUAL, 0x00, 0xff)
            localStyle.stencil.stencilOp(StencilOperation.KEEP, StencilOperation.KEEP, StencilOperation.KEEP)
            Driver.instance.setState(localStyle)
            Driver.instance.drawVertexBuffer(
                shader,
                listOf(command.vertexBuffer),
                DrawPrimitive.TRIANGLE_STRIP,
                command.vertexOffset,
                command.vertexCount,
                verticesPerPatch = 0
            )

            // -- reset stencil
            localStyle.channelWriteMask = ChannelMask(red = false, green = false, blue = false, alpha = false)
            localStyle.stencil.stencilFunc(StencilTest.ALWAYS, 0x0, 0xff)
            localStyle.stencil.stencilOp(StencilOperation.ZERO, StencilOperation.ZERO, StencilOperation.ZERO)
            Driver.instance.setState(localStyle)
            Driver.instance.drawVertexBuffer(
                shader,
                listOf(command.vertexBuffer),
                DrawPrimitive.TRIANGLE_STRIP,
                command.vertexOffset,
                command.vertexCount,
                verticesPerPatch = 0
            )

            localStyle.stencil.stencilTest = StencilTest.DISABLED
            localStyle.channelWriteMask = ChannelMask(red = true, green = true, blue = true, alpha = true)
            Driver.instance.setState(localStyle)
        }
        shader.end()
    }

    /**
     * @param clipCommands polygons that clip the strokes, rendered with even-odd parity into [CLIP_BIT] of the stencil
     * buffer
     * @param fringeCommands the anti-aliased edge of the clipped strokes, drawn on the other side of the clip
     * @param clipInside draw the strokes inside of the clip polygons when true, outside of them when false
     */
    private fun renderStrokeCommandsInterleaved(
        drawContext: DrawContext,
        drawStyle: DrawStyle,
        commands: List<ExpansionCommand>,
        fringeScale: Double,
        bounds: Vector4,
        clipCommands: List<ExpansionCommand> = emptyList(),
        fringeCommands: List<ExpansionCommand> = emptyList(),
        clipInside: Boolean = true
    ) {
        if (commands.isNotEmpty()) {
            val shader = shaderManager.shader(drawStyle.shadeStyle, listOf(vertexFormat))
            shader.begin()
            drawContext.applyToShader(shader)
            drawStyle.applyToShader(shader)
            Driver.instance.setState(drawStyle)

            val localStyle = drawStyle
            val vertexCount = commands.last().let { it.vertexOffset + it.vertexCount }
            val fs = fringeScale
            shader.uniform("strokeMult", (drawStyle.strokeWeight * 0.5 + fs * 0.5) / (fs))
            shader.uniform("strokeFillFactor", 0.0)
            strokeCoverage(shader, drawStyle, fs)

            shader.uniform("bounds", bounds)
            shader.uniform("fringeMode", 0)

            // -- clip, mark the clip polygons in the stencil buffer
            val clipped = clipCommands.isNotEmpty()
            if (clipped) {
                localStyle.channelWriteMask = ChannelMask.NONE
                localStyle.stencil.stencilWriteMask = CLIP_BIT
                localStyle.stencil.stencilFunc(StencilTest.ALWAYS, 0x00, 0xff)
                localStyle.stencil.stencilOp(StencilOperation.KEEP, StencilOperation.KEEP, StencilOperation.INVERT)
                Driver.instance.setState(localStyle)
                drawMerged(shader, clipCommands, setOf(ExpansionType.FILL), DrawPrimitive.TRIANGLES)
                localStyle.stencil.stencilWriteMask = 0xff
            }
            // strokes are drawn where the stencil buffer holds this value, drawn pixels are increased to prevent
            // blending twice
            val reference = if (clipped && clipInside) CLIP_BIT else 0x00

            localStyle.channelWriteMask = ChannelMask(red = true, green = true, blue = true, alpha = true)
            // -- pre
            shader.uniform("strokeThr", 1.0f - 0.5f / 255.0f)
            localStyle.stencil.stencilFunc(StencilTest.EQUAL, reference, 0xff)
            localStyle.stencil.stencilOp(StencilOperation.KEEP, StencilOperation.KEEP, StencilOperation.INCREASE)
            Driver.instance.setState(localStyle)
            Driver.instance.drawVertexBuffer(
                shader,
                listOf(commands[0].vertexBuffer),
                DrawPrimitive.TRIANGLE_STRIP,
                commands[0].vertexOffset,
                vertexCount,
                verticesPerPatch = 0
            )

            // -- anti-aliased
            shader.uniform("strokeThr", 0.0f)
            localStyle.stencil.stencilFunc(StencilTest.EQUAL, reference, 0xff)
            localStyle.stencil.stencilOp(StencilOperation.KEEP, StencilOperation.KEEP, StencilOperation.KEEP)
            Driver.instance.setState(localStyle)
            Driver.instance.drawVertexBuffer(
                shader,
                listOf(commands[0].vertexBuffer),
                DrawPrimitive.TRIANGLE_STRIP,
                commands[0].vertexOffset,
                vertexCount,
                verticesPerPatch = 0
            )

            // -- the anti-aliased edge of clipped strokes, outside of the region the strokes are clipped to
            if (fringeCommands.isNotEmpty()) {
                shader.uniform("strokeThr", -1.0f)
                shader.uniform("strokeMult", 1.0)
                // the fringes are made a pixel wide
                shader.uniform("strokeCoverage", 0)
                localStyle.stencil.stencilFunc(StencilTest.EQUAL, if (clipInside) 0x00 else CLIP_BIT, 0xff)
                localStyle.stencil.stencilOp(StencilOperation.KEEP, StencilOperation.KEEP, StencilOperation.INCREASE)
                Driver.instance.setState(localStyle)
                drawMerged(shader, fringeCommands, setOf(ExpansionType.FRINGE), DrawPrimitive.TRIANGLE_STRIP)
            }

            // -- reset stencil
            localStyle.channelWriteMask = ChannelMask(red = false, green = false, blue = false, alpha = false)
            localStyle.stencil.stencilFunc(StencilTest.ALWAYS, 0x0, 0xff)
            localStyle.stencil.stencilOp(StencilOperation.ZERO, StencilOperation.ZERO, StencilOperation.ZERO)
            Driver.instance.setState(localStyle)
            Driver.instance.drawVertexBuffer(
                shader,
                listOf(commands[0].vertexBuffer),
                DrawPrimitive.TRIANGLE_STRIP,
                commands[0].vertexOffset,
                vertexCount,
                verticesPerPatch = 0
            )
            // the clip bit is also set outside of the strokes and fringes
            drawMerged(shader, fringeCommands, setOf(ExpansionType.FRINGE), DrawPrimitive.TRIANGLE_STRIP)
            drawMerged(shader, clipCommands, setOf(ExpansionType.FILL), DrawPrimitive.TRIANGLES)

            localStyle.stencil.stencilTest = StencilTest.DISABLED
            localStyle.channelWriteMask = ChannelMask(red = true, green = true, blue = true, alpha = true)
            Driver.instance.setState(localStyle)
        }
    }

    private fun renderConvexFillCommands(
        drawContext: DrawContext,
        drawStyle: DrawStyle,
        commands: List<ExpansionCommand>,
        fringeScale: Double,
        bounds: Vector4
    ) {
        val shader = shaderManager.shader(drawStyle.shadeStyle, vertexFormat)
        shader.begin()
        drawContext.applyToShader(shader)
        drawStyle.applyToShader(shader)
        Driver.instance.setState(drawStyle)
        shader.uniform("bounds", bounds)
        shader.uniform("fringeMode", 0)
        shader.uniform("strokeCoverage", 0)

        shader.uniform("strokeThr", -1.0f)
        shader.uniform("strokeMult", 1.0)

        shader.uniform("strokeFillFactor", 1.0)
        // the fill and its fringe are lists of triangles that follow each other, drawn with a single draw call
        drawMerged(shader, commands, setOf(ExpansionType.FILL, ExpansionType.FRINGE), DrawPrimitive.TRIANGLES)
    }

    private fun renderFillCommands(
        drawContext: DrawContext,
        drawStyle: DrawStyle,
        commands: List<ExpansionCommand>,
        fringeWidth: Double,
        bounds: Vector4
    ) {
        if (commands.isEmpty()) {
            return
        }

        val shader = shaderManager.shader(drawStyle.shadeStyle, vertexFormat)
        shader.begin()
        drawContext.applyToShader(shader)
        drawStyle.applyToShader(shader)
        Driver.instance.setState(drawStyle)

        val localStyle = drawStyle

        // the windings of the fill are counted in the stencil buffer, the fill rule determines which counts are inside:
        // all counts but zero for the non-zero rule, the odd counts for the even-odd rule
        val insideMask = when (drawStyle.fillRule) {
            FillRule.NON_ZERO -> 0xff
            FillRule.EVEN_ODD -> 0x01
        }

        // -- pass 1 : draw fill shapes in stencil only
        shader.uniform("strokeThr", -1.0f)
        shader.uniform("strokeMult", 1.0)


        shader.uniform("strokeFillFactor", 1.0)

        val minX = commands.minByOrNull { it.minX }?.minX ?: error("no commands")
        val minY = commands.minByOrNull { it.minY }?.minY ?: error("no commands")
        val maxX = commands.maxByOrNull { it.maxX }?.maxX ?: error("no commands")
        val maxY = commands.maxByOrNull { it.maxY }?.maxY ?: error("no commands")

        shader.uniform("bounds", bounds)
        shader.uniform("fringeMode", 0)
        shader.uniform("strokeCoverage", 0)
        localStyle.frontStencil = StencilStyle()
        localStyle.backStencil = StencilStyle()

        localStyle.frontStencil.stencilWriteMask = 0xff
        localStyle.backStencil.stencilWriteMask = 0xff

        localStyle.frontStencil.stencilOp(
            onStencilTestFail = StencilOperation.KEEP,
            onDepthTestFail = StencilOperation.KEEP,
            onDepthTestPass = StencilOperation.INCREASE_WRAP
        )
        localStyle.backStencil.stencilOp(
            onStencilTestFail = StencilOperation.KEEP,
            onDepthTestFail = StencilOperation.KEEP,
            onDepthTestPass = StencilOperation.DECREASE_WRAP
        )
        localStyle.frontStencil.stencilFunc(stencilTest = StencilTest.ALWAYS, testReference = 0, testMask = 0xff)
        localStyle.backStencil.stencilFunc(stencilTest = StencilTest.ALWAYS, testReference = 0, testMask = 0xff)

        localStyle.channelWriteMask = ChannelMask.NONE
        localStyle.cullTestPass = CullTestPass.ALWAYS
        Driver.instance.setState(localStyle)

        drawMerged(shader, commands, setOf(ExpansionType.FILL), DrawPrimitive.TRIANGLES)

        localStyle.frontStencil = localStyle.stencil
        localStyle.backStencil = localStyle.stencil

        // -- pass 2: draw anti-aliased fringes. The fringes are centered on the outline, the stencil tells on which
        // side of it a pixel is. Pixels inside of the fill are drawn by a fringe only where the other side of the
        // outline is outside of the fill, outlines within the fill are not anti-aliased. Inside pixels drawn by a
        // fringe are cleared in the stencil, so that pass 3 and other fringes do not draw them again.
        localStyle.channelWriteMask = ChannelMask.ALL
        shader.uniform("strokeThr", 0.0f)
        shader.uniform("strokeMult", 1.0)

        fun drawFringes(mode: Int, reference: Int, mask: Int, onPass: StencilOperation) {
            shader.uniform("fringeMode", mode)
            localStyle.stencil.stencilFunc(stencilTest = StencilTest.EQUAL, testReference = reference, testMask = mask)
            localStyle.stencil.stencilOp(
                onStencilTestFail = StencilOperation.KEEP,
                onDepthTestFail = StencilOperation.KEEP,
                onDepthTestPass = onPass
            )
            Driver.instance.setState(localStyle)
            drawMerged(shader, commands, setOf(ExpansionType.FRINGE), DrawPrimitive.TRIANGLE_STRIP)
        }
        // the outside of the fill, where the other side of an outline is always inside
        drawFringes(1, 0x00, insideMask, StencilOperation.KEEP)
        when (drawStyle.fillRule) {
            // the parity changes across every outline, the other side of the inside is outside
            FillRule.EVEN_ODD -> drawFringes(2, 0x01, 0x01, StencilOperation.ZERO)
            // the windings change by one across an outline, more on its left. The other side is outside where the
            // windings are 1 on the left of the outline, or -1 on its right
            FillRule.NON_ZERO -> {
                drawFringes(3, 0x01, 0xff, StencilOperation.ZERO)
                drawFringes(4, 0xff, 0xff, StencilOperation.ZERO)
            }
        }
        shader.uniform("fringeMode", 0)
        Driver.instance.setState(localStyle)

        // -- pass 3: fill in stenciled area in pass 1
        shader.uniform("strokeThr", -1.0f)
        shader.uniform("strokeMult", 1.0)
        localStyle.stencil.stencilFunc(stencilTest = StencilTest.NOT_EQUAL, testReference = 0x0, testMask = insideMask)
        localStyle.stencil.stencilOp(
            onStencilTestFail = StencilOperation.ZERO,
            onDepthTestFail = StencilOperation.ZERO,
            onDepthTestPass = StencilOperation.ZERO
        )
        localStyle.channelWriteMask = ChannelMask.ALL
        localStyle.cullTestPass = CullTestPass.ALWAYS

        val quad = quads[quadCounter.mod(quads.size)]
        quad.shadow.writer().apply {
            rewind()
            write(minX.toFloat(), minY.toFloat()); write(0.5f, 1.0f, 0.0f)
            write(minX.toFloat(), maxY.toFloat()); write(0.5f, 1.0f, 0.0f)
            write(maxX.toFloat(), maxY.toFloat()); write(0.5f, 1.0f, 0.0f)

            write(maxX.toFloat(), maxY.toFloat()); write(0.5f, 1.0f, 0.0f)
            write(maxX.toFloat(), minY.toFloat()); write(0.5f, 1.0f, 0.0f)
            write(minX.toFloat(), minY.toFloat()); write(0.5f, 1.0f, 0.0f)
        }
        quad.shadow.upload()
        quadCounter++
        Driver.instance.setState(localStyle)
        Driver.instance.drawVertexBuffer(shader, listOf(quad), DrawPrimitive.TRIANGLES, 0, 6, verticesPerPatch = 0)


        localStyle.stencil.stencilTest = StencilTest.DISABLED
        shader.end()
    }

    private fun toCommand(
        vertices: VertexBuffer,
        expansion: Expansion,
        vertexOffset: Int,
        stripsAsTriangles: Boolean
    ): ExpansionCommand {
        if (expansion.vertexCount > 0) {
            val vertexSize = (expansion.bufferPosition - expansion.bufferStart) / expansion.vertexCount
            if (expansion.type == ExpansionType.FILL || stripsAsTriangles) {
                if (expansion.vertexCount < 3) {
                    return ExpansionCommand(vertices, ExpansionType.SKIP, 0, 0, 0.0, 0.0, 0.0, 0.0)
                }

                val vertexCount = (expansion.vertexCount - 2) * 3
                val command = ExpansionCommand(
                    vertices, expansion.type, vertexOffset, vertexCount,
                    expansion.minx, expansion.miny, expansion.maxx, expansion.maxy
                )
                vertices.shadow.writer().apply {
                    fun writeVertex(i: Int) {
                        write(expansion.fb, expansion.bufferStart + i * vertexSize, vertexSize)
                    }

                    positionElements = vertexOffset
                    if (expansion.type == ExpansionType.FILL) {
                        // a fan around the first vertex
                        for (i in 1 until expansion.vertexCount - 1) {
                            writeVertex(0)
                            writeVertex(i)
                            writeVertex(i + 1)
                        }
                    } else {
                        // the triangles of a strip, in the orientation they have in the strip
                        for (i in 0 until expansion.vertexCount - 2) {
                            if (i % 2 == 0) {
                                writeVertex(i)
                                writeVertex(i + 1)
                            } else {
                                writeVertex(i + 1)
                                writeVertex(i)
                            }
                            writeVertex(i + 2)
                        }
                    }
                }
                return command

            } else {
                val command = ExpansionCommand(
                    vertices, expansion.type, vertexOffset, stripVertexCount(expansion.vertexCount),
                    expansion.minx, expansion.miny, expansion.maxx, expansion.maxy
                )
                val w = vertices.shadow.writer().apply {
                    positionElements = vertexOffset
                }

                // insert leading degenerate triangles
                w.write(expansion.fb, expansion.bufferStart, vertexSize)

                w.write(expansion.fb, expansion.bufferStart, expansion.bufferPosition - expansion.bufferStart)

                // insert trailing degenerate triangles. Strips have an even number of vertices, so that strips that
                // follow each other keep their facing when they are drawn with a single draw call
                val last = expansion.bufferStart + vertexSize * (expansion.vertexCount - 1)
                w.write(expansion.fb, last, vertexSize)
                if ((expansion.vertexCount + 2) % 2 == 1) {
                    w.write(expansion.fb, last, vertexSize)
                }

                return command
            }


        } else {
            return ExpansionCommand(vertices, ExpansionType.SKIP, 0, 0, 0.0, 0.0, 0.0, 0.0)
        }
    }

    /** the number of vertices of a strip of [vertexCount] vertices with degenerate ends, made even */
    private fun stripVertexCount(vertexCount: Int): Int {
        val count = vertexCount + 2
        return if (count % 2 == 1) count + 1 else count
    }

    /** the number of vertices the command of [expansion] takes in the vertex buffer */
    private fun commandVertexCount(expansion: Expansion, stripsAsTriangles: Boolean = false): Int =
        if (expansion.type == ExpansionType.FILL || stripsAsTriangles) {
            (expansion.vertexCount - 2).coerceAtLeast(0) * 3
        } else {
            stripVertexCount(expansion.vertexCount)
        }

    /**
     * Draws the [commands] of [types] as [primitive]. Commands that follow each other in a vertex buffer are drawn with
     * a single draw call: lists of triangles join as they are, strips join by their degenerate ends.
     */
    private fun drawMerged(
        shader: Shader,
        commands: List<ExpansionCommand>,
        types: Set<ExpansionType>,
        primitive: DrawPrimitive
    ) {
        var first: ExpansionCommand? = null
        var count = 0
        fun flush() {
            first?.let {
                Driver.instance.drawVertexBuffer(
                    shader, listOf(it.vertexBuffer), primitive, it.vertexOffset, count, verticesPerPatch = 0
                )
            }
            first = null
            count = 0
        }
        for (command in commands) {
            if (command.type !in types) continue
            val f = first
            if (f != null && f.vertexBuffer === command.vertexBuffer && f.vertexOffset + count == command.vertexOffset) {
                count += command.vertexCount
            } else {
                flush()
                first = command
                count = command.vertexCount
            }
        }
        flush()
    }

    /**
     * Converts a list of geometry expansions into a series of commands using the provided vertex buffer.
     *
     * @param vertices The vertex buffer used to store vertex data for the generated commands.
     * @param expansions A list of expansions that define the geometry to be processed into commands.
     * @return A list of commands resulting from converting the geometry expansions.
     */
    private fun toCommands(
        vertices: VertexBuffer,
        expansions: List<Expansion>,
        stripsAsTriangles: Boolean = false
    ): List<ExpansionCommand> {
        var vertexOffset = 0
        val commands = mutableListOf<ExpansionCommand>()
        expansions.forEach {
            val command = toCommand(vertices, it, vertexOffset, stripsAsTriangles)
            if (command.type != ExpansionType.SKIP) {
                commands.add(command)
                vertexOffset += command.vertexCount
            }
        }
        vertices.shadow.uploadElements(0, vertexOffset)
        return commands
    }

    private var counter = 0
    private var quadCounter = 0

    private val fewVerticesCapacity = fewVertices[0].vertexCount

    private fun vertices(count: Int): VertexBuffer {
        return if (count <= fewVerticesCapacity) {
            counter++
            fewVertices[counter.mod(fewVertices.size)]
        } else {
            manyVertices
        }
    }

    /**
     * Renders a stroke using the specified drawing context, style, and geometry expansion.
     *
     * @param drawContext The drawing context containing transformation matrices and rendering parameters.
     * @param drawStyle The drawing style defining the appearance of the stroke, such as color and blending.
     * @param expansion The geometry expansion describing the shape of the stroke to be rendered.
     * @param fringeScale The scale factor applied to the fringe of the stroke for anti-aliasing or smoothing.
     */
    fun renderStroke(
        drawContext: DrawContext,
        drawStyle: DrawStyle,
        expansion: Expansion,
        fringeScale: Double,
        bounds: Vector4
    ) {
        val vertexCount = commandVertexCount(expansion)
        renderStrokeCommands(
            drawContext, drawStyle, toCommands(vertices(vertexCount), listOf(expansion)), fringeScale, bounds
        )
    }

    /**
     * Renders strokes using the specified drawing context, style, and geometry expansions.
     *
     * @param drawContext The drawing context containing transformation matrices and rendering parameters.
     * @param drawStyle The drawing style defining the appearance of the strokes, such as color and blending.
     * @param expansions A list of expansions describing the geometry of the strokes to be rendered.
     * @param fringeScale The scale factor applied to the fringe of the strokes for anti-aliasing or smoothing.
     */
    fun renderStrokes(
        drawContext: DrawContext,
        drawStyle: DrawStyle,
        expansions: List<Expansion>,
        fringeScale: Double,
        bounds: Vector4,
        clip: StrokeClip? = null
    ) {
        val clipExpansions = clip?.let { it.polygons + it.fringes } ?: emptyList()
        val vertexCount = expansions.sumOf { commandVertexCount(it) } + clipExpansions.sumOf { commandVertexCount(it) }
        // the strokes come first in the vertex buffer, they are drawn with a single draw call
        val commands = toCommands(vertices(vertexCount), expansions + clipExpansions)
        renderStrokeCommandsInterleaved(
            drawContext,
            drawStyle,
            commands.filter { it.type == ExpansionType.STROKE },
            fringeScale,
            bounds,
            commands.filter { it.type == ExpansionType.FILL },
            commands.filter { it.type == ExpansionType.FRINGE },
            clip?.inside ?: true
        )
    }

    /**
     * Renders filled shapes using the specified drawing context, style, and geometry expansions.
     *
     * @param drawContext The drawing context containing transformation matrices and rendering parameters.
     * @param drawStyle The drawing style defining the appearance of the fill, such as color and blending.
     * @param expansions A list of expansions describing the geometry of the shapes to be filled.
     * @param convex A boolean indicating whether the shapes being rendered are convex.
     * @param fringeScale The scale factor applied to the fringe of the shapes for anti-aliasing or smoothing.
     */
    fun renderFill(
        drawContext: DrawContext,
        drawStyle: DrawStyle,
        expansions: List<Expansion>,
        convex: Boolean,
        fringeScale: Double,
        bounds: Vector4
    ) {
        if (convex) {
            // convex fills are drawn with a single draw call, their fringes are written as triangles
            val vertexCount = expansions.sumOf { commandVertexCount(it, stripsAsTriangles = true) }
            renderConvexFillCommands(
                drawContext, drawStyle, toCommands(vertices(vertexCount), expansions, stripsAsTriangles = true),
                fringeScale, bounds
            )
        } else {
            val vertexCount = expansions.sumOf { commandVertexCount(it) }
            renderFillCommands(drawContext, drawStyle, toCommands(vertices(vertexCount), expansions), fringeScale, bounds)
        }
    }

    /**
     * Renders filled shapes using the specified drawing context, style, and expansions.
     *
     * @param drawContext The drawing context containing transformation matrices and rendering parameters.
     * @param drawStyle The drawing style defining the appearance of the fill, such as color and blending.
     * @param expansions A list of expansions describing the geometry of the shapes to be filled.
     * @param fringeScale The scale factor applied to the fringe of the shapes for anti-aliasing or smoothing.
     */
    fun renderFills(
        drawContext: DrawContext,
        drawStyle: DrawStyle,
        expansions: List<Expansion>,
        fringeScale: Double,
        bounds: Vector4
    ) {
        val vertexCount = expansions.sumOf { commandVertexCount(it) }
        renderFillCommands(drawContext, drawStyle, toCommands(vertices(vertexCount), expansions), fringeScale, bounds)
    }
}