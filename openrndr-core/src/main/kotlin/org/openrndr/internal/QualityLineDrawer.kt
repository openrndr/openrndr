package org.openrndr.internal

import org.openrndr.draw.DrawContext
import org.openrndr.draw.DrawStyle
import org.openrndr.math.Vector2
import org.openrndr.math.map

class QualityLineDrawer {
    private val expansionDrawer = ExpansionDrawer()

    fun drawLineStrips(drawContext: DrawContext,
                       drawStyle: DrawStyle, strips: Iterable<Iterable<Vector2>>, fringeWidth: Double) {
        val expansions = strips.map {
            val path = Path.fromLineStrip(it, false)
            path.expandStroke(fringeWidth, drawStyle.strokeWeight / 2.0, drawStyle.lineCap, drawStyle.lineJoin, 100.0)
        }
        expansionDrawer.renderStrokes(drawContext, drawStyle, expansions, fringeWidth)
    }

    fun drawLineStrips(drawContext: DrawContext,
                       drawStyle: DrawStyle, strips: Iterable<Iterable<Vector2>>, weights: List<Double>, fringeWidth: Double = 1.0) {
        val expansions = strips.mapIndexed { index, it ->
            val path = Path.fromLineStrip(it, false)
            path.expandStroke(fringeWidth, weights[index] / 2.0, drawStyle.lineCap, drawStyle.lineJoin, 100.0)
        }
        expansionDrawer.renderStrokes(drawContext, drawStyle, expansions, fringeWidth)
    }

    fun drawLineLoops(drawContext: DrawContext,
                      drawStyle: DrawStyle, strips: List<List<Vector2>>, fringeWidth: Double = 1.0) {
        val expansions = strips.map {
            val path = Path.fromLineStrip(it, true)
            path.expandStroke(fringeWidth, drawStyle.strokeWeight / 2.0, drawStyle.lineCap, drawStyle.lineJoin, 100.0)
        }
        expansionDrawer.renderStrokes(drawContext, drawStyle, expansions, fringeWidth)
    }

    fun drawLineLoops(drawContext: DrawContext,
                      drawStyle: DrawStyle, strips: List<List<Vector2>>, weights: List<Double>, fringeWidth: Double = 1.0) {
        val expansions = strips.mapIndexed { index, it ->
            val path = Path.fromLineStrip(it, true)
            path.expandStroke(fringeWidth, weights[index] / 2.0, drawStyle.lineCap, drawStyle.lineJoin, 100.0)
        }
        expansionDrawer.renderStrokes(drawContext, drawStyle, expansions, fringeWidth)
    }


}