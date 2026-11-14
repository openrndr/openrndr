package org.openrndr.internal

import org.openrndr.draw.LineCap
import org.openrndr.draw.LineJoin
import org.openrndr.math.Vector2
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The geometry that fills, strokes and clips of contours expand to does not change: the vertex counts and weighted sums
 * of the vertex data of a set of expansions, recorded from an earlier implementation
 */
class TestExpansionGeometry {
    private val star = List(10) { i ->
        val r = if (i % 2 == 0) 80.0 else 32.0
        val a = i * (2.0 * kotlin.math.PI / 10.0)
        Vector2(100.0 + r * cos(a), 100.0 + r * sin(a))
    }
    private val square = listOf(Vector2(10.0, 10.0), Vector2(60.0, 10.0), Vector2(60.0, 50.0), Vector2(10.0, 50.0))
    private val hole = listOf(Vector2(20.0, 20.0), Vector2(20.0, 40.0), Vector2(50.0, 40.0), Vector2(50.0, 20.0))
    private val zigzag = List(7) { Vector2(10.0 + it * 20.0, if (it % 2 == 0) 10.0 else 70.0) }

    private fun closed(loop: List<Vector2>) = loop + loop.first()
    private fun corners(loop: List<Vector2>) = loop.indices.map { it % 3 != 1 }

    /** the vertex count and a weighted sum of the vertex data of [expansions] */
    private fun digest(expansions: List<Expansion>): Pair<Int, Double> {
        var count = 0
        var sum = 0.0
        for (expansion in expansions) {
            count += expansion.vertexCount
            for (i in expansion.bufferStart until expansion.bufferPosition) {
                sum += expansion.fb[i] * (1 + (i - expansion.bufferStart) % 7)
            }
        }
        return count to sum
    }

    private fun digests(): List<Pair<String, Pair<Int, Double>>> {
        val result = mutableListOf<Pair<String, Pair<Int, Double>>>()
        for (join in LineJoin.entries) {
            val fill = Path.fromLineLoops(listOf(closed(star)), listOf(corners(closed(star))))
            result.add("fill $join" to digest(fill.expandFill(1.0, 1.0, join, 2.4)))
            val stroke = Path.fromLineStrip(star, corners(star), true)
            result.add("loop $join" to digest(listOf(stroke.expandStroke(1.0, 4.0, LineCap.BUTT, join, 1.8))))
            val inside = Path.fromLineStrip(star, corners(star), true)
            result.add("loop inside $join" to digest(listOf(inside.expandStroke(1.0, 4.0, LineCap.BUTT, join, 4.0, 0.0, false))))
        }
        for (cap in LineCap.entries) {
            val strip = Path.fromLineStrip(zigzag, corners(zigzag), false)
            result.add("strip $cap" to digest(listOf(strip.expandStroke(0.5, 3.0, cap, LineJoin.ROUND, 4.0))))
        }
        val holes = Path.fromLineLoops(listOf(closed(square), closed(hole)), listOf(corners(closed(square)), corners(closed(hole))))
        result.add("holes" to digest(holes.expandFill(1.0, 1.0, LineJoin.BEVEL, 2.4)))
        val convex = Path.fromLineLoops(listOf(closed(square)), listOf(corners(closed(square))))
        result.add("convex" to digest(convex.expandFill(1.0, 1.0, LineJoin.BEVEL, 2.4)))
        val clip = Path.fromLineStrip(star, corners(star), true)
        result.add("clip" to digest(clip.expandClip(-0.5)))
        val clipFringe = Path.fromLineStrip(star, corners(star), true)
        result.add("clip fringe" to digest(clipFringe.expandClipFringe(1.0, true)))
        return result
    }

    @Test
    fun `expansions keep their geometry`() {
        val expected = mapOf<String, Pair<Int, Double>>(
            "fill MITER" to (74 to 59495.46592712402),
            "loop MITER" to (52 to 106201.65073498897),
            "loop inside MITER" to (22 to 48889.81460618973),
            "fill BEVEL" to (89 to 72413.3639087677),
            "loop BEVEL" to (70 to 150003.4929220751),
            "loop inside BEVEL" to (64 to 138114.62003719807),
            "fill ROUND" to (89 to 72413.3639087677),
            "loop ROUND" to (132 to 273608.8913459778),
            "loop inside ROUND" to (138 to 289508.38664507866),
            "fill MITER_CLIP" to (58 to 46245.94973754883),
            "loop MITER_CLIP" to (80 to 162201.61622776277),
            "loop inside MITER_CLIP" to (22 to 48889.81460618973),
            "strip ROUND" to (108 to 132267.16785430908),
            "strip BUTT" to (72 to 94825.64574241638),
            "strip SQUARE" to (72 to 94725.08535385132),
            "holes" to (75 to 19974.0),
            "convex" to (32 to 8615.0),
            "clip" to (10 to 7916.973670959473),
            "clip fringe" to (22 to 17515.437377929688)
        )
        for ((name, digest) in digests()) {
            val reference = expected.getValue(name)
            assertEquals(reference.first, digest.first, "$name: vertex count")
            assertTrue(abs(reference.second - digest.second) <= 1E-6 * abs(reference.second), "$name: expected ${reference.second}, was ${digest.second}")
        }
        assertEquals(expected.keys, digests().map { it.first }.toSet())
    }
}
