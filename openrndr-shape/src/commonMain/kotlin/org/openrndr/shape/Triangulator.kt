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
    ODD,
    NONZERO_WINDING,
}

/**
 * Triangulates a [Shape] into a [List] of triangle corner positions.
 *
 * @param distanceTolerance How refined should the triangular shape be, smaller values equate to higher precision.
 */
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
            FillRule.NONZERO_WINDING -> tessellator.gluTessProperty(
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
 * Triangulate [shape] into a [List] of indexed triangles
 */
@JvmName("triangulateV2")
fun triangulate(
    shape: List<List<Vector2>>,
    fillRule: FillRule = FillRule.NONZERO_WINDING
): List<Int> {
    if (shape.isEmpty()) {
        return emptyList()
    }
    val tessellator = IndexedTessellator()

    when (fillRule) {
        FillRule.ODD -> tessellator.gluTessProperty(GLU.GLU_TESS_WINDING_RULE, GLU.GLU_TESS_WINDING_ODD)
        FillRule.NONZERO_WINDING -> tessellator.gluTessProperty(GLU.GLU_TESS_WINDING_RULE, GLU.GLU_TESS_WINDING_NONZERO)
    }

    tessellator.gluTessBeginPolygon(null)
    var vertexIndex = 0
    val flatVertices = shape.flatten()

    for (contour in shape) {
        if (contour.isNotEmpty()) {
            tessellator.gluTessBeginContour()
            val positions = contour
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
                tessellator.gluTessVertex(positionData, i * 3, vertexIndex)
                vertexIndex++
            }
            tessellator.gluTessEndContour()
        }
    }
    tessellator.gluTessEndPolygon()
    val result = mutableListOf<Int>()
    for (pd in tessellator.primitives) {
        if (pd.indices.isNotEmpty()) {
            when (pd.type) {
                GL_TRIANGLES -> {
                    result.addAll(pd.indices)
                }

                GL_TRIANGLE_FAN -> {
                    val fixed = pd.indices[0]
                    for (i in 1 until pd.indices.size - 1) {
                        result.add(fixed)
                        result.add(pd.indices[i])
                        result.add(pd.indices[i + 1])
                    }
                }

                GL_TRIANGLE_STRIP -> {
                    for (i in 0 until pd.indices.size - 2) {
                        result.add(pd.indices[i])
                        result.add(pd.indices[i + 1])
                        result.add(pd.indices[i + 2])
                    }
                }

                else -> error("type not supported: ${pd.type}")
            }
        }
    }
    for (i in result.indices step 3) {
        val ia = result[i]
        val ib = result[i + 1]
        val ic = result[i + 2]

        val a = flatVertices[ia]
        val b = flatVertices[ib]
        val c = flatVertices[ic]

        // is c left of the line segment a--b?
        if ((b.x - a.x) * (c.y - a.y) < (b.y - a.y) * (c.x - a.x)) {
            result[i + 1] = ic
            result[i + 2] = ib
        }
    }
    tessellator.gluDeleteTess()
    return result
}


/**
 * Triangulate [shape] into a [List] of indexed triangles
 */
@JvmName("triangulateV3")
fun triangulate(
    shape: List<List<Vector3>>,
    fillRule: FillRule = FillRule.NONZERO_WINDING
): List<Int> {
    if (shape.isEmpty()) {
        return emptyList()
    }
    val tessellator = IndexedTessellator()

    when (fillRule) {
        FillRule.ODD -> tessellator.gluTessProperty(GLU.GLU_TESS_WINDING_RULE, GLU.GLU_TESS_WINDING_ODD)
        FillRule.NONZERO_WINDING -> tessellator.gluTessProperty(GLU.GLU_TESS_WINDING_RULE, GLU.GLU_TESS_WINDING_NONZERO)
    }

    tessellator.gluTessBeginPolygon(null)
    var vertexIndex = 0
    val flatVertices = shape.flatten()

    for (contour in shape) {
        if (contour.isNotEmpty()) {
            tessellator.gluTessBeginContour()
            val positions = contour
            val positionData = DoubleArray(positions.size * 3) { 0.0 }
            var offset = 0
            for (i in positions.indices) {
                positionData[offset] = positions[i].x
                offset++
                positionData[offset] = positions[i].y
                offset++
                positionData[offset] = positions[i].z
                offset++
            }
            for (i in positions.indices) {
                tessellator.gluTessVertex(positionData, i * 3, vertexIndex)
                vertexIndex++
            }
            tessellator.gluTessEndContour()
        }
    }
    tessellator.gluTessEndPolygon()
    val result = mutableListOf<Int>()
    for (pd in tessellator.primitives) {
        if (pd.indices.isNotEmpty()) {
            when (pd.type) {
                GL_TRIANGLES -> {
                    result.addAll(pd.indices)
                }

                GL_TRIANGLE_FAN -> {
                    val fixed = pd.indices[0]
                    for (i in 1 until pd.indices.size - 1) {
                        result.add(fixed)
                        result.add(pd.indices[i])
                        result.add(pd.indices[i + 1])
                    }
                }

                GL_TRIANGLE_STRIP -> {
                    for (i in 0 until pd.indices.size - 2) {
                        result.add(pd.indices[i])
                        result.add(pd.indices[i + 1])
                        result.add(pd.indices[i + 2])
                    }
                }

                else -> error("type not supported: ${pd.type}")
            }
        }
    }
//    for (i in result.indices step 3) {
//        val ia = result[i]
//        val ib = result[i + 1]
//        val ic = result[i + 2]
//
//        val a = flatVertices[ia]
//        val b = flatVertices[ib]
//        val c = flatVertices[ic]
//
////        // is c left of the line segment a--b?
////        if ((b.x - a.x) * (c.y - a.y) < (b.y - a.y) * (c.x - a.x)) {
////            result[i + 1] = ic
////            result[i + 2] = ib
////        }
//    }
    tessellator.gluDeleteTess()
    return result
}