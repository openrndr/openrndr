package org.openrndr.internal

import org.openrndr.draw.*
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import kotlin.jvm.JvmName

class PerformanceLineDrawer {

    val vertexFormat = vertexFormat {
        position(3)
        attribute("instance", VertexElementType.FLOAT32)
        attribute("vertexOffset", VertexElementType.FLOAT32)
    }
    private val manyVertices = vertexBuffer(vertexFormat, 1024 * 1024, Session.root)
    private val fewVertices = List(DrawerConfiguration.vertexBufferMultiBufferCount) { vertexBuffer(vertexFormat, 128, Session.root) }

    private var counter = 0

    private val shaderManager: ShadeStyleManager = ShadeStyleManager.fromGenerators("performance-line",
            vsGenerator = Driver.instance.shaderGenerators::fastLineVertexShader,
            fsGenerator = Driver.instance.shaderGenerators::fastLineFragmentShader)

    @JvmName("drawLineSegments3d")
    fun drawLineSegments(drawContext: DrawContext,
                         drawStyle: DrawStyle, segments: List<Vector3>) {
        val vertices = vertices(segments.size)
        val shader = shaderManager.shader(drawStyle.shadeStyle, vertexFormat)
        shader.begin()
        drawContext.applyToShader(shader)
        drawStyle.applyToShader(shader)
        val w = vertices.shadow.writer()

        w.rewind()
        var vertexCount = 0
        segments.forEachIndexed { index, it ->
            w.write(it)
            w.write((index / 2).toFloat())
            w.write((index % 2).toFloat())
            vertexCount++
        }
        vertices.shadow.uploadElements(0, vertexCount)

        Driver.instance.setState(drawStyle)
        Driver.instance.drawVertexBuffer(shader, listOf(vertices), DrawPrimitive.LINES, 0, vertexCount, verticesPerPatch = 0)
        shader.end()
    }

    private fun vertices(count: Int): VertexBuffer {
        return if (count < 64) {
            counter++
            fewVertices[counter.mod(fewVertices.size)]
        } else {
            manyVertices
        }
    }

    fun drawLineSegments(drawContext: DrawContext,
                         drawStyle: DrawStyle, segments: List<Vector2>) {

        val vertices = vertices(segments.size)
        val shader = shaderManager.shader(drawStyle.shadeStyle, vertexFormat)
        shader.begin()
        drawContext.applyToShader(shader)
        drawStyle.applyToShader(shader)
        val w = vertices.shadow.writer()

        w.rewind()

        var vertexCount = 0
        segments.forEachIndexed { index, it ->
            w.write(it); w.write(0.0f)
            w.write((index / 2).toFloat())
            w.write((index % 2).toFloat())
            vertexCount++
        }
        vertices.shadow.uploadElements(0, vertexCount)

        Driver.instance.setState(drawStyle)
        Driver.instance.drawVertexBuffer(shader, listOf(vertices), DrawPrimitive.LINES, 0, vertexCount, verticesPerPatch = 0)
        shader.end()
    }

    fun drawLineLoops(drawContext: DrawContext,
                      drawStyle: DrawStyle, loops: List<List<Vector2>>) {

        val vertices = vertices(loops.sumOf { it.size })
        val shader = shaderManager.shader(drawStyle.shadeStyle, vertexFormat)
        shader.begin()
        drawContext.applyToShader(shader)
        drawStyle.applyToShader(shader)
        val w = vertices.shadow.writer()
        w.rewind()
        var vertexCount = 0

        loops.forEachIndexed { loopIndex, loop ->
            //loop.forEachIndexed { index, it ->
            for (i in 0 until loop.size - 1) {
                w.write(loop[i]); w.write(0.0f)
                w.write(loopIndex.toFloat())
                w.write(1.0f)
                w.write(loop[i + 1]); w.write(0.0f)
                w.write(loopIndex.toFloat())
                w.write(1.0f)

                vertexCount += 2
            }
            //}
        }
        vertices.shadow.uploadElements(0, vertexCount)
        Driver.instance.setState(drawStyle)
        Driver.instance.drawVertexBuffer(shader, listOf(vertices), DrawPrimitive.LINES, 0, vertexCount, verticesPerPatch = 0)
        shader.end()
    }

    @JvmName("drawLineLoops3d")
    fun drawLineLoops(drawContext: DrawContext,
                      drawStyle: DrawStyle, loops: List<List<Vector3>>) {
        val vertices = vertices(loops.sumOf { it.size })
        val shader = shaderManager.shader(drawStyle.shadeStyle, vertices.vertexFormat)
        shader.begin()
        drawContext.applyToShader(shader)
        drawStyle.applyToShader(shader)
        val w = vertices.shadow.writer()
        w.rewind()
        var vertexCount = 0

        loops.forEachIndexed { loopIndex, loop ->
            for (i in 0 until loop.size - 1) {
                w.write(loop[i])
                w.write(loopIndex.toFloat())
                w.write(1.0f)
                w.write(loop[i + 1])
                w.write(loopIndex.toFloat())
                w.write(1.0f)
                vertexCount += 2
            }
        }
        vertices.shadow.uploadElements(0, vertexCount)
        Driver.instance.setState(drawStyle)
        Driver.instance.drawVertexBuffer(shader, listOf(vertices), DrawPrimitive.LINES, 0, vertexCount, verticesPerPatch = 0)
        shader.end()
    }
}