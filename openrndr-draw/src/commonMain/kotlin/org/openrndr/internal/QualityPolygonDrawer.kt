package org.openrndr.internal

import org.openrndr.draw.DrawContext
import org.openrndr.draw.DrawStyle
import org.openrndr.draw.LineJoin
import org.openrndr.math.Vector2

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
     */
    fun drawPolygon(drawContext: DrawContext,
                    drawStyle: DrawStyle,
                    loops: List<List<Vector2>>,
                    corners: List<List<Boolean>>,
                    fringeWidth: Double) {

        if (drawStyle.fill != null && loops.isNotEmpty()) {
            val path = Path.fromLineLoops(loops, corners)
            val alpha = drawStyle.stroke?.alpha ?: 0.0
            val fw = if (drawStyle.smooth) {
                fringeWidth * (1.0 - alpha)
            } else 0.0
            val fillExpansions = path.expandFill(fw, fw, LineJoin.BEVEL, 2.4)
            expansionDrawer.renderFill(drawContext, drawStyle, fillExpansions, path.convex, fw)
        }
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
