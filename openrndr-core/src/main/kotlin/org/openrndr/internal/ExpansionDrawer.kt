package org.openrndr.internal

import org.openrndr.draw.*
import org.openrndr.math.Vector4

internal class Command(val vertexBuffer: VertexBuffer, val type: ExpansionType, val vertexOffset: Int, val vertexCount: Int,
                       val minX: Double, val minY: Double, val maxX: Double, val maxY: Double)

internal class ExpansionDrawer {

    private val shaderManager = ShadeStyleManager.fromGenerators(Driver.instance.shaderGenerators::expansionVertexShader,
    Driver.instance.shaderGenerators::expansionFragmentShader)

    var vertices = VertexBuffer.createDynamic(VertexFormat().position(2).textureCoordinate(2).attribute("vertexOffset", 1, VertexElementType.FLOAT32), 4 * 1024 * 1024)
    var quad = VertexBuffer.createDynamic(VertexFormat().position(2).textureCoordinate(2).attribute("vertexOffset", 1, VertexElementType.FLOAT32), 6)

    fun renderStrokeCommands(drawContext: DrawContext, drawStyle: DrawStyle, commands: List<Command>) {

        val shader = shaderManager.shader(drawStyle.shadeStyle, listOf(vertices.vertexFormat), emptyList())
        shader.begin()
        drawContext.applyToShader(shader)
        drawStyle.applyToShader(shader)
        Driver.instance.setState(drawStyle)

        val localStyle = drawStyle.copy()

        val fringe = 1.0
        shader.uniform("strokeMult", (drawStyle.strokeWeight+fringe)/fringe)

//        shader.uniform("strokeMult", drawStyle.strokeWeight / 2.0 + 0.65)
        shader.uniform("strokeFillFactor", 0.0)
        commands.forEach { command ->

            //shader.uniform("bounds", Vector4(command.minX, command.minY, command.maxX - command.minX, command.maxY - command.minY))
            localStyle.channelWriteMask = ChannelMask(true, true, true, true)
            // -- pre
            shader.uniform("strokeThr", 1.0f - 0.5f / 255.0f)
            localStyle.stencil.stencilFunc(StencilTest.EQUAL, 0x00, 0xff)
            localStyle.stencil.stencilOp(StencilOperation.KEEP, StencilOperation.KEEP, StencilOperation.INCREASE)
            Driver.instance.setState(localStyle)
            Driver.instance.drawVertexBuffer(shader, listOf(command.vertexBuffer), DrawPrimitive.TRIANGLE_STRIP, command.vertexOffset, command.vertexCount)

            // -- anti-aliased
            shader.uniform("strokeThr", -1.0f)
            localStyle.stencil.stencilFunc(StencilTest.EQUAL, 0x00, 0xff)
            localStyle.stencil.stencilOp(StencilOperation.KEEP, StencilOperation.KEEP, StencilOperation.KEEP)
            Driver.instance.setState(localStyle)
            Driver.instance.drawVertexBuffer(shader, listOf(command.vertexBuffer), DrawPrimitive.TRIANGLE_STRIP, command.vertexOffset, command.vertexCount)

            // -- reset stencil
            localStyle.channelWriteMask = ChannelMask(false, false, false, false)
            localStyle.stencil.stencilFunc(StencilTest.ALWAYS, 0x0, 0xff)
            localStyle.stencil.stencilOp(StencilOperation.ZERO, StencilOperation.ZERO, StencilOperation.ZERO)
            Driver.instance.setState(localStyle)
            Driver.instance.drawVertexBuffer(shader, listOf(command.vertexBuffer), DrawPrimitive.TRIANGLE_STRIP, command.vertexOffset, command.vertexCount)

            localStyle.stencil.stencilTest = StencilTest.DISABLED
            localStyle.channelWriteMask = ChannelMask(true, true, true, true)
            Driver.instance.setState(localStyle)
        }
        shader.end()
    }

    fun renderStrokeCommandsInterleaved(drawContext: DrawContext, drawStyle: DrawStyle, commands: List<Command>) {

        val shader = shaderManager.shader(drawStyle.shadeStyle, listOf(vertices.vertexFormat))
        shader.begin()
        drawContext.applyToShader(shader)
        drawStyle.applyToShader(shader)
        Driver.instance.setState(drawStyle)

        val localStyle = drawStyle
        val vertexCount = commands.last().let { it.vertexOffset + it.vertexCount }

        shader.uniform("strokeMult", drawStyle.strokeWeight / 2.0 + 0.65)
        shader.uniform("strokeFillFactor", 0.0)

        //shader.uniform("bounds", Vector4(-1000.0, -1000.0, 2000.0, 2000.0))
        localStyle.channelWriteMask = ChannelMask(true, true, true, true)
        // -- pre
        shader.uniform("strokeThr", 1.0f - 0.5f / 255.0f)
        localStyle.stencil.stencilFunc(StencilTest.EQUAL, 0x00, 0xff)
        localStyle.stencil.stencilOp(StencilOperation.KEEP, StencilOperation.KEEP, StencilOperation.INCREASE)
        Driver.instance.setState(localStyle)
        Driver.instance.drawVertexBuffer(shader, listOf(commands[0].vertexBuffer), DrawPrimitive.TRIANGLE_STRIP, commands[0].vertexOffset, vertexCount)

        // -- anti-aliased
        shader.uniform("strokeThr", 0.0f)
        localStyle.stencil.stencilFunc(StencilTest.EQUAL, 0x00, 0xff)
        localStyle.stencil.stencilOp(StencilOperation.KEEP, StencilOperation.KEEP, StencilOperation.KEEP)
        Driver.instance.setState(localStyle)
        Driver.instance.drawVertexBuffer(shader, listOf(commands[0].vertexBuffer), DrawPrimitive.TRIANGLE_STRIP, commands[0].vertexOffset, vertexCount)

        // -- reset stencil
        localStyle.channelWriteMask = ChannelMask(false, false, false, false)
        localStyle.stencil.stencilFunc(StencilTest.ALWAYS, 0x0, 0xff)
        localStyle.stencil.stencilOp(StencilOperation.ZERO, StencilOperation.ZERO, StencilOperation.ZERO)
        Driver.instance.setState(localStyle)
        Driver.instance.drawVertexBuffer(shader, listOf(commands[0].vertexBuffer), DrawPrimitive.TRIANGLE_STRIP, commands[0].vertexOffset, vertexCount)

        localStyle.stencil.stencilTest = StencilTest.DISABLED
        localStyle.channelWriteMask = ChannelMask(true, true, true, true)
        Driver.instance.setState(localStyle)
    }

    fun renderConvexFillCommands(drawContext: DrawContext, drawStyle: DrawStyle, commands: List<Command>) {
        val shader = shaderManager.shader(drawStyle.shadeStyle, vertices.vertexFormat)
        shader.begin()
        drawContext.applyToShader(shader)
        drawStyle.applyToShader(shader)
        Driver.instance.setState(drawStyle)

        shader.uniform("strokeThr", -1.0f)
        shader.uniform("strokeMult", drawStyle.strokeWeight / 2.0 + 0.65)
        shader.uniform("strokeFillFactor", 1.0)
        commands.forEach { command ->
            if (command.type == ExpansionType.FILL) {
                Driver.instance.drawVertexBuffer(shader, listOf(command.vertexBuffer), DrawPrimitive.TRIANGLE_FAN, command.vertexOffset, command.vertexCount)
            }
        }
        commands.forEach { command ->
            if (command.type == ExpansionType.FRINGE) {
                Driver.instance.drawVertexBuffer(shader, listOf(command.vertexBuffer), DrawPrimitive.TRIANGLE_STRIP, command.vertexOffset, command.vertexCount)
            }
        }
    }

    fun renderFillCommands(drawContext: DrawContext, drawStyle: DrawStyle, commands: List<Command>) {

        if (commands.isEmpty()) {
            return
        }

        val shader = shaderManager.shader(drawStyle.shadeStyle, vertices.vertexFormat)
        shader.begin()
        drawContext.applyToShader(shader)
        drawStyle.applyToShader(shader)
        Driver.instance.setState(drawStyle)

        val localStyle = drawStyle

        // -- pass 1 : draw fill shapes in stencil only
        shader.uniform("strokeThr", -1.0f)
        //shader.uniform("strokeMult", drawStyle.strokeWeight / 2.0 + 0.65)
        val fringe = 1.0
        shader.uniform("strokeMult", (drawStyle.strokeWeight+fringe)/fringe)
        shader.uniform("strokeFillFactor", 1.0)
        val command = commands[0]
        //shader.uniform("bounds", Vector4(command.minX, command.minY, command.maxX - command.minX, command.maxY - command.minY))
        localStyle.frontStencil = StencilStyle()
        localStyle.backStencil = StencilStyle()

        localStyle.frontStencil.stencilWriteMask = 0xff
        localStyle.backStencil.stencilWriteMask = 0xff

        localStyle.frontStencil.stencilOp(StencilOperation.KEEP, StencilOperation.KEEP, StencilOperation.INCREASE_WRAP)
        localStyle.backStencil.stencilOp(StencilOperation.KEEP, StencilOperation.KEEP, StencilOperation.DECREASE_WRAP)
        localStyle.frontStencil.stencilFunc(StencilTest.ALWAYS, 0, 0xff)
        localStyle.backStencil.stencilFunc(StencilTest.ALWAYS, 0, 0xff)

        localStyle.channelWriteMask = ChannelMask.NONE
        localStyle.cullTestPass = CullTestPass.ALWAYS
        Driver.instance.setState(localStyle)

        commands.forEach { c ->
            if (c.type == ExpansionType.FILL) {
                Driver.instance.drawVertexBuffer(shader, listOf(c.vertexBuffer), DrawPrimitive.TRIANGLE_FAN, c.vertexOffset, c.vertexCount)
            }
        }

        localStyle.frontStencil = localStyle.stencil
        localStyle.backStencil = localStyle.stencil

        // -- pass 2: draw anti-aliased fringes
        localStyle.channelWriteMask = ChannelMask.ALL
        shader.uniform("strokeThr", 0.0f)
        localStyle.stencil.stencilFunc(StencilTest.EQUAL, 0x00, 0xff)
        localStyle.stencil.stencilOp(StencilOperation.KEEP, StencilOperation.KEEP, StencilOperation.KEEP)
        Driver.instance.setState(localStyle)
        commands.forEach { c ->
            if (c.type == ExpansionType.FRINGE) {
                Driver.instance.drawVertexBuffer(shader, listOf(c.vertexBuffer), DrawPrimitive.TRIANGLE_STRIP, c.vertexOffset, c.vertexCount)
            }
        }

        // -- pass 3: fill in stencilled area in pass 1
        shader.uniform("strokeThr", -1.0f)
        localStyle.stencil.stencilFunc(StencilTest.NOT_EQUAL, 0x0, 0xff)
        localStyle.stencil.stencilOp(StencilOperation.ZERO, StencilOperation.ZERO, StencilOperation.ZERO)
        localStyle.channelWriteMask = ChannelMask.ALL
        localStyle.cullTestPass = CullTestPass.ALWAYS

        val minX = command.minX
        val maxX = command.maxX
        val minY = command.minY
        val maxY = command.maxY

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
        Driver.instance.setState(localStyle)
        Driver.instance.drawVertexBuffer(shader, listOf(quad), DrawPrimitive.TRIANGLES, 0, 6)
        localStyle.stencil.stencilTest = StencilTest.DISABLED
        shader.end()
    }

    fun toCommand(vertices: VertexBuffer, expansion: Expansion, vertexOffset: Int): Command {
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
    }

    fun toCommands(vertices: VertexBuffer, expansions: List<Expansion>): List<Command> {
        var vertexOffset = 0
        val commands = mutableListOf<Command>()
        expansions.forEach {
            commands.add(toCommand(vertices, it, vertexOffset))
            vertexOffset += it.vertexCount + 2
        }
        vertices.shadow.uploadElements(0, vertexOffset)
        return commands
    }

    fun renderStroke(drawContext: DrawContext, drawStyle: DrawStyle, expansion: Expansion) {
        renderStrokeCommands(drawContext, drawStyle, toCommands(vertices, listOf(expansion)))
    }

    fun renderStrokes(drawContext: DrawContext, drawStyle: DrawStyle, expansions: List<Expansion>) {
        renderStrokeCommandsInterleaved(drawContext, drawStyle, toCommands(vertices, expansions))
    }

    fun renderFill(drawContext: DrawContext, drawStyle: DrawStyle, expansions: List<Expansion>, convex: Boolean) {
        if (convex) {
            renderConvexFillCommands(drawContext, drawStyle, toCommands(vertices, expansions))
        } else {
            renderFillCommands(drawContext, drawStyle, toCommands(vertices, expansions))
        }
    }

    fun renderFills(drawContext: DrawContext, drawStyle: DrawStyle, expansions: List<Expansion>) {
        renderFillCommands(drawContext, drawStyle, toCommands(vertices, expansions))
    }
}