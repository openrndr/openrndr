package org.openrndr.kartifex.utils.regions

import org.openrndr.kartifex.*
import org.openrndr.kartifex.utils.Intersections
import kotlin.math.max
import kotlin.math.min

/**
 * The width below which a fold of the boundary is considered degenerate and is flattened by
 * [removeFolds]. This matches the thickness below which [nonZeroSelfUnion] considers a ring a sliver.
 */
internal const val FOLD_TOLERANCE = 10 * Intersections.SPATIAL_EPSILON

/** The distance from a vertex at which the directions of the curves meeting there are compared. */
private const val LEG_DISTANCE = 10 * FOLD_TOLERANCE

/** cos of the angle between the two legs leaving a vertex above which they're considered folded. */
private const val FOLDED_COS = 0.99

private const val MAX_FOLD_PASSES = 8

/**
 * Flattens the places where the boundary of [region] folds back onto itself within
 * [FOLD_TOLERANCE], before its self-intersections are resolved.
 *
 * Curves that turn around within less than the intersection precision (e.g. the offset of a curve
 * by more than its radius of curvature, which ought to have a cusp) end up in one of two shapes,
 * neither of which can be resolved meaningfully:
 * - a hook: a few curves only ~1e-5 long that double back between two longer curves, which
 *   resolve into a zero-width spike whose two sides can't be told apart;
 * - a U-turn whose two legs cross again shortly after the turn, which encloses a loop only ~1e-5
 *   wide that resolves into a sliver ring.
 *
 * Hooks are collapsed into a single point ([collapseMicroRuns]); thin U-turn loops are cut off at
 * the crossing of their legs ([cutFoldLoops]). Either moves the boundary by at most
 * [FOLD_TOLERANCE], and only there.
 */
internal fun removeFolds(region: Region2): Region2 {
    var changed = false
    val rings = region.rings.map { ring ->
        val original = ring.curves.toList()
        var curves = original
        for (pass in 0 until MAX_FOLD_PASSES) {
            val collapsed = collapseMicroRuns(curves)
            val next = cutFoldLoops(collapsed) ?: collapsed
            if (next === curves) {
                break
            }
            curves = next
        }
        if (curves === original) {
            ring
        } else {
            changed = true
            Ring2(curves)
        }
    }
    return if (changed) Region2(rings) else region
}

private fun chord(c: Curve2): Double = c.end().sub(c.start()).length()

/**
 * Collapses every maximal run of consecutive curves that fits within [FOLD_TOLERANCE] (between
 * curves that don't) into the point at its start, moving the start of the curve after it there.
 *
 * @return the new curves, or the input itself when nothing was collapsed
 */
private fun collapseMicroRuns(curves: List<Curve2>): List<Curve2> {
    val n = curves.size
    val tiny = BooleanArray(n) { chord(curves[it]) < FOLD_TOLERANCE }
    if (tiny.all { it } || tiny.none { it }) {
        return curves
    }

    // start at a curve that isn't tiny, so runs never wrap around the end of the list
    val first = tiny.indexOfFirst { !it }
    val result = mutableListOf<Curve2>()
    var collapsed = false
    var i = 0
    while (i < n) {
        val idx = (first + i) % n
        if (!tiny[idx]) {
            result.add(curves[idx])
            i++
            continue
        }
        var end = i
        val runStart = curves[idx].start()
        var extent = 0.0
        while (end < n && tiny[(first + end) % n]) {
            extent = max(extent, curves[(first + end) % n].end().sub(runStart).length())
            end++
        }
        if (extent < FOLD_TOLERANCE) {
            // drop the run; the curve after it (never tiny, by maximality) now starts at runStart
            val nextIdx = (first + end) % n
            collapsed = true
            if (end < n) {
                curves[nextIdx].let { result.add(it.endpoints(runStart, it.end())) }
                i = end + 1
            } else {
                // the run ends the list; the curve after it is result[0], the first curve
                result[0] = result[0].endpoints(runStart, result[0].end())
                i = end
            }
        } else {
            for (k in i until end) {
                result.add(curves[(first + k) % n])
            }
            i = end
        }
    }
    return if (collapsed && result.size >= 2) result else curves
}

/**
 * The point on [c] at (chord) distance [d] from [tip], found by bisection from the end of [c]
 * that sits at the tip.
 */
private fun legPoint(c: Curve2, tip: Vec2, fromEnd: Boolean, d: Double): Vec2 {
    var lo = 0.0
    var hi = 1.0
    repeat(60) {
        val m = (lo + hi) / 2
        if (c.position(if (fromEnd) 1 - m else m).sub(tip).length() < d) lo = m else hi = m
    }
    return c.position(if (fromEnd) 1 - lo else lo)
}

/**
 * Refines a crossing of [a] and [b] found by [Intersections.intersections], whose positions on
 * either curve can be a few SPATIAL_EPSILON apart, with Newton's method on a(s) - b(t) = 0. Cutting
 * at an unrefined crossing and snapping one curve onto the other would bend that whole curve.
 *
 * @return the refined (s, t), or [hit] itself when Newton's method doesn't converge
 */
private fun refineCrossing(a: Curve2, b: Curve2, hit: Vec2): Vec2 {
    var s = hit.x
    var t = hit.y
    repeat(16) {
        val f = a.position(s).sub(b.position(t))
        if (f.length() < 1e-12) {
            return Vec2(s, t)
        }
        val da = a.direction(s)
        val db = b.direction(t)
        // solve [da, -db] * (ds, dt) = -f
        val det = -da.x * db.y + db.x * da.y
        if (det == 0.0) {
            return hit
        }
        val ds = (-f.x * -db.y - -db.x * -f.y) / det
        val dt = (da.x * -f.y - -f.x * da.y) / det
        s += ds
        t += dt
        if (s !in 0.0..1.0 || t !in 0.0..1.0) {
            return hit
        }
    }
    return if (a.position(s).sub(b.position(t)).length() < a.position(hit.x).sub(b.position(hit.y)).length()) {
        Vec2(s, t)
    } else {
        hit
    }
}

/**
 * Where two consecutive curves leave their shared vertex in (nearly) the same direction and cross
 * each other again, cuts both at that crossing, provided the loop they enclose is thinner than
 * [FOLD_TOLERANCE].
 *
 * @return the new curves, or null when nothing was cut
 */
private fun cutFoldLoops(curves: List<Curve2>): List<Curve2>? {
    val n = curves.size
    if (n < 3) {
        return null
    }
    val result = curves.toMutableList()
    var cut = false
    for (i in 0 until n) {
        val a = result[i]
        val b = result[(i + 1) % n]
        val tip = a.end()
        val d = min(LEG_DISTANCE, min(chord(a), chord(b)) / 2)
        if (d <= 0.0) {
            continue
        }
        val va = legPoint(a, tip, true, d).sub(tip).norm()
        val vb = legPoint(b, tip, false, d).sub(tip).norm()
        if (Vec.dot(va, vb) < FOLDED_COS) {
            continue
        }

        // the crossing furthest along the legs that isn't the shared vertex itself
        val hit = a.intersections(b)
            .filter { it.x < 1.0 - Intersections.PARAMETRIC_EPSILON && it.y > Intersections.PARAMETRIC_EPSILON }
            .filter { !Vec.equals(a.position(it.x), tip, Intersections.SPATIAL_EPSILON) }
            .minByOrNull { it.x }
            ?.let { refineCrossing(a, b, it) } ?: continue
        if (hit.x <= Intersections.PARAMETRIC_EPSILON || hit.y >= 1.0 - Intersections.PARAMETRIC_EPSILON) {
            continue
        }

        // the crossing found on either leg can be a few SPATIAL_EPSILON apart; close the loop
        // exactly, or its signed area (taken about the origin) picks up an error proportional
        // to that gap times the distance to the origin
        val loopA = a.range(hit.x, 1.0)
        val loopB = b.range(0.0, hit.y).let { it.endpoints(it.start(), loopA.start()) }
        val loop = Ring2(listOf(loopA, loopB))
        val perimeter = loop.curves.sumOf { chord(it) }
        if (perimeter == 0.0 || 2.0 * loop.area / perimeter >= FOLD_TOLERANCE) {
            continue
        }

        val keptA = a.range(0.0, hit.x)
        result[i] = keptA
        result[(i + 1) % n] = b.range(hit.y, 1.0).let { it.endpoints(keptA.end(), it.end()) }
        cut = true
    }
    return if (cut) result else null
}
