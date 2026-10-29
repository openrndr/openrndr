package org.openrndr.draw.windfoil

import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import org.openrndr.shape.toQuadratics
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestWindfoilGeometry {

    private fun Piece.isMonotone(): Boolean {
        fun between(a: Double, b: Double, c: Double) = b in minOf(a, c) - 1E-9..maxOf(a, c) + 1E-9
        return between(start.x, control.x, end.x) && between(start.y, control.y, end.y)
    }

    @Test
    fun `quadratics are split at their extrema in x and y`() {
        // a quadratic with an extremum in x and one in y
        val pieces = mutableListOf<Piece>()
        monotonePieces(Piece(Vector2(0.0, 0.0), Vector2(10.0, 10.0), Vector2(-5.0, 2.0)), pieces)
        assertEquals(3, pieces.size)
        assertTrue(pieces.all { it.isMonotone() })
        // the pieces chain from the start to the end of the quadratic
        assertEquals(Vector2(0.0, 0.0), pieces.first().start)
        assertEquals(Vector2(-5.0, 2.0), pieces.last().end)
        for ((a, b) in pieces.zipWithNext()) {
            assertEquals(a.end, b.start)
        }
    }

    @Test
    fun `monotone quadratics are not split`() {
        val pieces = mutableListOf<Piece>()
        monotonePieces(Piece(Vector2(0.0, 0.0), Vector2(5.0, 1.0), Vector2(10.0, 10.0)), pieces)
        assertEquals(1, pieces.size)
    }

    private fun circlePieces(radius: Double): List<Piece> {
        val pieces = mutableListOf<Piece>()
        for (segment in Circle(0.0, 0.0, radius).contour.segments) {
            for (q in segment.toQuadratics(0.01)) {
                monotonePieces(Piece(q.start, q.control[0], q.end), pieces)
            }
        }
        return pieces
    }

    @Test
    fun `band winding areas add up to the area of the shape`() {
        val radius = 10.0
        val pieces = circlePieces(radius)
        val whole = kotlin.math.abs(bandWindingArea(pieces, -radius, -radius, radius))
        assertEquals(PI * radius * radius, whole, PI * radius * radius * 1E-3)

        // bands tile, the areas of the bands add up to the whole
        val bands = 7
        val sum = (0 until bands).sumOf { b ->
            bandWindingArea(pieces, -radius, -radius + 2.0 * radius * b / bands, -radius + 2.0 * radius * (b + 1) / bands)
        }
        assertEquals(whole, kotlin.math.abs(sum), 1E-9 * whole)

        // the band through the middle of the circle, |y| < r / 2, holds the area of the circular segment pair
        val middle = kotlin.math.abs(bandWindingArea(pieces, -radius, -radius / 2.0, radius / 2.0))
        val expected = 2.0 * (radius * radius * kotlin.math.asin(0.5) + radius / 2.0 * kotlin.math.sqrt(radius * radius - radius * radius / 4.0))
        assertEquals(expected, middle, expected * 1E-3)
    }
}
