package org.openrndr.internal

import org.openrndr.draw.DrawContext
import org.openrndr.draw.DrawStyle
import org.openrndr.math.Vector2

class QualityLineDrawer {
    private val expansionDrawer = ExpansionDrawer()

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

    fun drawLineLoops(drawContext: DrawContext,
                      drawStyle: DrawStyle,
                      strips: List<List<Vector2>>,
                      corners: List<List<Boolean>>,
                      fringeWidth: Double = 1.0) {
        val fw = if (drawStyle.smooth) fringeWidth else 0.0
        if (drawStyle.stroke != null && drawStyle.strokeWeight > 0) {
            val expansions = strips.mapIndexed { index, it ->
                val path = Path.fromLineStrip(it, corners[index], true)
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