package org.openrndr.shape

import org.openrndr.ktessellation.GLConstants.GL_TRIANGLES
import org.openrndr.ktessellation.GLConstants.GL_TRIANGLE_FAN
import org.openrndr.ktessellation.GLConstants.GL_TRIANGLE_STRIP
import org.openrndr.ktessellation.GLU
import org.openrndr.ktessellation.IndexedTessellator
import org.openrndr.ktessellation.Tessellator
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import kotlin.jvm.JvmName

enum class FillRule {
    EVEN_ODD,
    NON_ZERO,
}

/**
 * Triangulates a [Shape] into a [List] of triangle corner positions.
 *
 * @param distanceTolerance How refined should the triangular shape be, smaller values equate to higher precision.
 */
fun triangulate(
    shape: Shape,
    distanceTolerance: Double = 0.5,
    fillRule: FillRule = FillRule.NON_ZERO
): List<Vector2> {
    if (shape.contours.isEmpty() || shape.topology == ShapeTopology.OPEN) {
        return emptyList()
    }
    val tessellator = Tessellator()
    if (shape.topology == ShapeTopology.CLOSED) {
        when (fillRule) {
            FillRule.EVEN_ODD -> tessellator.gluTessProperty(GLU.GLU_TESS_WINDING_RULE, GLU.GLU_TESS_WINDING_ODD)
            FillRule.NON_ZERO -> tessellator.gluTessProperty(
                GLU.GLU_TESS_WINDING_RULE,
                GLU.GLU_TESS_WINDING_NONZERO
            )
        }
    }
    tessellator.gluTessBeginPolygon(null)
    for (contour in shape.closedContours) {
        if (contour.segments.isNotEmpty()) {
            tessellator.gluTessBeginContour()
            val positions = contour.adaptivePositions(distanceTolerance).dropLast(1)
            val positionData = DoubleArray(positions.size * 3) { 0.0 }
            var offset = 0
            for (i in positions.indices) {
                positionData[offset] = positions[i].x
                offset++
                positionData[offset] = positions[i].y
                offset++
                positionData[offset] = 0.0
                offset++
            }
            for (i in positions.indices) {
                tessellator.gluTessVertex(positionData, i * 3, doubleArrayOf(positions[i].x, positions[i].y, 0.0))
            }
            tessellator.gluTessEndContour()
        }
    }
    tessellator.gluTessEndPolygon()
    val result = mutableListOf<Vector2>()
    for (pd in tessellator.primitives) {
        if (pd.positions.isNotEmpty()) {
            when (pd.type) {
                GL_TRIANGLES -> {
                    result.addAll(pd.positions)
                }

                GL_TRIANGLE_FAN -> {
                    val fixed = pd.positions[0]
                    for (i in 1 until pd.positions.size - 1) {
                        result.add(fixed)
                        result.add(pd.positions[i])
                        result.add(pd.positions[i + 1])
                    }
                }

                GL_TRIANGLE_STRIP -> {
                    for (i in 0 until pd.positions.size - 2) {
                        result.add(pd.positions[i])
                        result.add(pd.positions[i + 1])
                        result.add(pd.positions[i + 2])
                    }
                }

                else -> error("type not supported: ${pd.type}")
            }
        }
    }
    for (i in result.indices step 3) {
        val a = result[i]
        val b = result[i + 1]
        val c = result[i + 2]
        if ((b.x - a.x) * (c.y - a.y) < (b.y - a.y) * (c.x - a.x)) {
            result[i + 1] = c
            result[i + 2] = b
        }
    }
    tessellator.gluDeleteTess()
    return result
}


/**
 * Triangles as indices of [vertices]
 * @property vertices the vertices of the contours, in order, followed by the vertices at intersections of the contours
 * @property indices three indices per triangle
 */
class IndexedTriangulation<T>(val vertices: List<T>, val indices: List<Int>)

/**
 * Tessellates [contours] with an [IndexedTessellator], [position] gives the coordinates of a vertex
 * @return the triangles as indices, and the coordinates of the vertices at intersections
 */
private fun <T> tessellateIndexed(
    contours: List<List<T>>,
    fillRule: FillRule,
    position: (T) -> Vector3
): Pair<List<Int>, List<DoubleArray>> {
    val tessellator = IndexedTessellator()
    when (fillRule) {
        FillRule.EVEN_ODD -> tessellator.gluTessProperty(GLU.GLU_TESS_WINDING_RULE, GLU.GLU_TESS_WINDING_ODD)
        FillRule.NON_ZERO -> tessellator.gluTessProperty(GLU.GLU_TESS_WINDING_RULE, GLU.GLU_TESS_WINDING_NONZERO)
    }
    tessellator.gluTessBeginPolygon(null)
    var vertexIndex = 0
    for (contour in contours) {
        if (contour.isNotEmpty()) {
            tessellator.gluTessBeginContour()
            val positionData = DoubleArray(contour.size * 3)
            for ((i, vertex) in contour.withIndex()) {
                val p = position(vertex)
                positionData[i * 3] = p.x
                positionData[i * 3 + 1] = p.y
                positionData[i * 3 + 2] = p.z
            }
            for (i in contour.indices) {
                tessellator.gluTessVertex(positionData, i * 3, vertexIndex)
                vertexIndex++
            }
            tessellator.gluTessEndContour()
        }
    }
    tessellator.gluTessEndPolygon()

    // the vertices at intersections follow the vertices of the contours
    fun resolve(index: Int) = if (index >= 0) index else vertexIndex + (-1 - index)

    val result = mutableListOf<Int>()
    for (pd in tessellator.primitives) {
        val indices = pd.indices
        if (indices.isEmpty()) continue
        when (pd.type) {
            GL_TRIANGLES -> indices.forEach { result.add(resolve(it)) }
            GL_TRIANGLE_FAN -> for (i in 1 until indices.size - 1) {
                result.add(resolve(indices[0]))
                result.add(resolve(indices[i]))
                result.add(resolve(indices[i + 1]))
            }
            // the odd triangles of a strip are reversed, all triangles of a strip have the same orientation
            GL_TRIANGLE_STRIP -> for (i in 0 until indices.size - 2) {
                if (i % 2 == 0) {
                    result.add(resolve(indices[i]))
                    result.add(resolve(indices[i + 1]))
                } else {
                    result.add(resolve(indices[i + 1]))
                    result.add(resolve(indices[i]))
                }
                result.add(resolve(indices[i + 2]))
            }
            else -> error("type not supported: ${pd.type}")
        }
    }
    tessellator.gluDeleteTess()
    return result to tessellator.newVertices
}

/** orients the triangles of [indices] counterclockwise in [vertices], like [triangulate] does for shapes */
private fun orient(indices: MutableList<Int>, vertices: List<Vector2>) {
    for (i in indices.indices step 3) {
        val a = vertices[indices[i]]
        val b = vertices[indices[i + 1]]
        val c = vertices[indices[i + 2]]
        // is c left of the line segment a--b?
        if ((b.x - a.x) * (c.y - a.y) < (b.y - a.y) * (c.x - a.x)) {
            val ib = indices[i + 1]
            indices[i + 1] = indices[i + 2]
            indices[i + 2] = ib
        }
    }
}

/**
 * Triangulates [contours] into indexed triangles. Contours may intersect: [IndexedTriangulation.vertices] holds the
 * vertices of the contours followed by the vertices at their intersections.
 */
fun triangulateIndexed(
    contours: List<List<Vector2>>,
    fillRule: FillRule = FillRule.NON_ZERO
): IndexedTriangulation<Vector2> {
    if (contours.isEmpty()) {
        return IndexedTriangulation(emptyList(), emptyList())
    }
    val (indices, newVertices) = tessellateIndexed(contours, fillRule) { it.xy0 }
    val vertices = contours.flatten() + newVertices.map { Vector2(it[0], it[1]) }
    val oriented = indices.toMutableList()
    orient(oriented, vertices)
    return IndexedTriangulation(vertices, oriented)
}

/**
 * Triangulates planar [contours] in 3D into indexed triangles. Contours may intersect:
 * [IndexedTriangulation.vertices] holds the vertices of the contours followed by the vertices at their intersections.
 */
@JvmName("triangulateIndexedV3")
fun triangulateIndexed(
    contours: List<List<Vector3>>,
    fillRule: FillRule = FillRule.NON_ZERO
): IndexedTriangulation<Vector3> {
    if (contours.isEmpty()) {
        return IndexedTriangulation(emptyList(), emptyList())
    }
    val (indices, newVertices) = tessellateIndexed(contours, fillRule) { it }
    val vertices = contours.flatten() + newVertices.map { Vector3(it[0], it[1], it[2]) }
    return IndexedTriangulation(vertices, indices)
}

/**
 * Triangulate [shape] into a [List] of indexed triangles, indices of the flattened [shape].
 * @throws IllegalArgumentException when contours intersect, their intersections need new vertices, see
 * [triangulateIndexed]
 */
@JvmName("triangulateV2")
fun triangulate(
    shape: List<List<Vector2>>,
    fillRule: FillRule = FillRule.NON_ZERO
): List<Int> {
    val triangulation = triangulateIndexed(shape, fillRule)
    val vertexCount = shape.sumOf { it.size }
    require(triangulation.vertices.size == vertexCount) {
        "the contours intersect, the triangulation has ${triangulation.vertices.size - vertexCount} new vertices, use triangulateIndexed"
    }
    return triangulation.indices
}

/**
 * Triangulate [shape] into a [List] of indexed triangles, indices of the flattened [shape].
 * @throws IllegalArgumentException when contours intersect, their intersections need new vertices, see
 * [triangulateIndexed]
 */
@JvmName("triangulateV3")
fun triangulate(
    shape: List<List<Vector3>>,
    fillRule: FillRule = FillRule.NON_ZERO
): List<Int> {
    val triangulation = triangulateIndexed(shape, fillRule)
    val vertexCount = shape.sumOf { it.size }
    require(triangulation.vertices.size == vertexCount) {
        "the contours intersect, the triangulation has ${triangulation.vertices.size - vertexCount} new vertices, use triangulateIndexed"
    }
    return triangulation.indices
}
