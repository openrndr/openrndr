import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.FillRule
import org.openrndr.shape.Shape
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.triangulate
import org.openrndr.shape.triangulateIndexed
import kotlin.math.*
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TestKTessellationPrecision {
    /** a pentagram, its 5 intersections are the corners of a regular pentagon */
    private fun pentagram(offset: Vector2 = Vector2.ZERO) =
        List(5) { val a = PI / 2 + 2 * PI * (2 * it) / 5; offset + Vector2(cos(a), sin(a)) * 100.0 }

    private fun intersections(offset: Vector2 = Vector2.ZERO): List<Vector2> {
        val r = 100.0 * cos(2 * PI / 5) / cos(PI / 5)
        return List(5) { val a = -PI / 2 + 2 * PI * it / 5; offset + Vector2(cos(a), sin(a)) * r }
    }

    private fun area(t: List<Vector2>): Double {
        var a = 0.0
        for (i in t.indices step 3) a += abs((t[i + 1].x - t[i].x) * (t[i + 2].y - t[i].y) - (t[i + 1].y - t[i].y) * (t[i + 2].x - t[i].x)) / 2
        return a
    }

    @Test
    fun intersectionsAreExactFarFromTheOrigin() {
        val offset = Vector2(1e6, 1e6)
        val star = pentagram(offset)
        val triangles = triangulate(Shape(listOf(ShapeContour.fromPoints(star, true))), fillRule = FillRule.EVEN_ODD)
        val expected = intersections(offset)
        val newVertices = triangles.filter { it !in star }.distinct()
        assertEquals(5, newVertices.size)
        for (v in newVertices) {
            // with float weights the intersections were 0.08 units off
            assertTrue(expected.minOf { it.distanceTo(v) } < 1e-6, "intersection $v")
        }
    }

    @Test
    fun indexedTriangulationsHaveTheVerticesOfIntersections() {
        val star = pentagram()
        for (rule in FillRule.entries) {
            val indexed = triangulateIndexed(listOf(star), rule)
            assertEquals(10, indexed.vertices.size, "$rule vertices")
            assertEquals(star, indexed.vertices.take(5))
            for (v in indexed.vertices.drop(5)) {
                assertTrue(intersections().minOf { it.distanceTo(v) } < 1e-9, "$rule intersection $v")
            }
            val expected = area(triangulate(Shape(listOf(ShapeContour.fromPoints(star, true))), fillRule = rule))
            assertEquals(expected, area(indexed.indices.map { indexed.vertices[it] }), 1e-9, "$rule area")
        }
    }

    @Test
    fun indexOnlyTriangulationRejectsIntersections() {
        assertFailsWith<IllegalArgumentException> { triangulate(listOf(pentagram()), FillRule.EVEN_ODD) }
        val square = listOf(Vector2(0.0, 0.0), Vector2(10.0, 0.0), Vector2(10.0, 10.0), Vector2(0.0, 10.0))
        assertEquals(6, triangulate(listOf(square)).size)
    }

    @Test
    fun triangulationsIn3DAreConsistentlyOriented() {
        // a tilted star, tessellated in fans and strips
        val rnd = Random(1)
        val star = List(200) {
            val a = PI * it / 100
            val r = if (it % 2 == 0) 100.0 else 40.0 * rnd.nextDouble(0.5, 1.0)
            Vector3(cos(a) * r, sin(a) * r, cos(a) * r * 0.5)
        }
        val indexed = triangulateIndexed(listOf(star))
        assertEquals(198, indexed.indices.size / 3)
        val signs = (indexed.indices.indices step 3).map { i ->
            val (a, b, c) = Triple(indexed.vertices[indexed.indices[i]], indexed.vertices[indexed.indices[i + 1]], indexed.vertices[indexed.indices[i + 2]])
            val normal = (b - a).cross(c - a)
            sign(normal.dot(Vector3(-0.5, 0.0, 1.0)))
        }.toSet()
        assertEquals(1, signs.size, "orientations $signs")
    }

    @Test
    fun smallContoursKeepTheirLastVertex() {
        val rnd = Random(3)
        val star = List(400) {
            val a = PI * it / 200
            val r = if (it % 2 == 0) 100.0 else 40.0 * rnd.nextDouble(0.5, 1.0)
            Vector2(cos(a), sin(a)) * r * 1e-6
        }
        assertEquals(400, ShapeContour.fromPoints(star, true).segments.size)
        assertEquals(398 * 3, triangulate(Shape(listOf(ShapeContour.fromPoints(star, true)))).size)
    }

    @Test
    fun largeStarsAreTessellatedQuickly() {
        // the face relabeling and the edge dictionary search were quadratic: 220 ms for 10000 vertices, 30 s for 100000
        val rnd = Random(1)
        val star = List(100_000) {
            val a = PI * it / 50_000
            val r = if (it % 2 == 0) 100.0 else 40.0 * rnd.nextDouble(0.5, 1.0)
            Vector2(cos(a), sin(a)) * r
        }
        val shape = Shape(listOf(ShapeContour.fromPoints(star, true)))
        val start = System.nanoTime()
        val triangles = triangulate(shape)
        val seconds = (System.nanoTime() - start) / 1e9
        assertEquals(99_998 * 3, triangles.size)
        assertTrue(seconds < 10.0, "took $seconds s")
    }
}
