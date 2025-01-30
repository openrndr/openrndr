package org.openrndr.internal

import org.openrndr.draw.*
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import kotlin.jvm.JvmName

/**
 * A utility class for efficient drawing of lines and line loops in 2D and 3D spaces.
 *
 * This class leverages optimized vertex buffers and custom shaders to render a set of line segments
 * or closed loops with high performance. It manages vertex buffers dynamically based on the size of the
 * data being rendered, reusing small buffers for less complex drawings and larger buffers for more complex ones.
 *
 * The `PerformanceLineDrawer` provides methods to draw:
 * - Line segments in 2D and 3D spaces.
 * - Closed line loops in 2D and 3D spaces.
 *
 * The underlying rendering system uses a shade style manager to configure vertex and fragment
 * shader behavior, accommodating customization via provided shade styles.
 *
 * Functions:
 * - `drawLineSegments`: Draws a series of line segments, supporting both 2D and 3D points.
 * - `drawLineLoops`: Draws a series of closed line loops, supporting both 2D and 3D points.
 *
 * Dynamic vertex buffer selection ensures adaptability and performance. Small buffers are
 * recycled efficiently, while a larger buffer is used for heavy workloads. This enables
 * controlled memory use and optimal rendering speed.
 */
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

    /**
     * Draws a series of line segments in 3D space using the specified drawing context and style.
     *
     * This function processes a list of 3D vectors that represent the vertices of the line segments.
     * Each pair of consecutive vectors in the list defines a single line segment. The drawing operation
     * utilizes the provided `DrawContext` for its transformation matrices and the `DrawStyle` to apply
     * appearance settings such as color, stroke weight, and additional visual properties.
     *
     * @param drawContext The drawing context containing transformation matrices and settings for rendering.
     * @param drawStyle The style parameters that define the appearance of the line segments.
     * @param segments A list of 3D vectors representing the points used to define the line segments. Consecutive pairs of points form individual line segments.
     */
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

    /**
     * Draws a series of line segments using the given drawing context and style.
     *
     * This function takes a list of 2D points that represent the vertices of the line segments.
     * Consecutive pairs of points in the list define individual line segments. The method uses
     * the specified drawing context and style to render the lines.
     *
     * @param drawContext The drawing context containing transformation matrices and settings for rendering.
     * @param drawStyle The style parameters that define the appearance of the line segments, such as color and stroke weight.
     * @param segments A list of 2D points representing the vertices of the line segments. Each pair
     *                 of consecutive points defines one line segment.
     */
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

    /**
     * Draws a series of connected line loops using the specified drawing context and style.
     *
     * This function processes a list of loops, where each loop is a list of 2D points. Each loop is
     * rendered as a sequence of connected line segments, closing the loop where the last point connects
     * back to the first point of the loop. The drawing operation utilizes the provided `DrawContext`
     * for its transformation matrices and the `DrawStyle` to apply appearance settings.
     *
     * @param drawContext The drawing context containing transformation matrices and settings for rendering.
     * @param drawStyle The style parameters that define the appearance of the line loops, such as color, thickness,
     *                  and other visual properties.
     * @param loops A list of loops, where each loop is a list of 2D points. Each loop defines a closed
     *              series of line segments to be drawn.
     */
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

    /**
     * Draws a series of connected line loops in 3D space using the specified drawing context and style.
     *
     * This function processes a list of loops, where each loop is a list of 3D points. Each loop is
     * rendered as a sequence of connected line segments, closing the loop where the last point connects
     * back to the first point of the loop. The drawing operation utilizes the provided `DrawContext`
     * for its transformation matrices and the `DrawStyle` to apply appearance settings.
     *
     * @param drawContext The drawing context containing transformation matrices and settings for rendering.
     * @param drawStyle The style parameters that define the appearance of the line loops, such as color,
     *                  thickness, and other visual properties.
     * @param loops A list of loops, where each loop is a list of 3D points. Each loop defines a closed
     *              series of line segments to be drawn.
     */
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