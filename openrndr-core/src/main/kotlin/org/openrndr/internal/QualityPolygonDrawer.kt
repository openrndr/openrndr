package org.openrndr.internal

import org.openrndr.draw.DrawContext
import org.openrndr.draw.DrawStyle
import org.openrndr.math.Vector2

class QualityPolygonDrawer {
    private val expansionDrawer = ExpansionDrawer()

    fun drawPolygon(drawContext: DrawContext,
                    drawStyle: DrawStyle,
                    loops: List<List<Vector2>>,
                    corners: List<List<Boolean>>,
                    fringeWidth: Double) {

        if (drawStyle.fill != null) {
            val path = Path.fromLineLoops(loops.mapIndexed { index, it ->
                it.let { it.subList(0, it.size) }
                        .let { if (index == 0) it else it }
            }, corners)
            val strokeWeight = if (drawStyle.stroke == null) 1.0 else 0.0
            val fillExpansions = path.expandFill(fringeWidth, fringeWidth, drawStyle.lineJoin, 2.4)
            expansionDrawer.renderFill(drawContext, drawStyle, fillExpansions, path.convex, fringeWidth)
        }
        if (drawStyle.stroke != null) {
            loops.forEachIndexed { index, it ->
                val path = Path.fromLineStrip(it, corners[index], true)
                val strokeExpansion = path.expandStroke(fringeWidth, drawStyle.strokeWeight / 2.0, drawStyle.lineCap, drawStyle.lineJoin, 2.4)
                expansionDrawer.renderStroke(drawContext, drawStyle, strokeExpansion, fringeWidth)
            }
        }
    }

    fun drawPolygons(drawContext: DrawContext,
                     drawStyle: DrawStyle,
                     loops: List<List<List<Vector2>>>,
                     corners: List<List<List<Boolean>>>,
                     fringeWidth: Double = 1.0) {

        if (drawStyle.fill != null) {
            val paths =
                    loops.mapIndexed { loopIndex, loop ->
                        Path.fromLineLoops(loop.mapIndexed { index, it ->
                            it.let { it.subList(0, it.size) }
                                    .let { if (index == 0) it else it.reversed() }
                        }, corners[loopIndex])
                    }
            val fillExpansions = paths.flatMap { path -> path.expandFill(fringeWidth, 0.25, drawStyle.lineJoin, 2.4) }
            expansionDrawer.renderFills(drawContext, drawStyle, fillExpansions, fringeWidth)
        }
//        if (drawStyle.stroke != null) {
//            loops.forEach {
//                val path = Path.fromLineStrip(it.subList(0, it.size - 1), true)
//                val strokeExpansion = path.expandStroke(1.0 / ratio, drawStyle.strokeWeight, drawStyle.lineCap, drawStyle.lineJoin, 2.4)
//                expansionDrawer.renderStroke(drawContext, drawStyle, strokeExpansion)
//            }
//        }
    }

}
