package org.openrndr.internal

import org.openrndr.draw.DrawContext
import org.openrndr.draw.DrawStyle
import org.openrndr.draw.StrokeAlignment
import org.openrndr.math.Vector2
import org.openrndr.math.Vector4
import kotlin.math.abs

/**
 * A utility class for rendering high-quality line drawings, supporting customizable
 * stroke styles, smooth rendering, and various line configurations including strips
 * and loops.
 */
class QualityLineDrawer {
    private val expansionDrawer = ExpansionDrawer()

    /**
     * Draws a series of connected line strips with the specified drawing context and style.
     *
     * The method processes input line strips with optional corner smoothing or bluntness
     * and renders them using the provided drawing context and style parameters.
     *
     * @param drawContext The drawing context containing transformation matrices and additional parameters
     *                    necessary to configure a rendering shader.
     * @param drawStyle   The style to apply for the stroke, including color, weight, and other line attributes.
     * @param strips      A list of line strips where each strip is a list of 2D points (Vector2) representing the vertices.
     * @param corners     A list of boolean lists indicating whether the corner at each corresponding vertex
     *                    in the line strip should be smooth (true) or sharp (false).
     * @param fringeWidth The additional width to add to the line edges for anti-aliasing or fringe rendering.
     * @param bounds      The bounds that shade styles read positions in as `c_boundsPosition`, the position of the
     *                    top-left corner and the size, by default the bounds of the strips.
     */
    fun drawLineStrips(drawContext: DrawContext,
                       drawStyle: DrawStyle,
                       strips: List<List<Vector2>>,
                       corners: List<List<Boolean>>,
                       fringeWidth: Double,
                       bounds: Vector4 = expansionBounds(strips)) {
        val fw = strokeFringe(drawContext, drawStyle, fringeWidth)
        if (drawStyle.stroke != null && drawStyle.strokeWeight > 0.0) {
            val expansions = strips.mapIndexed { index, it ->
                val path = Path.fromLineStrip(it, corners[index], false)
                path.expandStroke(
                    fw,
                    drawStyle.strokeWeight / 2.0,
                    drawStyle.lineCap,
                    drawStyle.lineJoin,
                    drawStyle.miterLimit
                )
            }
            expansionDrawer.renderStrokes(drawContext, drawStyle, expansions, fw, bounds)
        }
    }

    /**
     * Draws a series of connected line strips with specified drawing context, style, and parameters.
     *
     * This function processes input line strips, applies optional corner smoothing or sharpness,
     * and renders them using the provided drawing context and style configurations.
     *
     * @param drawContext The drawing context containing transformation matrices and additional parameters needed for rendering.
     * @param drawStyle The style to apply for the stroke, including attributes such as stroke color, weight, and line caps.
     * @param strips A list of line strips where each strip is represented as a list of 2D points (Vector2),
     *               defining the vertices of the line path.
     * @param corners A list of boolean lists specifying the corner type at each corresponding vertex in the line strip.
     *                Use true for smooth corners and false for sharp corners.
     * @param weights A list of double values representing the stroke width for each respective line strip.
     * @param fringeWidth The additional line fringe width added for anti-aliasing or rendering enhancement.
     */
    fun drawLineStrips(drawContext: DrawContext,
                       drawStyle: DrawStyle,
                       strips: List<List<Vector2>>,
                       corners: List<List<Boolean>>,
                       weights: List<Double>,
                       fringeWidth: Double) {
        val fw = strokeFringe(drawContext, drawStyle, fringeWidth)
        if (drawStyle.stroke != null && drawStyle.strokeWeight > 0.0) {
            val expansions = strips.mapIndexed { index, it ->
                val path = Path.fromLineStrip(it, corners[index], false)
                path.expandStroke(fw, weights[index] / 2.0, drawStyle.lineCap, drawStyle.lineJoin, drawStyle.miterLimit)
            }
            expansionDrawer.renderStrokes(drawContext, drawStyle, expansions, fw, expansionBounds(strips))
        }
    }

    /**
     * Draws a series of closed line loops with the specified drawing context and style.
     *
     * This function processes the input line loops, applies optional corner smoothing or sharpness,
     * and renders them using the provided drawing context and style configurations.
     *
     * @param drawContext The drawing context containing transformation matrices and additional parameters needed for rendering.
     * @param drawStyle The style to apply for the stroke, including attributes such as stroke color, weight, and line caps.
     * @param strips A list of line loops where each loop is represented as a list of 2D points (Vector2),
     *               forming the vertices of the closed line path.
     * @param corners A list of boolean lists specifying the corner type at each corresponding vertex in the line loop.
     *                Use true for smooth corners and false for sharp corners.
     * @param fringeWidth The additional line fringe width added for anti-aliasing or rendering enhancement. Defaults to 1.0.
     * @param asShape Are the loops the contours of a single shape? The outside of all loops is then determined by the
     *                loop with the largest area, so that holes wound opposite to it are stroked into the shape.
     *                Otherwise the outside of every loop is determined by the loop itself.
     * @param bounds The bounds that shade styles read positions in as `c_boundsPosition`, the position of the top-left
     *               corner and the size, by default the bounds of the loops, or of every loop when they are clipped
     *               independently.
     */
    fun drawLineLoops(drawContext: DrawContext,
                      drawStyle: DrawStyle,
                      strips: List<List<Vector2>>,
                      corners: List<List<Boolean>>,
                      fringeWidth: Double = 1.0,
                      asShape: Boolean = false,
                      bounds: Vector4? = null) {
        val effectiveFringeWidth = strokeFringe(drawContext, drawStyle, fringeWidth)
        if (drawStyle.stroke != null && drawStyle.strokeWeight > 0) {
            if (!asShape && strips.size > 1 && drawStyle.strokeAlignment != StrokeAlignment.CENTER) {
                // independent loops are clipped independently
                for (index in strips.indices) {
                    drawLineLoops(drawContext, drawStyle, listOf(strips[index]), listOf(corners[index]), fringeWidth, true, bounds)
                }
                return
            }
            val reference = if (asShape) strips.maxByOrNull { abs(Path.signedArea(it)) } else null
            val expansions = strips.mapIndexed { index, it ->
                val path = Path.fromLineStrip(it, corners[index], true)
                path.expandStroke(
                    effectiveFringeWidth,
                    drawStyle.strokeWeight / 2.0,
                    drawStyle.lineCap,
                    drawStyle.lineJoin,
                    drawStyle.miterLimit,
                    drawStyle.strokeAlignment.outsideFraction,
                    Path.signedArea(reference ?: it) > 0.0
                )
            }
            expansionDrawer.renderStrokes(
                drawContext, drawStyle, expansions, effectiveFringeWidth, bounds ?: expansionBounds(strips),
                clip(drawContext, drawStyle, strips, corners, effectiveFringeWidth)
            )
        }
    }

    /**
     * Strokes that are not centered are clipped to the inside or the outside of the loops, so that they never cross
     * the outline. The clip is hard, it lies half of the fringe away from the outline, the anti-aliased edge on the
     * outline is drawn by a fringe in the stroke color. Strokes narrower than two fringes cannot cross the outline
     * noticeably and are not clipped.
     */
    private fun clip(
        drawContext: DrawContext,
        drawStyle: DrawStyle,
        strips: List<List<Vector2>>,
        corners: List<List<Boolean>>,
        fringeWidth: Double,
        strokeWeight: Double = drawStyle.strokeWeight
    ): StrokeClip? {
        if (drawStyle.strokeAlignment == StrokeAlignment.CENTER || strokeWeight < 2.0 * fringeWidth) {
            return null
        }
        // the clip and its fringes are made in device pixels when possible, so that the fringes are a pixel wide
        val pixels = if (fringeWidth > 0.0) devicePixelMap(drawContext) else null
        val width = if (pixels != null) 1.0 else fringeWidth
        val path = Path()
        strips.forEachIndexed { index, strip ->
            val points = if (pixels != null) {
                val (a, b, c, d) = pixels
                strip.map { Vector2(a * it.x + b * it.y, c * it.x + d * it.y) }
            } else strip
            path.contours.addAll(Path.fromLineStrip(points, corners[index], true).contours)
        }
        path.closed = true
        val inside = drawStyle.strokeAlignment == StrokeAlignment.INSIDE
        val polygons = path.expandClip(if (inside) -width * 0.5 else width * 0.5)
        val fringes = if (width > 0.0) path.expandClipFringe(width, inside) else emptyList()
        if (pixels != null) {
            mapFromDevicePixels(polygons, pixels)
            mapFromDevicePixels(fringes, pixels)
        }
        return StrokeClip(polygons, fringes, inside)
    }

    /**
     * The width of the fringe of strokes: wide enough to be at least a pixel wide in every direction, the coverage of
     * strokes is measured in device pixels by the shader
     */
    private fun strokeFringe(drawContext: DrawContext, drawStyle: DrawStyle, fringeWidth: Double): Double {
        if (!drawStyle.smooth) {
            return 0.0
        }
        val pixels = devicePixelMap(drawContext) ?: return fringeWidth
        val scale = minimumScale(pixels)
        return if (scale > 1E-12) 1.0 / scale else fringeWidth
    }
    /**
     * Draws a series of closed line loops with the specified drawing context, style, and parameters.
     *
     * This function processes the input line loops, applies optional corner smoothing or sharpness,
     * and renders them using the provided drawing context and style configurations.
     *
     * @param drawContext The drawing context containing transformation matrices and additional parameters needed for rendering.
     * @param drawStyle The style to apply for the stroke, including attributes such as stroke color, weight, and line caps.
     * @param strips A list of line loops where each loop is represented as a list of 2D points (Vector2),
     *               forming the vertices of the closed line path.
     * @param corners A list of boolean lists specifying the corner type at each corresponding vertex in the line loop.
     *                Use true for smooth corners and false for sharp corners.
     * @param weights A list of double values representing the stroke width for each respective line loop.
     * @param fringeWidth The additional line fringe width added for anti-aliasing or rendering enhancement. Defaults to 1.0.
     */
    fun drawLineLoops(drawContext: DrawContext,
                      drawStyle: DrawStyle,
                      strips: List<List<Vector2>>,
                      corners: List<List<Boolean>>,
                      weights: List<Double>, fringeWidth: Double = 1.0) {
        val fw = strokeFringe(drawContext, drawStyle, fringeWidth)
        if (drawStyle.stroke != null && drawStyle.strokeWeight > 0.0) {
            // independent loops are clipped independently
            val groups = if (drawStyle.strokeAlignment == StrokeAlignment.CENTER) listOf(strips.indices.toList()) else strips.indices.map { listOf(it) }
            for (group in groups) {
                val expansions = group.map { index ->
                    val path = Path.fromLineStrip(strips[index], corners[index], true)
                    path.expandStroke(
                        fw, weights[index] / 2.0, drawStyle.lineCap, drawStyle.lineJoin, drawStyle.miterLimit,
                        drawStyle.strokeAlignment.outsideFraction, Path.signedArea(strips[index]) > 0.0
                    )
                }
                expansionDrawer.renderStrokes(
                    drawContext, drawStyle, expansions, fw, expansionBounds(group.map { strips[it] }),
                    clip(drawContext, drawStyle, group.map { strips[it] }, group.map { corners[it] }, fw, group.minOf { weights[it] })
                )
            }
        }
    }
}