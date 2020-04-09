package org.openrndr.shape

import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
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
 * triangulates a [Shape] into a list of triangles
 * @param shape the shape to triangulate
 * @param distanceTolerance how refined should the shape be, smaller values for higher precision
 */
fun triangulate(shape: Shape, distanceTolerance: Double = 0.5): List<Vector2> {
    val compounds = shape.splitCompounds()
    val result = mutableListOf<Vector2>()
    for (compound in compounds) {
        val positions = compound.contours.map { it.adaptivePositions(distanceTolerance) }

        val holes = if (shape.contours.size > 1) {
            positions.dropLast(1).map { it.size }.cumsum().toIntArray()
        } else {
            null
        }

        val data = positions.flatMap { it.flatMap { listOf(it.x, it.y) } }.toDoubleArray()
        val indices = Triangulator.earcut(data, holes, 2)
        for (i in indices) {
            result.add(Vector2(data[i * 2], data[i * 2 + 1]))
        }
    }
    return result

}

/**
 * Indexed triangulation consisting of a list of vertices and triangle indices.
 */
class IndexedTriangulation<T>(val vertices: List<T>, val triangles: List<Int>)

/**
 * triangulates a [Shape] into a list of indexed triangles
 * @param shape the shape to triangulate
 * @param distanceTolerance how refined should the shape be, smaller values for higher precision
 */
fun triangulateIndexed(shape: Shape, distanceTolerance: Double = 0.5): IndexedTriangulation<Vector2> {
    val positions = shape.contours.map { it.adaptivePositions(distanceTolerance) }

    val holes = if (shape.contours.size > 1) {
        positions.dropLast(1).map { it.size }.cumsum().toIntArray()
    } else {
        null
    }
    val vertices = positions.flatMap { it }
    val data = vertices.flatMap { listOf(it.x, it.y) }.toDoubleArray()
    val indices = Triangulator.earcut(data, holes, 2).toList()
    return IndexedTriangulation(vertices, indices)
}

/**
 * Triangulate the polygon formed by `vertices`, this polygon has no holes
 */
@JvmName("triangulateIndexed3")
fun triangulateIndexed(vertices: List<Vector3>): IndexedTriangulation<Vector3> {
    val data = vertices.flatMap { listOf(it.x, it.y, it.z) }.toDoubleArray()
    val indices = Triangulator.earcut(data, null, 3).toList()
    return IndexedTriangulation(vertices, indices)
}

/**
 * Triangulate the polygon formed by `vertices`, this polygon has no holes
 */
@JvmName("triangulateIndexed2")
fun triangulateIndexed(vertices: List<Vector2>): IndexedTriangulation<Vector2> {
    val data = vertices.flatMap { listOf(it.x, it.y) }.toDoubleArray()
    val indices = Triangulator.earcut(data, null, 3).toList()
    return IndexedTriangulation(vertices, indices)
}


/**
 * ISC License

Copyright (c) 2016, Mapbox

Permission to use, copy, modify, and/or distribute this software for any purpose
with or without fee is hereby granted, provided that the above copyright notice
and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH
REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND
FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT,
INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS
OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER
TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
THIS SOFTWARE.


This is a pretty hacky port of
https://github.com/earcut4j/earcut4j/blob/a5b26b5644940bba9bd529a741491d90f16d1a31/src/main/java/earcut4j/Earcut.java
 */

internal object Triangulator {
    /**
     * Triangulates the given polygon
     *
     * @param data is a flat array of vertice coordinates like [x0,y0, x1,y1, x2,y2, ...].
     * @param holeIndices is an array of hole indices if any (e.g. [5, 8] for a 12-vertice input would mean one hole with vertices 5–7 and another with 8–11).
     * @param dim  is the number of coordinates per vertice in the input array
     * @return List containing groups of three vertice indices in the resulting array forms a triangle.
     */
    internal fun earcut(data: DoubleArray, holeIndices: IntArray? = null, dim: Int = 2): List<Int> {

        val hasHoles = holeIndices != null && holeIndices.size > 0
        val outerLen = if (hasHoles) holeIndices!![0] * dim else data.size

        var outerNode = linkedList(data, 0, outerLen, dim, true)

        val triangles = ArrayList<Int>()

        if (outerNode == null)
            return triangles

        var minX = 0.0
        var minY = 0.0
        var maxX: Double
        var maxY: Double
        var size = Double.MIN_VALUE

        if (hasHoles)
            outerNode = eliminateHoles(data, holeIndices!!, outerNode, dim)

        // if the shape is not too simple, we'll use z-order curve hash later;
        // calculate polygon bbox
        if (data.size > 80 * dim) {
            maxX = data[0]
            minX = maxX
            maxY = data[1]
            minY = maxY

            var i = dim
            while (i < outerLen) {
                val x = data[i]
                val y = data[i + 1]
                if (x < minX)
                    minX = x
                if (y < minY)
                    minY = y
                if (x > maxX)
                    maxX = x
                if (y > maxY)
                    maxY = y
                i += dim
            }

            // minX, minY and size are later used to transform coords into
            // integers for z-order calculation
            size = Math.max(maxX - minX, maxY - minY)
        }

        earcutLinked(outerNode, triangles, dim, minX, minY, size, Integer.MIN_VALUE)

        return triangles
    }

    private fun earcutLinked(ear: Node?, triangles: MutableList<Int>, dim: Int, minX: Double, minY: Double, size: Double, pass: Int) {
        var earRef: Node? = ear ?: return

        // interlink polygon nodes in z-order
        if (pass == Integer.MIN_VALUE && size != java.lang.Double.MIN_VALUE)
            indexCurve(earRef, minX, minY, size)

        var stop = earRef

        // iterate through ears, slicing them one by one
        while (earRef!!.prev !== earRef!!.next) {
            val prev = earRef!!.prev
            val next = earRef.next

            if (if (size != java.lang.Double.MIN_VALUE) isEarHashed(earRef, minX, minY, size) else isEar(earRef)) {
                // cut off the triangle
                triangles.add(prev!!.i / dim)
                triangles.add(earRef.i / dim)
                triangles.add(next!!.i / dim)

                removeNode(earRef)

                // skipping the next vertice leads to less sliver triangles
                earRef = next.next
                stop = next.next

                continue
            }

            earRef = next

            // if we looped through the whole remaining polygon and can't find
            // any more ears
            if (earRef === stop) {
                // try filtering points and slicing again
                if (pass == Integer.MIN_VALUE) {
                    earcutLinked(filterPoints(earRef, null), triangles, dim, minX, minY, size, 1)

                    // if this didn't work, try curing all small
                    // self-intersections locally
                } else if (pass == 1) {
                    earRef = cureLocalIntersections(earRef
                            ?: throw IllegalStateException("ear is null"), triangles, dim)
                    earcutLinked(earRef, triangles, dim, minX, minY, size, 2)

                    // as a last resort, try splitting the remaining polygon
                    // into two
                } else if (pass == 2) {
                    splitEarcut(earRef ?: throw IllegalStateException("ear is null"), triangles, dim, minX, minY, size)
                }

                break
            }
        }
    }

    private fun splitEarcut(start: Node, triangles: MutableList<Int>, dim: Int, minX: Double, minY: Double, size: Double) {
        // look for a valid diagonal that divides the polygon into two
        var a: Node? = start
        do {
            var b = a!!.next!!.next
            while (b !== a!!.prev) {
                if (a!!.i != b!!.i && isValidDiagonal(a, b)) {
                    // split the polygon in two by the diagonal
                    var c: Node? = splitPolygon(a, b)

                    // filter colinear points around the cuts
                    a = filterPoints(a, a.next)
                    c = filterPoints(c, c!!.next)

                    // run earcut on each half
                    earcutLinked(a, triangles, dim, minX, minY, size, Integer.MIN_VALUE)
                    earcutLinked(c, triangles, dim, minX, minY, size, Integer.MIN_VALUE)
                    return
                }
                b = b.next
            }
            a = a!!.next
        } while (a !== start)
    }

    private fun isValidDiagonal(a: Node, b: Node): Boolean {
        return a.next!!.i != b.i && a.prev!!.i != b.i && !intersectsPolygon(a, b) && locallyInside(a, b) && locallyInside(b, a) && middleInside(a, b)
    }

    private fun middleInside(a: Node, b: Node): Boolean {
        var p: Node? = a
        var inside = false
        val px = (a.x + b.x) / 2
        val py = (a.y + b.y) / 2
        do {
            if (p!!.y > py != p.next!!.y > py && px < (p.next!!.x - p.x) * (py - p.y) / (p.next!!.y - p.y) + p.x)
                inside = !inside
            p = p.next
        } while (p !== a)

        return inside
    }

    private fun intersectsPolygon(a: Node, b: Node): Boolean {
        var p: Node = a
        do {
            if (p.i != a.i && p.next!!.i != a.i && p.i != b.i && p.next!!.i != b.i && intersects(p, p.next
                            ?: throw IllegalStateException("p.next is null"), a, b))
                return true
            p = p.next ?: throw IllegalStateException("p.next is null")
        } while (p !== a)

        return false
    }

    private fun intersects(p1: Node, q1: Node, p2: Node, q2: Node): Boolean {
        return if (equals(p1, q1) && equals(p2, q2) || equals(p1, q2) && equals(p2, q1)) true else area(p1, q1, p2) > 0 != area(p1, q1, q2) > 0 && area(p2, q2, p1) > 0 != area(p2, q2, q1) > 0
    }

    private fun cureLocalIntersections(start: Node, triangles: MutableList<Int>, dim: Int): Node {
        var startRef = start
        var p: Node = startRef
        do {
            val a = p.prev!!
            val b = p.next!!.next!!

            if (!equals(a, b) && intersects(a, p, p.next
                            ?: throw IllegalStateException("p.next is null"), b) && locallyInside(a, b) && locallyInside(b, a)) {

                triangles.add(a.i / dim)
                triangles.add(p.i / dim)
                triangles.add(b.i / dim)

                // remove two nodes involved
                removeNode(p)
                removeNode(p.next!!)

                startRef = b
                p = startRef
            }
            p = p.next!!
        } while (p !== startRef)

        return p
    }

    private fun isEar(ear: Node): Boolean {
        val a = ear.prev
        val c = ear.next

        if (area(a!!, ear, c!!) >= 0)
            return false // reflex, can't be an ear

        // now make sure we don't have other points inside the potential ear
        var p = ear.next!!.next

        while (p !== ear.prev) {
            if (pointInTriangle(a.x, a.y, ear.x, ear.y, c.x, c.y, p!!.x, p.y) && area(p.prev!!, p, p.next!!) >= 0)
                return false
            p = p.next
        }

        return true
    }

    private fun isEarHashed(ear: Node, minX: Double, minY: Double, size: Double): Boolean {
        val a = ear.prev
        val c = ear.next

        if (area(a!!, ear, c!!) >= 0)
            return false // reflex, can't be an ear

        // triangle bbox; min & max are calculated like this for speed
        val minTX = if (a.x < ear.x) if (a.x < c.x) a.x else c.x else if (ear.x < c.x) ear.x else c.x
        val minTY = if (a.y < ear.y) if (a.y < c.y) a.y else c.y else if (ear.y < c.y) ear.y else c.y
        val maxTX = if (a.x > ear.x) if (a.x > c.x) a.x else c.x else if (ear.x > c.x) ear.x else c.x
        val maxTY = if (a.y > ear.y) if (a.y > c.y) a.y else c.y else if (ear.y > c.y) ear.y else c.y

        // z-order range for the current triangle bbox;
        val minZ = zOrder(minTX, minTY, minX, minY, size)
        val maxZ = zOrder(maxTX, maxTY, minX, minY, size)

        // first look for points inside the triangle in increasing z-order
        var p = ear.nextZ

        while (p != null && p.z <= maxZ) {
            if (p !== ear.prev && p !== ear.next && pointInTriangle(a.x, a.y, ear.x, ear.y, c.x, c.y, p.x, p.y) && area(p.prev!!, p, p.next!!) >= 0)
                return false
            p = p.nextZ
        }

        // then look for points in decreasing z-order
        p = ear.prevZ

        while (p != null && p.z >= minZ) {
            if (p !== ear.prev && p !== ear.next && pointInTriangle(a.x, a.y, ear.x, ear.y, c.x, c.y, p.x, p.y) && area(p.prev!!, p, p.next!!) >= 0)
                return false
            p = p.prevZ
        }

        return true
    }

    private fun zOrder(x: Double, y: Double, minX: Double, minY: Double, size: Double): Double {

        // coords are transformed into non-negative 15-bit integer range
        var lx = (32767 * (x - minX) / size).toInt()
        var ly = (32767 * (y - minY) / size).toInt()

        lx = lx or (lx shl 8) and 0x00FF00FF
        lx = lx or (lx shl 4) and 0x0F0F0F0F
        lx = lx or (lx shl 2) and 0x33333333
        lx = lx or (lx shl 1) and 0x55555555

        ly = ly or (ly shl 8) and 0x00FF00FF
        ly = ly or (ly shl 4) and 0x0F0F0F0F
        ly = ly or (ly shl 2) and 0x33333333
        ly = ly or (ly shl 1) and 0x55555555

        return (lx or (ly shl 1)).toDouble()
    }

    private fun indexCurve(start: Node?, minX: Double, minY: Double, size: Double) {
        var p: Node = start ?: return
        do {
            if (p.z == java.lang.Double.MIN_VALUE)
                p.z = zOrder(p.x, p.y, minX, minY, size)
            p.prevZ = p.prev
            p.nextZ = p.next
            p = p.next ?: throw IllegalStateException("p.next is null")
        } while (p !== start)

        p.prevZ?.nextZ = null
        p.prevZ = null

        sortLinked(p)
    }

    private fun sortLinked(list: Node?): Node? {
        var listRef = list
        var inSize = 1


        var numMerges: Int
        do {
            var p = listRef
            listRef = null
            var tail: Node? = null
            numMerges = 0

            while (p != null) {
                numMerges++
                var q = p
                var pSize = 0
                for (i in 0 until inSize) {
                    pSize++
                    q = q!!.nextZ
                    if (q == null)
                        break
                }

                var qSize = inSize

                while (pSize > 0 || qSize > 0 && q != null) {
                    val e: Node
                    if (pSize == 0) {
                        e = q ?: throw IllegalStateException("q is null")
                        q = q.nextZ
                        qSize--
                    } else if (qSize == 0 || q == null) {
                        e = p ?: throw IllegalStateException("p is null")
                        p = p.nextZ
                        pSize--
                    } else if (p!!.z <= q.z) {
                        e = p
                        p = p.nextZ
                        pSize--
                    } else {
                        e = q
                        q = q.nextZ
                        qSize--
                    }

                    if (tail != null)
                        tail.nextZ = e
                    else
                        listRef = e

                    e.prevZ = tail
                    tail = e
                }

                p = q
            }

            tail!!.nextZ = null
            inSize *= 2

        } while (numMerges > 1)

        return listRef
    }

    private fun eliminateHoles(data: DoubleArray, holeIndices: IntArray, outerNode: Node?, dim: Int): Node? {
        var outerNodeRef = outerNode
        val queue = ArrayList<Node>()

        val len = holeIndices.size
        for (i in 0 until len) {
            val start = holeIndices[i] * dim
            val end = if (i < len - 1) holeIndices[i + 1] * dim else data.size
            val list = linkedList(data, start, end, dim, false) ?: throw IllegalStateException("list is null")
            if (list === list.next)
                list.steiner = true
            queue.add(getLeftmost(list))
        }

        queue.sortWith(Comparator { o1, o2 ->
            if (o1.x - o2.x > 0)
                return@Comparator 1
            else if (o1.x - o2.x < 0)
                return@Comparator -2
            0
        })

        for (node in queue) {
            eliminateHole(node, outerNodeRef)
            outerNodeRef = filterPoints(outerNodeRef, outerNodeRef!!.next)
        }

        return outerNodeRef
    }

    private fun filterPoints(start: Node?, end: Node?): Node? {
        var endRef = end
        if (start == null)
            return start
        if (endRef == null)
            endRef = start

        var p = start
        var again: Boolean

        do {
            again = false

            if (!p!!.steiner && equals(p, p.next!!) || area(p.prev!!, p, p.next!!) == 0.0) {
                removeNode(p)
                endRef = p.prev
                p = endRef
                if (p === p!!.next)
                    return null
                again = true
            } else {
                p = p.next
            }
        } while (again || p !== endRef)

        return endRef
    }

    private fun equals(p1: Node, p2: Node): Boolean {
        return p1.x == p2.x && p1.y == p2.y
    }

    private fun area(p: Node, q: Node, r: Node): Double {
        return (q.y - p.y) * (r.x - q.x) - (q.x - p.x) * (r.y - q.y)
    }

    private fun eliminateHole(hole: Node, outerNode: Node?) {
        var outerNodeRef = outerNode
        outerNodeRef = findHoleBridge(hole, outerNodeRef)
        if (outerNodeRef != null) {
            val b = splitPolygon(outerNodeRef, hole)
            filterPoints(b, b.next)
        }
    }

    private fun splitPolygon(a: Node, b: Node): Node {
        val a2 = Node(a.i, a.x, a.y)
        val b2 = Node(b.i, b.x, b.y)
        val an = a.next
        val bp = b.prev

        a.next = b
        b.prev = a

        a2.next = an
        an!!.prev = a2

        b2.next = a2
        a2.prev = b2

        bp!!.next = b2
        b2.prev = bp

        return b2
    }

    // David Eberly's algorithm for finding a bridge between hole and outer
    // polygon
    private fun findHoleBridge(hole: Node, outerNode: Node?): Node? {
        var p = outerNode
        val hx = hole.x
        val hy = hole.y
        var qx = -java.lang.Double.MAX_VALUE
        var m: Node? = null

        // find a segment intersected by a ray from the hole's leftmost point to
        // the left;
        // segment's endpoint with lesser x will be potential connection point
        do {
            if (hy <= p!!.y && hy >= p.next!!.y) {
                val x = p.x + (hy - p.y) * (p.next!!.x - p.x) / (p.next!!.y - p.y)
                if (x <= hx && x > qx) {
                    qx = x
                    if (x == hx) {
                        if (hy == p.y)
                            return p
                        if (hy == p.next!!.y)
                            return p.next
                    }
                    m = if (p.x < p.next!!.x) p else p.next
                }
            }
            p = p.next
        } while (p !== outerNode)

        if (m == null)
            return null

        if (hx == qx)
            return m.prev // hole touches outer segment; pick lower endpoint

        // look for points inside the triangle of hole point, segment
        // intersection and endpoint;
        // if there are no points found, we have a valid connection;
        // otherwise choose the point of the minimum angle with the ray as
        // connection point

        val stop = m
        val mx = m.x
        val my = m.y
        var tanMin = java.lang.Double.MAX_VALUE
        var tan: Double

        p = m.next

        while (p !== stop) {
            if (hx >= p!!.x && p.x >= mx && pointInTriangle(if (hy < my) hx else qx, hy, mx, my, if (hy < my) qx else hx, hy, p.x, p.y)) {

                tan = Math.abs(hy - p.y) / (hx - p.x) // tangential

                if ((tan < tanMin || tan == tanMin && p.x > m!!.x) && locallyInside(p, hole)) {
                    m = p
                    tanMin = tan
                }
            }

            p = p.next
        }

        return m
    }

    private fun locallyInside(a: Node, b: Node): Boolean {
        return if (area(a.prev!!, a, a.next!!) < 0) area(a, b, a.next!!) >= 0 && area(a, a.prev!!, b) >= 0 else area(a, b, a.prev!!) < 0 || area(a, a.next!!, b) < 0
    }

    private fun pointInTriangle(ax: Double, ay: Double, bx: Double, by: Double, cx: Double, cy: Double, px: Double, py: Double): Boolean {
        return ((cx - px) * (ay - py) - (ax - px) * (cy - py) >= 0 && (ax - px) * (by - py) - (bx - px) * (ay - py) >= 0
                && (bx - px) * (cy - py) - (cx - px) * (by - py) >= 0)
    }

    private fun getLeftmost(start: Node): Node {
        var p: Node? = start
        var leftmost = start
        do {
            if (p!!.x < leftmost.x)
                leftmost = p
            p = p.next
        } while (p !== start)
        return leftmost
    }

    private fun linkedList(data: DoubleArray, start: Int, end: Int, dim: Int, clockwise: Boolean): Node? {
        var last: Node? = null
        if (clockwise == signedArea(data, start, end, dim) > 0) {
            var i = start
            while (i < end) {
                last = insertNode(i, data[i], data[i + 1], last)
                i += dim
            }
        } else {
            var i = end - dim
            while (i >= start) {
                last = insertNode(i, data[i], data[i + 1], last)
                i -= dim
            }
        }

        if (last != null && equals(last, last.next!!)) {
            removeNode(last)
            last = last.next
        }
        return last
    }

    private fun removeNode(p: Node) {
        p.next!!.prev = p.prev
        p.prev!!.next = p.next

        if (p.prevZ != null) {
            p.prevZ!!.nextZ = p.nextZ
        }
        if (p.nextZ != null) {
            p.nextZ!!.prevZ = p.prevZ
        }
    }

    private fun insertNode(i: Int, x: Double, y: Double, last: Node?): Node {
        val p = Node(i, x, y)

        if (last == null) {
            p.prev = p
            p.next = p
        } else {
            p.next = last.next
            p.prev = last
            last.next!!.prev = p
            last.next = p
        }
        return p
    }

    private fun signedArea(data: DoubleArray, start: Int, end: Int, dim: Int): Double {
        var sum = 0.0
        var j = end - dim
        var i = start
        while (i < end) {
            sum += (data[j] - data[i]) * (data[i + 1] + data[j + 1])
            j = i
            i += dim
        }
        return sum
    }

    private class Node internal constructor(internal var i: Int, internal var x: Double, internal var y: Double) {
        internal var z: Double = 0.toDouble()
        internal var steiner: Boolean = false

        internal var prev: Node? = null
        internal var next: Node? = null
        internal var prevZ: Node? = null
        internal var nextZ: Node? = null

        init {

            // previous and next vertice nodes in a polygon ring
            this.prev = null
            this.next = null

            // z-order curve value
            this.z = java.lang.Double.MIN_VALUE

            // previous and next nodes in z-order
            this.prevZ = null
            this.nextZ = null

            // indicates whether this is a steiner point
            this.steiner = false
        }// vertice index in coordinates array
        // vertex coordinates

        override fun toString(): String {
            val sb = StringBuilder()
            sb.append("{i: ").append(i).append(", x: ").append(x).append(", y: ").append(y).append(", prev: ").append(prev).append(", next: ").append(next)
            return sb.toString()
        }
    }
}
/**
 * Triangulates the given polygon
 *
 * @param data is a flat array of vertice coordinates like [x0,y0, x1,y1, x2,y2, ...].
 * @return List containing groups of three vertice indices in the resulting array forms a triangle.
 */