package org.openrndr.internal

import org.openrndr.draw.DrawContext
import org.openrndr.draw.DrawStyle
import org.openrndr.math.Vector2

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
     */
    fun drawLineStrips(drawContext: DrawContext,
                       drawStyle: DrawStyle,
                       strips: List<List<Vector2>>,
                       corners: List<List<Boolean>>,
                       fringeWidth: Double) {
        val fw = if (drawStyle.smooth) fringeWidth else 0.0
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
            expansionDrawer.renderStrokes(drawContext, drawStyle, expansions, fw)
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
        val fw = if (drawStyle.smooth) fringeWidth else 0.0
        if (drawStyle.stroke != null && drawStyle.strokeWeight > 0.0) {
            val expansions = strips.mapIndexed { index, it ->
                val path = Path.fromLineStrip(it, corners[index], false)
                path.expandStroke(fw, weights[index] / 2.0, drawStyle.lineCap, drawStyle.lineJoin, drawStyle.miterLimit)
            }
            expansionDrawer.renderStrokes(drawContext, drawStyle, expansions, fw)
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
     */
    fun drawLineLoops(drawContext: DrawContext,
                      drawStyle: DrawStyle,
                      strips: List<List<Vector2>>,
                      corners: List<List<Boolean>>,
                      fringeWidth: Double = 1.0) {
        val effectiveFringeWidth = if (drawStyle.smooth) fringeWidth else 0.0
        if (drawStyle.stroke != null && drawStyle.strokeWeight > 0) {
            val expansions = strips.mapIndexed { index, it ->
                val path = Path.fromLineStrip(it, corners[index], true)
                path.expandStroke(
                    effectiveFringeWidth,
                    drawStyle.strokeWeight / 2.0,
                    drawStyle.lineCap,
                    drawStyle.lineJoin,
                    drawStyle.miterLimit
                )
            }
            expansionDrawer.renderStrokes(drawContext, drawStyle, expansions, effectiveFringeWidth)
        }
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
        val fw = if (drawStyle.smooth) fringeWidth else 0.0
        if (drawStyle.stroke != null && drawStyle.strokeWeight > 0.0) {
            val expansions = strips.mapIndexed { index, it ->
                val path = Path.fromLineStrip(it, corners[index], true)
                path.expandStroke(fw, weights[index] / 2.0, drawStyle.lineCap, drawStyle.lineJoin, drawStyle.miterLimit)
            }
            expansionDrawer.renderStrokes(drawContext, drawStyle, expansions, fw)
        }
    }
}