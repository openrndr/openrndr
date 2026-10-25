package org.openrndr.internal

import org.openrndr.draw.LineJoin
import org.openrndr.math.Vector2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * [Path.convex] drives the convex fast path in [ExpansionDrawer.renderFill]. A simple polygon is
 * convex when every corner turns the same way -- regardless of whether that's clockwise or
 * counter-clockwise -- so these loops are tested in both the winding that OPENRNDR's default
 * (CW_NEGATIVE_Y) shapes are built in, and the reverse.
 */
class TestPathConvexity {

    private fun pathOf(vararg loops: List<Vector2>): Path {
        val closedLoops = loops.map { it + it.first() }
        return Path.fromLineLoops(closedLoops, closedLoops.map { loop -> loop.map { false } })
    }

    private fun Path.isConvex(): Boolean {
        expandFill(fringeWidth = 1.0, weight = 1.0, lineJoin = LineJoin.BEVEL, miterLimit = 2.4)
        return convex
    }

    private val square = listOf(
        Vector2(0.0, 0.0), Vector2(100.0, 0.0), Vector2(100.0, 100.0), Vector2(0.0, 100.0)
    )

    // classic 5-pointed star outline (alternating outer/inner vertices), concave by construction
    private val star = (0 until 10).map { i ->
        val r = if (i % 2 == 0) 80.0 else 32.0
        val a = i * (2.0 * kotlin.math.PI / 10.0)
        Vector2(r * cos(a), r * sin(a))
    }

    // a pentagram, traced vertex-to-vertex (skipping one vertex each step): self-intersecting, but
    // every vertex turns the same way, since it's a regular {5/2} star polygon
    private val pentagram = (0 until 5).map { i ->
        val a = i * (2.0 * kotlin.math.PI * 2.0 / 5.0)
        Vector2(100.0 * cos(a), 100.0 * sin(a))
    }

    @Test
    fun squareIsConvex() {
        assertTrue(pathOf(square).isConvex(), "a square wound in OPENRNDR's default order should be convex")
    }

    @Test
    fun reversedSquareIsConvex() {
        assertTrue(pathOf(square.reversed()).isConvex(), "a square wound in the opposite order should still be convex")
    }

    @Test
    fun starIsNotConvex() {
        assertFalse(pathOf(star).isConvex(), "a 5-pointed star has reflex vertices and must not be flagged convex")
    }

    @Test
    fun reversedStarIsNotConvex() {
        assertFalse(pathOf(star.reversed()).isConvex(), "winding direction must not turn a concave shape convex")
    }

    @Test
    fun pentagramIsNotConvex() {
        assertFalse(
            pathOf(pentagram).isConvex(),
            "a pentagram turns the same way at every vertex but winds around its center twice, " +
                "so it is not a simple polygon and must not be flagged convex"
        )
    }

    @Test
    fun reversedPentagramIsNotConvex() {
        assertFalse(pathOf(pentagram.reversed()).isConvex(), "winding direction must not make a self-intersecting shape convex")
    }

    @Test
    fun multipleContoursAreNeverConvex() {
        val hole = listOf(
            Vector2(25.0, 25.0), Vector2(25.0, 75.0), Vector2(75.0, 75.0), Vector2(75.0, 25.0)
        )
        assertFalse(pathOf(square, hole).isConvex(), "a shape with a hole can never be convex, even if every ring is")
    }
}
