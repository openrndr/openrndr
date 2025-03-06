package org.openrndr.internal

import org.openrndr.draw.*
import org.openrndr.math.Vector4

internal data class Command(val vertexBuffer: VertexBuffer, val type: ExpansionType, val vertexOffset: Int, val vertexCount: Int,
                            val minX: Double, val minY: Double, val maxX: Double, val maxY: Double)

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

    private val shaderManager = ShadeStyleManager.fromGenerators("expansion",
            vsGenerator = Driver.instance.shaderGenerators::expansionVertexShader,
            fsGenerator = Driver.instance.shaderGenerators::expansionFragmentShader)

    val vertexFormat = vertexFormat {
        position(2)
        textureCoordinate(2)
        attribute("vertexOffset", VertexElementType.FLOAT32)
    }

    val manyVertices = VertexBuffer.createDynamic(vertexFormat, 4 * 1024 * 1024, Session.root)
    val fewVertices = List(DrawerConfiguration.vertexBufferMultiBufferCount) { vertexBuffer(vertexFormat, 4 * 128, Session.root) }

    val quads = List(DrawerConfiguration.vertexBufferMultiBufferCount) { VertexBuffer.createDynamic(vertexFormat, 6, Session.root) }

    private fun renderStrokeCommands(drawContext: DrawContext, drawStyle: DrawStyle, commands: List<Command>, fringeWidth: Double) {

        val shader = shaderManager.shader(drawStyle.shadeStyle, listOf(vertexFormat), emptyList())
        shader.begin()
        drawContext.applyToShader(shader)
        drawStyle.applyToShader(shader)
        Driver.instance.setState(drawStyle)

        val localStyle = drawStyle.copy()
        val fs = fringeWidth
        shader.uniform("strokeMult", (drawStyle.strokeWeight*0.5 + fs*0.5 ) / (fs) )
        shader.uniform("strokeFillFactor", 0.0)
        commands.forEach { command ->

            //shader.uniform("bounds", Vector4(command.minX, command.minY, command.maxX - command.minX, command.maxY - command.minY))
            localStyle.channelWriteMask = ChannelMask(red = true, green = true, blue = true, alpha = true)
            // -- pre
            shader.uniform("strokeThr", 1.0f - 0.5f / 255.0f)
            localStyle.stencil.stencilFunc(StencilTest.EQUAL, 0x00, 0xff)
            localStyle.stencil.stencilOp(StencilOperation.KEEP, StencilOperation.KEEP, StencilOperation.INCREASE)
            Driver.instance.setState(localStyle)
            Driver.instance.drawVertexBuffer(shader, listOf(command.vertexBuffer), DrawPrimitive.TRIANGLE_STRIP, command.vertexOffset, command.vertexCount, verticesPerPatch = 0)

            // -- anti-aliased
            shader.uniform("strokeThr", -1.0f)
            localStyle.stencil.stencilFunc(StencilTest.EQUAL, 0x00, 0xff)
            localStyle.stencil.stencilOp(StencilOperation.KEEP, StencilOperation.KEEP, StencilOperation.KEEP)
            Driver.instance.setState(localStyle)
            Driver.instance.drawVertexBuffer(shader, listOf(command.vertexBuffer), DrawPrimitive.TRIANGLE_STRIP, command.vertexOffset, command.vertexCount, verticesPerPatch = 0)

            // -- reset stencil
            localStyle.channelWriteMask = ChannelMask(red = false, green = false, blue = false, alpha = false)
            localStyle.stencil.stencilFunc(StencilTest.ALWAYS, 0x0, 0xff)
            localStyle.stencil.stencilOp(StencilOperation.ZERO, StencilOperation.ZERO, StencilOperation.ZERO)
            Driver.instance.setState(localStyle)
            Driver.instance.drawVertexBuffer(shader, listOf(command.vertexBuffer), DrawPrimitive.TRIANGLE_STRIP, command.vertexOffset, command.vertexCount, verticesPerPatch = 0)

            localStyle.stencil.stencilTest = StencilTest.DISABLED
            localStyle.channelWriteMask = ChannelMask(red = true, green = true, blue = true, alpha = true)
            Driver.instance.setState(localStyle)
        }
        shader.end()
    }

    private fun renderStrokeCommandsInterleaved(drawContext: DrawContext, drawStyle: DrawStyle, commands: List<Command>, fringeScale: Double) {
        if (commands.isNotEmpty()) {
            val shader = shaderManager.shader(drawStyle.shadeStyle, listOf(vertexFormat))
            shader.begin()
            drawContext.applyToShader(shader)
            drawStyle.applyToShader(shader)
            Driver.instance.setState(drawStyle)

            val localStyle = drawStyle
            val vertexCount = commands.last().let { it.vertexOffset + it.vertexCount }
            val fs = fringeScale
            shader.uniform("strokeMult", (drawStyle.strokeWeight*0.5 + fs*0.5 ) / (fs) )
            shader.uniform("strokeFillFactor", 0.0)

            shader.uniform("bounds", Vector4(-1000.0, -1000.0, 2000.0, 2000.0))
            localStyle.channelWriteMask = ChannelMask(red = true, green = true, blue = true, alpha = true)
            // -- pre
            shader.uniform("strokeThr", 1.0f - 0.5f / 255.0f)
            localStyle.stencil.stencilFunc(StencilTest.EQUAL, 0x00, 0xff)
            localStyle.stencil.stencilOp(StencilOperation.KEEP, StencilOperation.KEEP, StencilOperation.INCREASE)
            Driver.instance.setState(localStyle)
            Driver.instance.drawVertexBuffer(shader, listOf(commands[0].vertexBuffer), DrawPrimitive.TRIANGLE_STRIP, commands[0].vertexOffset, vertexCount, verticesPerPatch = 0)

            // -- anti-aliased
            shader.uniform("strokeThr", 0.0f)
            localStyle.stencil.stencilFunc(StencilTest.EQUAL, 0x00, 0xff)
            localStyle.stencil.stencilOp(StencilOperation.KEEP, StencilOperation.KEEP, StencilOperation.KEEP)
            Driver.instance.setState(localStyle)
            Driver.instance.drawVertexBuffer(shader, listOf(commands[0].vertexBuffer), DrawPrimitive.TRIANGLE_STRIP, commands[0].vertexOffset, vertexCount, verticesPerPatch = 0)

            // -- reset stencil
            localStyle.channelWriteMask = ChannelMask(red = false, green = false, blue = false, alpha = false)
            localStyle.stencil.stencilFunc(StencilTest.ALWAYS, 0x0, 0xff)
            localStyle.stencil.stencilOp(StencilOperation.ZERO, StencilOperation.ZERO, StencilOperation.ZERO)
            Driver.instance.setState(localStyle)
            Driver.instance.drawVertexBuffer(shader, listOf(commands[0].vertexBuffer), DrawPrimitive.TRIANGLE_STRIP, commands[0].vertexOffset, vertexCount, verticesPerPatch = 0)

            localStyle.stencil.stencilTest = StencilTest.DISABLED
            localStyle.channelWriteMask = ChannelMask(red = true, green = true, blue = true, alpha = true)
            Driver.instance.setState(localStyle)
        }
    }

    private fun renderConvexFillCommands(drawContext: DrawContext, drawStyle: DrawStyle, commands: List<Command>, fringeScale: Double) {
        val shader = shaderManager.shader(drawStyle.shadeStyle, vertexFormat)
        shader.begin()
        drawContext.applyToShader(shader)
        drawStyle.applyToShader(shader)
        Driver.instance.setState(drawStyle)

        shader.uniform("strokeThr", -1.0f)
        shader.uniform("strokeMult", 1.0)

        shader.uniform("strokeFillFactor", 1.0)
        commands.forEach { command ->
            if (command.type == ExpansionType.FILL) {
                Driver.instance.drawVertexBuffer(shader, listOf(command.vertexBuffer), DrawPrimitive.TRIANGLE_FAN, command.vertexOffset, command.vertexCount, verticesPerPatch = 0)
            }
        }
        commands.forEach { command ->
            if (command.type == ExpansionType.FRINGE) {
                Driver.instance.drawVertexBuffer(shader, listOf(command.vertexBuffer), DrawPrimitive.TRIANGLE_STRIP, command.vertexOffset, command.vertexCount, verticesPerPatch = 0)
            }
        }
    }

    private fun renderFillCommands(drawContext: DrawContext, drawStyle: DrawStyle, commands: List<Command>, fringeWidth: Double) {
        if (commands.isEmpty()) {
            return
        }

        val shader = shaderManager.shader(drawStyle.shadeStyle, vertexFormat)
        shader.begin()
        drawContext.applyToShader(shader)
        drawStyle.applyToShader(shader)
        Driver.instance.setState(drawStyle)

        val localStyle = drawStyle

        // -- pass 1 : draw fill shapes in stencil only
        shader.uniform("strokeThr", -1.0f)
        shader.uniform("strokeMult", 1.0)


        shader.uniform("strokeFillFactor", 1.0)

        val minX = commands.minByOrNull { it.minX }?.minX ?: error("no commands")
        val minY = commands.minByOrNull { it.minY }?.minY ?: error("no commands")
        val maxX = commands.maxByOrNull { it.maxX }?.maxX ?: error("no commands")
        val maxY = commands.maxByOrNull { it.maxY }?.maxY ?: error("no commands")

        val command = commands[0]
        shader.uniform("bounds", Vector4(command.minX, command.minY, command.maxX - command.minX, command.maxY - command.minY))
        localStyle.frontStencil = StencilStyle()
        localStyle.backStencil = StencilStyle()

        localStyle.frontStencil.stencilWriteMask = 0xff
        localStyle.backStencil.stencilWriteMask = 0xff

        localStyle.frontStencil.stencilOp(onStencilTestFail = StencilOperation.KEEP, onDepthTestFail = StencilOperation.KEEP, onDepthTestPass = StencilOperation.INCREASE_WRAP)
        localStyle.backStencil.stencilOp(onStencilTestFail = StencilOperation.KEEP, onDepthTestFail = StencilOperation.KEEP, onDepthTestPass = StencilOperation.DECREASE_WRAP)
        localStyle.frontStencil.stencilFunc(stencilTest = StencilTest.ALWAYS, testReference = 0, writeMask = 0xff)
        localStyle.backStencil.stencilFunc(stencilTest = StencilTest.ALWAYS, testReference = 0, writeMask = 0xff)

        localStyle.channelWriteMask = ChannelMask.NONE
        localStyle.cullTestPass = CullTestPass.ALWAYS
        Driver.instance.setState(localStyle)

        val fillCommands = commands.count { it.type == ExpansionType.FILL }
        for (c in commands) {
            if (c.type == ExpansionType.FILL) {
                Driver.instance.drawVertexBuffer(shader, listOf(c.vertexBuffer), DrawPrimitive.TRIANGLE_FAN, c.vertexOffset, c.vertexCount, verticesPerPatch = 0)
                if (fillCommands > 1 && DrawerConfiguration.waitForFinish) {
                    Driver.instance.finish()
                }
            }
        }

        localStyle.frontStencil = localStyle.stencil
        localStyle.backStencil = localStyle.stencil

        // -- pass 2: draw anti-aliased fringes
        localStyle.channelWriteMask = ChannelMask.ALL
        shader.uniform("strokeThr", 0.0f)
        shader.uniform("strokeMult", 1.0)
        localStyle.stencil.stencilFunc(stencilTest = StencilTest.EQUAL, testReference = 0x00, writeMask = 0xff)
        localStyle.stencil.stencilOp(onStencilTestFail = StencilOperation.KEEP, onDepthTestFail = StencilOperation.KEEP, onDepthTestPass = StencilOperation.KEEP)

        Driver.instance.setState(localStyle)
        for (c in commands) {
            if (c.type == ExpansionType.FRINGE) {
                Driver.instance.drawVertexBuffer(shader, listOf(c.vertexBuffer), DrawPrimitive.TRIANGLE_STRIP, c.vertexOffset, c.vertexCount, verticesPerPatch = 0)
            }
        }
        Driver.instance.setState(localStyle)

        // -- pass 3: fill in stencilled area in pass 1
        shader.uniform("strokeThr", -1.0f)
        shader.uniform("strokeMult", 1.0)
        localStyle.stencil.stencilFunc(stencilTest = StencilTest.NOT_EQUAL, testReference = 0x0, writeMask = 0xff)
        localStyle.stencil.stencilTestMask = 0x1
        localStyle.stencil.stencilOp(onStencilTestFail = StencilOperation.ZERO, onDepthTestFail = StencilOperation.ZERO, onDepthTestPass = StencilOperation.ZERO)
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

    private fun toCommand(vertices: VertexBuffer, expansion: Expansion, vertexOffset: Int): Command {
        if (expansion.vertexCount > 0) {
            val command = Command(vertices, expansion.type, vertexOffset, expansion.vertexCount + 2,
                    expansion.minx, expansion.miny, expansion.maxx, expansion.maxy)
            val w = vertices.shadow.writer().apply {
                positionElements = vertexOffset
            }

            val vertexSize = (expansion.bufferPosition - expansion.bufferStart) / expansion.vertexCount

            // insert leading degenerate triangles
            w.write(expansion.fb, expansion.bufferStart, vertexSize)

            w.write(expansion.fb, expansion.bufferStart, expansion.bufferPosition - expansion.bufferStart)

            // insert trailing degenerate triangles
            w.write(expansion.fb, expansion.bufferStart + vertexSize * (expansion.vertexCount - 1), vertexSize)

            return command
        } else {
            return Command(vertices, ExpansionType.SKIP, 0, 0, 0.0, 0.0, 0.0, 0.0)
        }
    }

    /**
     * Converts a list of geometry expansions into a series of commands using the provided vertex buffer.
     *
     * @param vertices The vertex buffer used to store vertex data for the generated commands.
     * @param expansions A list of expansions that define the geometry to be processed into commands.
     * @return A list of commands resulting from converting the geometry expansions.
     */
    private fun toCommands(vertices: VertexBuffer, expansions: List<Expansion>): List<Command> {
        var vertexOffset = 0
        val commands = mutableListOf<Command>()
        expansions.forEach {
            val command = toCommand(vertices, it, vertexOffset)
            if (command.type != ExpansionType.SKIP) {
                commands.add(command)
                vertexOffset += it.vertexCount + 2
            }
        }
        vertices.shadow.uploadElements(0, vertexOffset)
        return commands
    }

    private var counter = 0
    private var quadCounter = 0

    private fun vertices(count: Int): VertexBuffer {
        return if (count < 128) {
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
    fun renderStroke(drawContext: DrawContext, drawStyle: DrawStyle, expansion: Expansion, fringeScale: Double) {
        renderStrokeCommands(drawContext, drawStyle, toCommands(vertices(expansion.vertexCount), listOf(expansion)), fringeScale)
    }

    /**
     * Renders strokes using the specified drawing context, style, and geometry expansions.
     *
     * @param drawContext The drawing context containing transformation matrices and rendering parameters.
     * @param drawStyle The drawing style defining the appearance of the strokes, such as color and blending.
     * @param expansions A list of expansions describing the geometry of the strokes to be rendered.
     * @param fringeScale The scale factor applied to the fringe of the strokes for anti-aliasing or smoothing.
     */
    fun renderStrokes(drawContext: DrawContext, drawStyle: DrawStyle, expansions: List<Expansion>, fringeScale: Double) {
        renderStrokeCommandsInterleaved(drawContext, drawStyle, toCommands(vertices(expansions.sumOf { it.vertexCount }), expansions), fringeScale)
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
    fun renderFill(drawContext: DrawContext, drawStyle: DrawStyle, expansions: List<Expansion>, convex: Boolean, fringeScale: Double) {
        if (convex) {
            renderConvexFillCommands(drawContext, drawStyle, toCommands(vertices(expansions.sumOf { it.vertexCount }), expansions), fringeScale)
        } else {
            renderFillCommands(drawContext, drawStyle, toCommands(vertices(expansions.sumOf { it.vertexCount }), expansions), fringeScale)
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
    fun renderFills(drawContext: DrawContext, drawStyle: DrawStyle, expansions: List<Expansion>, fringeScale: Double) {
        renderFillCommands(drawContext, drawStyle, toCommands(vertices(expansions.sumOf { it.vertexCount }), expansions), fringeScale)
    }
}