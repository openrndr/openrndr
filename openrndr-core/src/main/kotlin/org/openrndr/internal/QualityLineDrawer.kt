package org.openrndr.internal

import org.openrndr.draw.DrawContext
import org.openrndr.draw.DrawStyle
import org.openrndr.math.Vector2

class QualityLineDrawer {
    private val expansionDrawer = ExpansionDrawer()

    fun drawLineStrips(drawContext: DrawContext,
                       drawStyle: DrawStyle, strips: Iterable<Iterable<Vector2>>) {
        val expansions = strips.map {
            val path = Path.fromLineStrip(it, false)
            path.expandStroke(1.0, drawStyle.strokeWeight, drawStyle.lineCap, drawStyle.lineJoin, 1000.0)
        }
        expansionDrawer.renderStrokes(drawContext, drawStyle, expansions)
    }

    fun drawLineStrips(drawContext: DrawContext,
                       drawStyle: DrawStyle, strips: Iterable<Iterable<Vector2>>, weights: List<Double>) {
        val expansions = strips.mapIndexed { index, it ->
            val path = Path.fromLineStrip(it, false)
            path.expandStroke(1.0, weights[index], drawStyle.lineCap, drawStyle.lineJoin, 1000.0)
        }
        expansionDrawer.renderStrokes(drawContext, drawStyle, expansions)
    }

    fun drawLineLoops(drawContext: DrawContext,
                      drawStyle: DrawStyle, strips: List<List<Vector2>>) {
        val expansions = strips.map {
            val path = Path.fromLineStrip(it, true)
            path.expandStroke(1.0, drawStyle.strokeWeight, drawStyle.lineCap, drawStyle.lineJoin, 1000.0)
        }
        expansionDrawer.renderStrokes(drawContext, drawStyle, expansions)
    }

    fun drawLineLoops(drawContext: DrawContext,
                      drawStyle: DrawStyle, strips: List<List<Vector2>>, weights: List<Double>) {
        val expansions = strips.mapIndexed { index, it ->
            val path = Path.fromLineStrip(it, true)
            path.expandStroke(1.0, weights[index], drawStyle.lineCap, drawStyle.lineJoin, 1000.0)
        }
        expansionDrawer.renderStrokes(drawContext, drawStyle, expansions)
    }


}