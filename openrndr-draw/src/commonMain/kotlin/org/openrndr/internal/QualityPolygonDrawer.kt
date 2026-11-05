package org.openrndr.internal

import org.openrndr.draw.DrawContext
import org.openrndr.draw.DrawStyle
import org.openrndr.draw.LineJoin
import org.openrndr.math.Vector2
import org.openrndr.math.Vector4
import kotlin.math.abs

/**
 * Class responsible for high-quality polygon rendering with support for advanced features such as
 * smooth fills, fringe widths, and customizable line joins.
 *
 * This class utilizes an internal expansion drawer for generating and rendering polygon expansions
 * based on the provided drawing context and styles. It supports both fill and stroke styles and
 * can handle complex polygon loops with corner definitions.
 */
class QualityPolygonDrawer {
    private val expansionDrawer = ExpansionDrawer()

    /**
     * Draws a polygon based on the specified parameters including loops, corner settings, and a fringe width for smoothing.
     *
     * @param drawContext The drawing context containing render configurations and transformation matrices.
     * @param drawStyle The style settings including fill, stroke, and other visual customization options.
     * @param loops A list of closed loop paths represented as lists of `Vector2` points defining the edges of the polygon.
     * @param corners A list of lists indicating corner flags for each point in the loops. Defines if a corner is convex or concave.
     * @param fringeWidth A value specifying the fringe width for smoothing, primarily used in rendering filled polygons.
     * @param bounds The bounds that shade styles read positions in as `c_boundsPosition`, the position of the top-left
     *               corner and the size, by default the bounds of the loops.
     */
    fun drawPolygon(drawContext: DrawContext,
                    drawStyle: DrawStyle,
                    loops: List<List<Vector2>>,
                    corners: List<List<Boolean>>,
                    fringeWidth: Double,
                    bounds: Vector4 = expansionBounds(loops)) {

        if (drawStyle.fill != null && loops.isNotEmpty()) {
            // the fill is always anti-aliased, the stroke is drawn over it. A stroke does not necessarily cover the
            // edge of the fill: it can be thinner than a pixel, not be drawn at all (strokeWeight = 0), or lie
            // inside or outside of the outline
            val pixels = if (drawStyle.smooth) devicePixelMap(drawContext) else null
            if (pixels != null) {
                // the fill is expanded in device pixels, so that its fringe is a pixel wide across every edge, however
                // the geometry is scaled, skewed or rotated, and mapped back
                val (a, b, c, d) = pixels
                val path = Path.fromLineLoops(loops.map { loop -> loop.map { Vector2(a * it.x + b * it.y, c * it.x + d * it.y) } }, corners)
                val fillExpansions = path.expandFill(1.0, 1.0, LineJoin.BEVEL, 2.4)
                val determinant = a * d - b * c
                for (expansion in fillExpansions) {
                    expansion.mapPositions(d / determinant, -b / determinant, -c / determinant, a / determinant)
                }
                expansionDrawer.renderFill(drawContext, drawStyle, fillExpansions, path.convex, fringeWidth, bounds)
            } else {
                val path = Path.fromLineLoops(loops, corners)
                val fw = if (drawStyle.smooth) fringeWidth else 0.0
                val fillExpansions = path.expandFill(fw, fw, LineJoin.BEVEL, 2.4)
                expansionDrawer.renderFill(drawContext, drawStyle, fillExpansions, path.convex, fw, bounds)
            }
        }
    }

    /**
     * The linear part of the map from positions in the plane z = 0 to device pixels: (a, b, c, d), x' = a x + b y,
     * y' = c x + d y, when the map is affine, as it is with orthographic projections, null otherwise
     */
    private fun devicePixelMap(drawContext: DrawContext): DoubleArray? {
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

//    fun drawPolygons(drawContext: DrawContext,
//                     drawStyle: DrawStyle,
//                     loops: List<List<List<Vector2>>>,
//                     corners: List<List<List<Boolean>>>,
//                     fringeWidth: Double = 1.0) {
//
//        val fw = if (drawStyle.smooth) fringeWidth / 2.0 else 0.0
//        if (drawStyle.fill != null) {
//            val paths =
//                    loops.mapIndexed { loopIndex, loop ->
//                        Path.fromLineLoops(loop.mapIndexed { index, it ->
//                            it.let { it.subList(0, it.size) }
//                                .let { if (index == 0) it else it.reversed() }
//                        }, corners[loopIndex])
//                    }
//            val fillExpansions = paths.flatMap { path -> path.expandFill(fw, 0.25, drawStyle.lineJoin, 2.4) }
//            expansionDrawer.renderFills(drawContext, drawStyle, fillExpansions, fw)
//        }
//    }

}
