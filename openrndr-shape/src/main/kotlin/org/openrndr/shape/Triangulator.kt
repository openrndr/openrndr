package org.openrndr.shape

import org.openrndr.math.Vector2


/** A simple implementation of the ear cutting algorithm to triangulate simple polygons without holes. For more information:
 *
 *  * [http://cgm.cs.mcgill.ca/~godfried/
 * teaching/cg-projects/97/Ian/algorithm2.html](http://cgm.cs.mcgill.ca/~godfried/teaching/cg-projects/97/Ian/algorithm2.html)
 *  * [http://www.geometrictools.com/Documentation
 * /TriangulationByEarClipping.pdf](http://www.geometrictools.com/Documentation/TriangulationByEarClipping.pdf)
 *
 * If the input polygon is not simple (self-intersects), there will be output but it is of unspecified quality (garbage in,
 * garbage out).
 * @author badlogicgames@gmail.com
 * @author Nicolas Gramlich (optimizations, collinear edge support)
 * @author Eric Spitz
 * @author Thomas ten Cate (bugfixes, optimizations)
 * @author Nathan Sweet (rewrite, return indices, no allocation, optimizations)
 */
class Triangulator {

    private var indices: MutableList<Short>? = null
    private var vertices: List<Float>? = null
    private var vertexCount: Int = 0
    private val vertexTypes = mutableListOf<Int>()
    private val triangles = mutableListOf<Short>()



    fun triangulate(contour: ShapeContour):List<Vector2> {
        val vertices = contour.adaptivePositions().let { it.subList(0, it.size-1) }
        val floats = vertices.flatMap { listOf(it.x.toFloat(), it.y.toFloat()) }
        val indices = computeTriangles(floats)
        return indices.map { vertices[it.toInt()] }
    }



    /** Triangulates the given (convex or concave) simple polygon to a list of triangle vertices.
     * @param vertices pairs describing vertices of the polygon, in either clockwise or counterclockwise order.
     * @return triples of triangle indices in clockwise order. Note the returned array is reused for later calls to the same
     * method.
     */
    fun computeTriangles(vertices: List<Float>, offset: Int = 0, count: Int = vertices.size): List<Short> {
        this.vertices = vertices
        this.vertexCount = count / 2
        val vertexCount = this.vertexCount
        val vertexOffset = offset / 2

        //indicesArray.ensureCapacity(vertexCount)
        //indicesArray.size = vertexCount
        indices = mutableListOf()
        for (i in 0 until vertexCount) {
            indices!!.add(0)
        }
        val indices = this.indices
        if (areVerticesClockwise(vertices, offset, count)) {
            for (i in 0 until vertexCount)
                indices!![i] = (vertexOffset + i).toShort()
        } else {
            var i = 0
            val n = vertexCount - 1
            while (i < vertexCount) {
                indices!![i] = (vertexOffset + n - i).toShort()
                i++
            } // Reversed.
        }

        val vertexTypes = this.vertexTypes
        vertexTypes.clear()
        //vertexTypes.ensureCapacity(vertexCount)
        var i = 0
        while (i < vertexCount) {
            vertexTypes.add(classifyVertex(i))
            ++i
        }

        // A polygon with n vertices has a triangulation of n-2 triangles.
        val triangles = this.triangles
        triangles.clear()
        //triangles.ensureCapacity(Math.max(0, vertexCount - 2) * 3)
        triangulate()
        return triangles
    }

    private fun triangulate() {
        val vertexTypes = this.vertexTypes

        while (vertexCount > 3) {
            val earTipIndex = findEarTip()
            cutEarTip(earTipIndex)

            // The type of the two vertices adjacent to the clipped vertex may have changed.
            val previousIndex = previousIndex(earTipIndex)
            val nextIndex = if (earTipIndex == vertexCount) 0 else earTipIndex
            vertexTypes[previousIndex] = classifyVertex(previousIndex)
            vertexTypes[nextIndex] = classifyVertex(nextIndex)
        }

        if (vertexCount == 3) {
            val triangles = this.triangles
            val indices = this.indices
            triangles.add(indices!![0])
            triangles.add(indices[1])
            triangles.add(indices[2])
        }
    }

    /** @return [.CONCAVE] or [.CONVEX]
     */
    private fun classifyVertex(index: Int): Int {
        val indices = this.indices
        val previous = indices!![previousIndex(index)] * 2
        val current = indices[index] * 2
        val next = indices[nextIndex(index)] * 2
        val vertices = this.vertices
        return computeSpannedAreaSign(vertices!![previous], vertices[previous + 1], vertices[current], vertices[current + 1],
                vertices[next], vertices[next + 1])
    }

    private fun findEarTip(): Int {
        val vertexCount = this.vertexCount
        for (i in 0 until vertexCount)
            if (isEarTip(i)) return i

        // Desperate mode: if no vertex is an ear tip, we are dealing with a degenerate polygon (e.g. nearly collinear).
        // Note that the input was not necessarily degenerate, but we could have made it so by clipping some valid ears.

        // Idea taken from Martin Held, "FIST: Fast industrial-strength triangulation of polygons", Algorithmica (1998),
        // http://citeseerx.ist.psu.edu/viewdoc/summary?doi=10.1.1.115.291

        // Return a convex or tangential vertex if one exists.
        val vertexTypes = this.vertexTypes
        for (i in 0 until vertexCount)
            if (vertexTypes[i] != CONCAVE) return i
        return 0 // If all vertices are concave, just return the first one.
    }

    private fun isEarTip(earTipIndex: Int): Boolean {
        val vertexTypes = this.vertexTypes
        if (vertexTypes[earTipIndex] == CONCAVE) return false

        val previousIndex = previousIndex(earTipIndex)
        val nextIndex = nextIndex(earTipIndex)
        val indices = this.indices
        val p1 = indices!![previousIndex] * 2
        val p2 = indices[earTipIndex] * 2
        val p3 = indices[nextIndex] * 2
        val vertices = this.vertices
        val p1x = vertices!![p1]
        val p1y = vertices[p1 + 1]
        val p2x = vertices[p2]
        val p2y = vertices[p2 + 1]
        val p3x = vertices[p3]
        val p3y = vertices[p3 + 1]

        // Check if any point is inside the triangle formed by previous, current and next vertices.
        // Only consider vertices that are not part of this triangle, or else we'll always find one inside.
        var i = nextIndex(nextIndex)
        while (i != previousIndex) {
            // Concave vertices can obviously be inside the candidate ear, but so can tangential vertices
            // if they coincide with one of the triangle's vertices.
            if (vertexTypes[i] != CONVEX) {
                val v = indices[i] * 2
                val vx = vertices[v]
                val vy = vertices[v + 1]
                // Because the polygon has clockwise winding order, the area sign will be positive if the point is strictly inside.
                // It will be 0 on the edge, which we want to include as well.
                // note: check the edge defined by p1->p3 first since this fails _far_ more then the other 2 checks.
                if (computeSpannedAreaSign(p3x, p3y, p1x, p1y, vx, vy) >= 0) {
                    if (computeSpannedAreaSign(p1x, p1y, p2x, p2y, vx, vy) >= 0) {
                        if (computeSpannedAreaSign(p2x, p2y, p3x, p3y, vx, vy) >= 0) return false
                    }
                }
            }
            i = nextIndex(i)
        }
        return true
    }

    private fun cutEarTip(earTipIndex: Int) {
        val indices = this.indices
        val triangles = this.triangles

        triangles.add(indices!![previousIndex(earTipIndex)])
        triangles.add(indices[earTipIndex])
        triangles.add(indices[nextIndex(earTipIndex)])

        indices.removeAt(earTipIndex)
        vertexTypes.removeAt(earTipIndex)
        vertexCount--
    }

    private fun previousIndex(index: Int): Int {
        return (if (index == 0) vertexCount else index) - 1
    }

    private fun nextIndex(index: Int): Int {
        return (index + 1) % vertexCount
    }

    companion object {
        private val CONCAVE = -1
        private val CONVEX = 1

        private fun areVerticesClockwise(vertices: List<Float>, offset: Int, count: Int): Boolean {
            if (count <= 2) return false
            var area = 0f
            var p1x: Float
            var p1y: Float
            var p2x: Float
            var p2y: Float
            var i = offset
            val n = offset + count - 3
            while (i < n) {
                p1x = vertices[i]
                p1y = vertices[i + 1]
                p2x = vertices[i + 2]
                p2y = vertices[i + 3]
                area += p1x * p2y - p2x * p1y
                i += 2
            }
            p1x = vertices[offset + count - 2]
            p1y = vertices[offset + count - 1]
            p2x = vertices[offset]
            p2y = vertices[offset + 1]
            return area + p1x * p2y - p2x * p1y < 0
        }

        private fun computeSpannedAreaSign(p1x: Float, p1y: Float, p2x: Float, p2y: Float, p3x: Float, p3y: Float): Int {
            var area = p1x * (p3y - p2y)
            area += p2x * (p1y - p3y)
            area += p3x * (p2y - p1y)
            return Math.signum(area).toInt()
        }
    }
}
/** @see .computeTriangles
 */