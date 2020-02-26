package org.openrndr.internal

import org.openrndr.draw.DrawContext
import org.openrndr.draw.DrawStyle
import org.openrndr.math.Vector2

class QualityPolygonDrawer {
    private val expansionDrawer = ExpansionDrawer()

    fun drawPolygon(drawContext: DrawContext,
                    drawStyle: DrawStyle, loops: List<List<Vector2>>) {

        val ratio = 1.0
        if (drawStyle.fill != null) {
            val path = Path.fromLineStrips(loops.mapIndexed { index, it ->
                it.let { it.subList(0, it.size) }
                        .let { if (index == 0) it else it }
            })
            val strokeWeight = if (drawStyle.stroke == null) 1.0 else 0.0
            val fillExpansions = path.expandFill(0.0 / ratio, strokeWeight, drawStyle.lineJoin, 2.4)
            expansionDrawer.renderFill(drawContext, drawStyle, fillExpansions, path.convex)
        }
        if (drawStyle.stroke != null) {
            loops.forEach {
                val path = Path.fromLineStrip(it, true)
                val strokeExpansion = path.expandStroke(1.0 / ratio, drawStyle.strokeWeight, drawStyle.lineCap, drawStyle.lineJoin, 2.4)
                expansionDrawer.renderStroke(drawContext, drawStyle, strokeExpansion)
            }
        }
    }

    fun drawPolygons(drawContext: DrawContext,
                    drawStyle: DrawStyle, loops: List<List<List<Vector2>>>) {

        val ratio = 1.0
        if (drawStyle.fill != null) {
            val paths =
                    loops.map { loop ->
                        Path.fromLineStrips(loop.mapIndexed { index, it ->
                            it.let { it.subList(0, it.size - 1) }
                                    .let { if (index == 0) it else it.reversed() }
                        })
                    }
            val fillExpansions = paths.flatMap { path ->path.expandFill(1.0 / ratio, 0.25, drawStyle.lineJoin, 2.4)}
            expansionDrawer.renderFills(drawContext, drawStyle, fillExpansions)
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
