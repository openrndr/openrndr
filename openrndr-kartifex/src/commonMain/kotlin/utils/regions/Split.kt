package org.openrndr.kartifex.utils.regions

import org.openrndr.kartifex.*
import org.openrndr.kartifex.utils.Intersections
import org.openrndr.kartifex.utils.Intersections.SPATIAL_EPSILON
import org.openrndr.kartifex.utils.SweepQueue
import org.openrndr.kartifex.utils.equals
import utils.DoubleAccumulator

/**
 * Splits two regions into parts based on their intersections and unions.
 *
 * @param a The first region to be split.
 * @param b The second region to be split.
 * @return A SplitResult containing the resulting regions a and b after the split,
 * along with the set of intersection points (splits).
 */
fun split(a: Region2, b: Region2): SplitResult {
    val queues = arrayOf<SweepQueue<Curve2>>(SweepQueue(), SweepQueue())
    addToQueue(a, queues[0])
    addToQueue(b, queues[1])
    val union = VertexUnion()
    val intersections = mutableMapOf<Curve2, DoubleAccumulator>()
    val cs = arrayOfNulls<Curve2>(2)
    while (true) {
        val idx = SweepQueue.next(*queues)
        cs[idx] = queues[idx].take()
        if (cs[idx] == null) {
            break
        }
        intersections.getOrPut(cs[idx] ?: error("null")) { DoubleAccumulator() }
        for (c in queues[1 - idx].active()) {
            cs[1 - idx] = c
            // When splitting a region against itself (removeSelfIntersections), the exact same
            // curve instance is queued on both sides, so it will eventually appear as "active" on
            // the opposite queue too; comparing a curve against itself is degenerate and must be
            // skipped rather than treated as a self-intersection.
            if (cs[0] === cs[1]) {
                continue
            }
            val ts = cs[0]!!.intersections(cs[1]!!)
            for (i in ts.indices) {
                val t0 = ts[i].x
                val t1 = ts[i].y
                intersections[cs[0]]?.add(t0)
                intersections[cs[1]]?.add(t1)
                val p0 = cs[0]!!.position(t0)
                val p1 = cs[1]!!.position(t1)
                union.join(p0, p1)
            }
        }
    }
    // A single ring can be self-touching without being self-crossing (e.g. marching-squares
    // output threading through a saddle point): it visits the same vertex more than once. The
    // sweep above only finds intersections *between* a and b, so such a vertex is never added
    // to `union`, and partition() (which only cuts a ring where it meets a vertex from `splits`)
    // never separates the loops that share it. They end up merged into a single arc, which is
    // then classified (inside/outside) as one unit instead of as independent loops, silently
    // dropping or retaining more geometry than it should.
    markSelfTouchingVertices(a, union)
    markSelfTouchingVertices(b, union)

    val deduped = intersections.mapValues { (c, acc) -> dedupe(c, acc, union) }
    return SplitResult(split(a, deduped, union), split(b, deduped, union), union.roots())
}

/**
 * Finds the self-intersections (and self-touching vertices) of [r] and splits it at those points.
 *
 * This is the single-region analogue of [split]: that function pushes each operand's curves into
 * its own queue and compares the two queues against each other, which -- when both operands are
 * the same region (as [removeSelfIntersections][Region2.removeSelfIntersections] needs) -- means
 * every curve is queued twice and every pair ends up compared twice over. Here a single queue is
 * swept instead, and each newly-opened curve is compared only against the others already active
 * (skipping itself), so every pair of curves is compared exactly once.
 *
 * Since the result only ever stands in for both operands of a self-union, the resolved region is
 * built once and reused for both [SplitResult.a] and [SplitResult.b].
 */
fun selfSplit(r: Region2): SplitResult {
    val queue = SweepQueue<Curve2>()
    addToQueue(r, queue)

    val union = VertexUnion()
    val intersections = mutableMapOf<Curve2, DoubleAccumulator>()

    while (true) {
        val c0 = queue.take() ?: break
        intersections[c0] = DoubleAccumulator()
        for (c1 in queue.active()) {
            if (c0 === c1) {
                continue
            }
            val ts = c0.intersections(c1)
            for (i in ts.indices) {
                val t0 = ts[i].x
                val t1 = ts[i].y
                intersections[c0]?.add(t0)
                intersections[c1]?.add(t1)
                val p0 = c0.position(t0)
                val p1 = c1.position(t1)
                union.join(p0, p1)
            }
        }
    }

    markSelfTouchingVertices(r, union)

    val deduped = intersections.mapValues { (c, acc) -> dedupe(c, acc, union) }
    val cut = split(r, deduped, union)
    return SplitResult(cut, cut, union.roots())
}

private fun markSelfTouchingVertices(region: Region2, union: VertexUnion) {
    for (r in region.rings) {
        val counts = mutableMapOf<Vec2, Int>()
        for (c in r.curves) {
            counts[c.start()] = (counts[c.start()] ?: 0) + 1
        }
        for ((p, count) in counts) {
            if (count > 1) {
                union.join(p, p)
            }
        }
    }
}

private fun split(
    region: Region2,
    splits: Map<Curve2, DoubleAccumulator>,
    union: VertexUnion
): Region2 = Region2(region.rings.mapNotNull { ring -> split(ring, splits, union) }.toTypedArray())

private fun dedupe(
    c: Curve2,
    acc: DoubleAccumulator,
    union: VertexUnion
): DoubleAccumulator {
    val ts = acc.toArray()
    ts.sort()
    val result = DoubleAccumulator()
    for (i in ts.indices) {
        val t0 = if (result.size() == 0) 0.0 else result.last()
        val t1 = ts[i]
        if (equals(t0, t1, Intersections.PARAMETRIC_EPSILON)
            || Vec.equals(c.position(t0), c.position(t1), SPATIAL_EPSILON)
        ) {
            union.join(c.position(t0), c.position(t1))
        } else if (equals(t1, 1.0, Intersections.PARAMETRIC_EPSILON)
            || Vec.equals(c.position(t1), c.end(), SPATIAL_EPSILON)
        ) {
            union.join(c.position(t1), c.end())
        } else {
            result.add(t1)
        }
    }
    return result
}

private fun split(
    r: Ring2,
    splits: Map<Curve2, DoubleAccumulator>,
    union: VertexUnion
): Ring2? {
    val curves = mutableListOf<Curve2>()
    for (c in r.curves) {
        val acc = splits[c]!!
        for (cp in c.split(acc.toArray())) {
            val cpa = union.adjust(cp)
            if (cpa != null) {
                curves.add(cpa)
            }
        }
    }
    return if (curves.size == 0) null else Ring2(curves)
}

private fun addToQueue(region: Region2, queue: SweepQueue<Curve2>) {
    for (r in region.rings) {
        for (c in r.curves) {
            // TODO EJ: determine if taking the extends of the bounding box of the curve is the better solution
            queue.add(c, c.start().x, c.end().x)
//                val bounds = c.bounds()
//                if (!c.isFlat(SPATIAL_EPSILON)) {
//                    val cs = c.split(c.inflections())
//                    for (s in cs) {
//                        queue.add(s, s.bounds().lower().x, s.bounds().upper().x)
//                    }
//                } else {
//                    queue.add(c, bounds.lower().x, bounds.upper().x)
//                }

        }
    }
}

internal class VertexUnion {
    private val parent = mutableMapOf<Vec2, Vec2>()
    private val roots = mutableSetOf<Vec2>()
    fun join(a: Vec2, b: Vec2) {
        @Suppress("NAME_SHADOWING") var a: Vec2 = a
        @Suppress("NAME_SHADOWING") var b: Vec2 = b
        a = adjust(a)
        b = adjust(b)
        val cmp = a.compareTo(b)
        when {
            cmp < 0 -> {
                parent[b] = a
                roots.add(a)
            }

            cmp > 0 -> {
                parent[a] = b
                roots.add(b)
            }

            else -> {
                roots.add(b)
            }
        }
    }

    fun adjust(p: Vec2): Vec2 {
        var curr = p
        while (true) {
            val next = parent[curr]
            if (next == null) {
                if (curr != p) {
                    parent[p] = curr
                }
                return curr
            }
            curr = next
        }
    }

    fun adjust(c: Curve2): Curve2? {
        val start = adjust(c.start())
        val end = adjust(c.end())
        return if (start == end) null else c.endpoints(start, end)
    }

    fun roots(): Set<Vec2> {
        return (roots - parent.keys)
    }
}

class SplitResult(val a: Region2, val b: Region2, val splits: Set<Vec2>)