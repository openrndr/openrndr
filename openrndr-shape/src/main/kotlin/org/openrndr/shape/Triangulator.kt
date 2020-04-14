package org.openrndr.shape

import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.shape.tessellation.GLConstants
import org.openrndr.shape.tessellation.GLConstants.*
import org.openrndr.shape.tessellation.GLU
import org.openrndr.shape.tessellation.GLUtessellatorCallbackAdapter
import org.openrndr.shape.tessellation.Tessellator
import java.util.*

fun List<Int>.cumsum(): List<Int> {
    val result = mutableListOf<Int>()
    var sum = 0
    for (i in this) {
        sum += i
        result += sum
    }
    return result
}

/**
 * Indexed triangulation consisting of a list of vertices and triangle indices.
 */
class IndexedTriangulation<T>(val vertices: List<T>, val triangles: List<Int>)

///**
// * triangulates a [Shape] into a list of indexed triangles
// * @param shape the shape to triangulate
// * @param distanceTolerance how refined should the shape be, smaller values for higher precision
// */
//fun triangulateIndexed(shape: Shape, distanceTolerance: Double = 0.5): IndexedTriangulation<Vector2> {
//    val compounds = shape.splitCompounds().filter { it.topology == ShapeTopology.CLOSED }
//
//    val totalVertices = mutableListOf<Vector2>()
//    val totalIndices = mutableListOf<Int>()
//    var offset = 0
//    for (compound in compounds) {
//        val positions = shape.contours.map { it.adaptivePositions(distanceTolerance) }
//
//        val holes = if (shape.contours.size > 1) {
//            positions.dropLast(1).map { it.size }.cumsum().toIntArray()
//        } else {
//            null
//        }
//        val vertices = positions.flatMap { it }
//        val data = vertices.flatMap { listOf(it.x, it.y) }.toDoubleArray()
//        val indices = Triangulator.earcut(data, holes, 2).toList().map { it + offset }
//        offset += vertices.size
//
//        totalVertices.addAll(vertices)
//        totalIndices.addAll(indices)
//    }
//    return IndexedTriangulation(totalVertices, totalIndices)
//}
//
///**
// * Triangulate the polygon formed by `vertices`, this polygon has no holes
// */
//@JvmName("triangulateIndexed3")
//fun triangulateIndexed(vertices: List<Vector3>): IndexedTriangulation<Vector3> {
//    val data = vertices.flatMap { listOf(it.x, it.y, it.z) }.toDoubleArray()
//    val indices = Triangulator.earcut(data, null, 3).toList()
//    return IndexedTriangulation(vertices, indices)
//}
//
///**
// * Triangulate the polygon formed by `vertices`, this polygon has no holes
// */
//@JvmName("triangulateIndexed2")
//fun triangulateIndexed(vertices: List<Vector2>): IndexedTriangulation<Vector2> {
//    val data = vertices.flatMap { listOf(it.x, it.y) }.toDoubleArray()
//    val indices = Triangulator.earcut(data, null, 3).toList()
//    return IndexedTriangulation(vertices, indices)
//}


enum class FillRule {
    ODD,
    NONZERO_WINDING,
}

fun triangulate(
        shape: Shape,
        distanceTolerance: Double = 0.5,
        fillRule: FillRule = FillRule.NONZERO_WINDING
): List<Vector2> {
    if (shape.contours.isEmpty() || shape.topology == ShapeTopology.OPEN) {
        return emptyList()
    }
    val tessellator = Tessellator()
    if (shape.topology == ShapeTopology.CLOSED) {
        when (fillRule) {
            FillRule.ODD -> tessellator.gluTessProperty(GLU.GLU_TESS_WINDING_RULE, GLU.GLU_TESS_WINDING_ODD)
            FillRule.NONZERO_WINDING -> tessellator.gluTessProperty(GLU.GLU_TESS_WINDING_RULE, GLU.GLU_TESS_WINDING_NONZERO)
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
    tessellator.gluDeleteTess()
    return result
}

