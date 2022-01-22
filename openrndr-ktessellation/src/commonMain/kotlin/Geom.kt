package org.openrndr.ktessellation

import kotlin.math.abs

internal object Geom {
    /* Given three vertices u,v,w such that VertLeq(u,v) && VertLeq(v,w),
     * evaluates the t-coord of the edge uw at the s-coord of the vertex v.
     * Returns v->t - (uw)(v->s), ie. the signed distance from uw to v.
     * If uw is vertical (and thus passes thru v), the result is zero.
     *
     * The calculation is extremely accurate and stable, even when v
     * is very close to u or w.  In particular if we set v->t = 0 and
     * let r be the negated result (this evaluates (uw)(v->s)), then
     * r is guaranteed to satisfy MIN(u->t,w->t) <= r <= MAX(u->t,w->t).
     */
    fun EdgeEval(
        u: GLUvertex,
        v: GLUvertex,
        w: GLUvertex
    ): Double {
        require(VertLeq(u, v) && VertLeq(v, w))
        val gapL = v.s - u.s
        val gapR = w.s - v.s
        return if (gapL + gapR > 0) {
            if (gapL < gapR) {
                v.t - u.t + (u.t - w.t) * (gapL / (gapL + gapR))
            } else {
                v.t - w.t + (w.t - u.t) * (gapR / (gapL + gapR))
            }
        } else 0.0
        /* vertical line */
    }

    fun EdgeSign(
        u: GLUvertex,
        v: GLUvertex,
        w: GLUvertex
    ): Double {
        require(VertLeq(u, v) && VertLeq(v, w))
        val gapL = v.s - u.s
        val gapR = w.s - v.s
        return if (gapL + gapR > 0) {
            (v.t - w.t) * gapL + (v.t - u.t) * gapR
        } else 0.0
        /* vertical line */
    }

    /***********************************************************************
     * Define versions of EdgeSign, EdgeEval with s and t transposed.
     */
    fun TransEval(
        u: GLUvertex,
        v: GLUvertex,
        w: GLUvertex
    ): Double {
        /* Given three vertices u,v,w such that TransLeq(u,v) && TransLeq(v,w),
         * evaluates the t-coord of the edge uw at the s-coord of the vertex v.
         * Returns v->s - (uw)(v->t), ie. the signed distance from uw to v.
         * If uw is vertical (and thus passes thru v), the result is zero.
         *
         * The calculation is extremely accurate and stable, even when v
         * is very close to u or w.  In particular if we set v->s = 0 and
         * let r be the negated result (this evaluates (uw)(v->t)), then
         * r is guaranteed to satisfy MIN(u->s,w->s) <= r <= MAX(u->s,w->s).
         */
        require(TransLeq(u, v) && TransLeq(v, w))
        val gapL = v.t - u.t
        val gapR = w.t - v.t
        return if (gapL + gapR > 0) {
            if (gapL < gapR) {
                v.s - u.s + (u.s - w.s) * (gapL / (gapL + gapR))
            } else {
                v.s - w.s + (w.s - u.s) * (gapR / (gapL + gapR))
            }
        } else 0.0
        /* vertical line */
    }

    fun TransSign(
        u: GLUvertex,
        v: GLUvertex,
        w: GLUvertex
    ): Double {
        /* Returns a number whose sign matches TransEval(u,v,w) but which
         * is cheaper to evaluate.  Returns > 0, == 0 , or < 0
         * as v is above, on, or below the edge uw.
         */
        val gapL: Double
        val gapR: Double
        require(TransLeq(u, v) && TransLeq(v, w))
        gapL = v.t - u.t
        gapR = w.t - v.t
        return if (gapL + gapR > 0) {
            (v.s - w.s) * gapL + (v.s - u.s) * gapR
        } else 0.0
        /* vertical line */
    }

    fun VertCCW(
        u: GLUvertex,
        v: GLUvertex,
        w: GLUvertex
    ): Boolean {
        /* For almost-degenerate situations, the results are not reliable.
         * Unless the floating-point arithmetic can be performed without
         * rounding errors, *any* implementation will give incorrect results
         * on some degenerate inputs, so the client must have some way to
         * handle this situation.
         */
        return u.s * (v.t - w.t) + v.s * (w.t - u.t) + w.s * (u.t - v.t) >= 0
    }

    /* Given parameters a,x,b,y returns the value (b*x+a*y)/(a+b),
 * or (x+y)/2 if a==b==0.  It requires that a,b >= 0, and enforces
 * this in the rare case that one argument is slightly negative.
 * The implementation is extremely stable numerically.
 * In particular it guarantees that the result r satisfies
 * MIN(x,y) <= r <= MAX(x,y), and the results are very accurate
 * even when a and b differ greatly in magnitude.
 */
    fun Interpolate(a: Double, x: Double, b: Double, y: Double): Double {
        var a = a
        var b = b
        a = if (a < 0) 0.0 else a
        b = if (b < 0) 0.0 else b
        return if (a <= b) {
            if (b == 0.0) {
                (x + y) / 2.0
            } else {
                x + (y - x) * (a / (a + b))
            }
        } else {
            y + (x - y) * (b / (a + b))
        }
    }

    fun EdgeIntersect(
        o1: GLUvertex, d1: GLUvertex,
        o2: GLUvertex, d2: GLUvertex,
        v: GLUvertex
    ) /* Given edges (o1,d1) and (o2,d2), compute their point of intersection.
 * The computed point is guaranteed to lie in the intersection of the
 * bounding rectangles defined by each edge.
 */ {
        var o1: GLUvertex = o1
        var d1: GLUvertex = d1
        var o2: GLUvertex = o2
        var d2: GLUvertex = d2
        var z1: Double
        var z2: Double

        /* This is certainly not the most efficient way to find the intersection
         * of two line segments, but it is very numerically stable.
         *
         * Strategy: find the two middle vertices in the VertLeq ordering,
         * and interpolate the intersection s-value from these.  Then repeat
         * using the TransLeq ordering to find the intersection t-value.
         */if (!VertLeq(o1, d1)) {
            val temp: GLUvertex = o1
            o1 = d1
            d1 = temp
        }
        if (!VertLeq(o2, d2)) {
            val temp: GLUvertex = o2
            o2 = d2
            d2 = temp
        }
        if (!VertLeq(o1, o2)) {
            var temp: GLUvertex = o1
            o1 = o2
            o2 = temp
            temp = d1
            d1 = d2
            d2 = temp
        }
        if (!VertLeq(o2, d1)) {
            /* Technically, no intersection -- do our best */
            v.s = (o2.s + d1.s) / 2.0
        } else if (VertLeq(d1, d2)) {
            /* Interpolate between o2 and d1 */
            z1 = EdgeEval(o1, o2, d1)
            z2 = EdgeEval(o2, d1, d2)
            if (z1 + z2 < 0) {
                z1 = -z1
                z2 = -z2
            }
            v.s = Interpolate(z1, o2.s, z2, d1.s)
        } else {
            /* Interpolate between o2 and d2 */
            z1 = EdgeSign(o1, o2, d1)
            z2 = -EdgeSign(o1, d2, d1)
            if (z1 + z2 < 0) {
                z1 = -z1
                z2 = -z2
            }
            v.s = Interpolate(z1, o2.s, z2, d2.s)
        }

        /* Now repeat the process for t */if (!TransLeq(o1, d1)) {
            val temp: GLUvertex = o1
            o1 = d1
            d1 = temp
        }
        if (!TransLeq(o2, d2)) {
            val temp: GLUvertex = o2
            o2 = d2
            d2 = temp
        }
        if (!TransLeq(o1, o2)) {
            var temp: GLUvertex = o2
            o2 = o1
            o1 = temp
            temp = d2
            d2 = d1
            d1 = temp
        }
        if (!TransLeq(o2, d1)) {
            /* Technically, no intersection -- do our best */
            v.t = (o2.t + d1.t) / 2.0
        } else if (TransLeq(d1, d2)) {
            /* Interpolate between o2 and d1 */
            z1 = TransEval(o1, o2, d1)
            z2 = TransEval(o2, d1, d2)
            if (z1 + z2 < 0) {
                z1 = -z1
                z2 = -z2
            }
            v.t = Interpolate(z1, o2.t, z2, d1.t)
        } else {
            /* Interpolate between o2 and d2 */
            z1 = TransSign(o1, o2, d1)
            z2 = -TransSign(o1, d2, d1)
            if (z1 + z2 < 0) {
                z1 = -z1
                z2 = -z2
            }
            v.t = Interpolate(z1, o2.t, z2, d2.t)
        }
    }

    fun VertEq(u: GLUvertex, v: GLUvertex): Boolean {
        return u.s == v.s && u.t == v.t
    }

    fun VertLeq(u: GLUvertex, v: GLUvertex): Boolean {
        return u.s < v.s || u.s == v.s && u.t <= v.t
    }

    /* Versions of VertLeq, EdgeSign, EdgeEval with s and t transposed. */
    fun TransLeq(u: GLUvertex, v: GLUvertex): Boolean {
        return u.t < v.t || u.t == v.t && u.s <= v.s
    }

    fun EdgeGoesLeft(e: GLUhalfEdge): Boolean {
        return VertLeq(e.Sym?.Org ?: error("sym.org == null"), e.Org ?: error("e.org == null"))
    }

    fun EdgeGoesRight(e: GLUhalfEdge): Boolean {
        return VertLeq(e.Org ?: error("e.org == null"), e.Sym?.Org ?: error("e.sym.org == null"))
    }

    fun VertL1dist(u: GLUvertex, v: GLUvertex): Double {
        return abs(u.s - v.s) + abs(u.t - v.t)
    }
}
