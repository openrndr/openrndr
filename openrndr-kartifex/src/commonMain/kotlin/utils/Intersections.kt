package org.openrndr.kartifex.utils

import org.openrndr.kartifex.*
import kotlin.jvm.JvmName
import kotlin.math.*

@JvmName("kartifexHackSort1")
fun <T> Array<T>.sort(start: Int = 0, end: Int = size, selector: (T) -> Double) {
    if (start != 0 || end != size) {
        val copy = copyOfRange(start, end)
        copy.sortBy(selector)
        copy.copyInto(this, start)
    } else {
        sortBy(selector)
    }
}

@JvmName("kartifexHackSort2")
fun <T> sort(a: Array<T>, start: Int = 0, end: Int = a.size, selector: (T) -> Double) {
    a.sort(start, end, selector)
}


object Intersections {
    // utilities
    const val FAT_LINE_PARAMETRIC_RESOLUTION = 1e-5
    const val FAT_LINE_SPATIAL_EPSILON = 1e-5

    const val PARAMETRIC_EPSILON = 1e-5
    const val SPATIAL_EPSILON = 1e-5

    const val MAX_CUBIC_CUBIC_INTERSECTIONS = 9

    val PARAMETRIC_BOUNDS: Box2 =
        Box.box(Vec2(0.0, 0.0), Vec2(1.0, 1.0))

    fun lineCurve(a: Line2, b: Curve2): Array<Vec2> {
        return if (b is Line2) {
            lineLine(a, b)
        } else if (b.isFlat(SPATIAL_EPSILON)) {
            lineLine(a, Line2.line(b.start(), b.end()))
        } else if (b is Bezier2.QuadraticBezier2) {
            lineQuadratic(a, b)
        } else {
            lineCubic(a, b as Bezier2.CubicBezier2)
        }
    }

    fun lineLine(a: Line2, b: Line2): Array<Vec2> {
        val av: Vec2 = a.end().sub(a.start())
        val bv: Vec2 = b.end().sub(b.start())
        val d: Double = Vec2.cross(av, bv)
        if (abs(d) < 1e-6) {
            val ints: Array<Vec2> = collinearIntersection(a, b)
            if (ints.all { v: Vec2 ->
                    Vec.equals(
                        a.position(v.x),
                        b.position(v.y),
                        SPATIAL_EPSILON
                    )
                }
            ) {
                return ints
            } else if (abs(d) == 0.0) {
                return emptyArray()
            }
        }
        val asb = a.start().sub(b.start())
        val s = Vec2.cross(bv, asb) / d
        val t = Vec2.cross(av, asb) / d
        return arrayOf(Vec2(s, t))
    }

    fun lineQuadratic(
        p: Line2,
        q: Bezier2.QuadraticBezier2
    ): Array<Vec2> {
        // (p0 - 2p1 + p2) t^2 + (-2p0 + 2p1) t + p0
        val a: Vec2 = q.p0.add(q.p1.mul(-2.0)).add(q.p2)
        val b: Vec2 = q.p0.mul(-2.0).add(q.p1.mul(2.0))
        val c: Vec2 = q.p0
        val dir: Vec2 = p.end().sub(p.start())
        val n = Vec2(-dir.y, dir.x)
        val roots: DoubleArray = Equations.solveQuadratic(
            Vec.dot(n, a),
            Vec.dot(n, b),
            Vec.dot(n, c) + Vec2.cross(p.start(), p.end())
        )
        val result: Array<Vec2> = if (Scalars.equals(dir.x, 0.0, Scalars.EPSILON)) {
            val y0: Double = p.start().y
            Array(roots.size) { i ->
                val t = roots[i]
                val y1: Double = q.position(t).y
                Vec2((y1 - y0) / dir.y, t)
            }
        } else {
            val x0: Double = p.start().x
            Array(roots.size) { i ->
                val t = roots[i]
                val x1: Double = q.position(t).x
                Vec2((x1 - x0) / dir.x, t)
            }
        }
        return result
    }

    fun lineCubic(
        p: Line2,
        q: Bezier2.CubicBezier2
    ): Array<Vec2> {
        // (-p0 + 3p1 - 3p2 + p3) t^3 + (3p0 - 6p1 + 3p2) t^2 + (-3p0 + 3p1) t + p0
        val a: Vec2 = q.p0.mul(-1.0).add(q.p1.mul(3.0)).add(q.p2.mul(-3.0)).add(q.p3)
        val b: Vec2 = q.p0.mul(3.0).add(q.p1.mul(-6.0)).add(q.p2.mul(3.0))
        val c: Vec2 = q.p0.mul(-3.0).add(q.p1.mul(3.0))
        val d: Vec2 = q.p0
        val dir: Vec2 = p.end().sub(p.start())
        val dLen: Double = dir.length()
        val n = Vec2(-dir.y, dir.x)
        val roots: DoubleArray = Equations.solveCubic(
            Vec.dot(n, a),
            Vec.dot(n, b),
            Vec.dot(n, c),
            Vec.dot(n, d) + Vec2.cross(p.start(), p.end())
        )
        val result = Array(roots.size) { i ->
            val t = roots[i]
            val v = q.position(t).sub(p.start())
            val vLen: Double = v.length()
            val s: Double = vLen / dLen * signum(Vec.dot(dir, v))
            Vec2(s, t)
        }
        return result
    }

    fun normalize(intersections: Array<Vec2>): Array<Vec2> {
        var limit = intersections.size
        if (limit == 0) {
            return intersections
        }
        var readIdx: Int
        var writeIdx: Int

        // round and filter within [0, 1]
        readIdx = 0
        writeIdx = 0
        while (readIdx < limit) {
            val i: Vec2 = intersections[readIdx].map { n: Double ->
                round(
                    n,
                    PARAMETRIC_EPSILON
                )
            }
            if (PARAMETRIC_BOUNDS.contains(i)) {
                intersections[writeIdx++] = i
            }
            readIdx++
        }
        limit = writeIdx
        if (limit > 1) {
            // dedupe intersections on b
            //intersections.sortBy { it.y }
            sort(
                intersections,
                0,
                limit,
                { v: Vec2 -> v.y }
            )
            readIdx = 0
            writeIdx = -1
            while (readIdx < limit) {
                val i: Vec2 = intersections[readIdx]
                if (writeIdx < 0 || !Scalars.equals(intersections[writeIdx].y, i.y, Scalars.EPSILON)) {
                    intersections[++writeIdx] = i
                }
                readIdx++
            }
            limit = writeIdx + 1
        }
        if (limit > 1) {
            // dedupe intersections on a
            sort(
                intersections,
                0,
                limit,
                { v: Vec2 -> v.x })

            readIdx = 0
            writeIdx = -1
            while (readIdx < limit) {
                val i: Vec2 = intersections[readIdx]
                if (writeIdx < 0 || !Scalars.equals(intersections[writeIdx].x, i.x, Scalars.EPSILON)) {
                    intersections[++writeIdx] = i
                }
                readIdx++
            }
            limit = writeIdx + 1
        }
        val result: Array<Vec2> = Array(limit) {
            intersections[it]
        }
        return result
    }

    // analytical methods
    fun collinearIntersection(
        a: Curve2,
        b: Curve2
    ): Array<Vec2> {
        val result = mutableListOf<Vec2>()
        for (i in 0..1) {
            val tb: Double = b.nearestPoint(a.position(i.toDouble()))

            // a overhangs the start of b
            if (tb <= 0) {
                val s: Double = round(
                    a.nearestPoint(b.start()),
                    PARAMETRIC_EPSILON
                )
                if (0.0 <= s && s <= 1.0) {
                    result.add(Vec2(s, 0.0))
                }

                // a overhangs the end of b
            } else if (tb >= 1) {
                val s: Double = round(
                    a.nearestPoint(b.end()),
                    PARAMETRIC_EPSILON
                )
                if (0 <= s && s <= 1) {
                    result.add(Vec2(s, 1.0))
                }

                // a is contained in b
            } else {
                result.add(Vec2(i.toDouble(), tb))
            }
        }
        if (result.size == 2 && Vec.equals(
                result[0],
                result[1],
                PARAMETRIC_EPSILON
            )
        ) {
            result.removeLast()
        }
        return result.toTypedArray()
    }

    // subdivision (slow, but as close to a reference implementation as exists)
    class CurveInterval(
        curve: Curve2,
        tLo: Double,
        tHi: Double,
        pLo: Vec2,
        pHi: Vec2
    ) {
        val curve: Curve2
        val isFlat: Boolean
        val tLo: Double
        val tHi: Double
        val pLo: Vec2
        val pHi: Vec2
        fun bounds(): Box2 {
            return Box.box(pLo, pHi)
        }

        fun intersects(c: CurveInterval): Boolean {
            return bounds().expand(SPATIAL_EPSILON).intersects(c.bounds())
        }

        fun split(): Array<CurveInterval> {
            return if (isFlat) {
                arrayOf(this)
            } else {
                val tMid = (tLo + tHi) / 2
                val pMid: Vec2 = curve.position(tMid)
                arrayOf(
                    CurveInterval(curve, tLo, tMid, pLo, pMid),
                    CurveInterval(curve, tMid, tHi, pMid, pHi)
                )
            }
        }

        fun intersections(c: CurveInterval, acc: MutableList<Vec2>) {
            for (i in lineLine(Line2.line(pLo, pHi), Line2.line(c.pLo, c.pHi))) {
                if (PARAMETRIC_BOUNDS.expand(PARAMETRIC_EPSILON).contains(i)) {
                    acc.add(Vec.lerp(Vec2(tLo, c.tLo), Vec2(tHi, c.tHi), i))
                }
            }
        }

        override fun toString(): String {
            return "[$tLo, $tHi]"
        }

        companion object {
            fun from(c: Curve2): Array<CurveInterval?> {
                val ts: DoubleArray = c.inflections()
                ts.sort()
                return if (ts.size == 0) {
                    arrayOf(CurveInterval(c, 0.0, 1.0, c.start(), c.end()))
                } else {
                    val ls = arrayOfNulls<CurveInterval>(ts.size + 1)
                    for (i in ls.indices) {
                        val lo: Double = if (i == 0) 0.0 else ts[i - 1]
                        val hi: Double = if (i == ls.size - 1) 1.0 else ts[i]
                        ls[i] = CurveInterval(c, lo, hi, c.position(lo), c.position(hi))
                    }
                    ls
                }
            }
        }

        init {
            this.curve = curve
            this.tLo = tLo
            this.tHi = tHi
            this.pLo = pLo
            this.pHi = pHi
            isFlat = (Vec.equals(pLo, pHi, SPATIAL_EPSILON)
                    || tHi - tLo < PARAMETRIC_EPSILON || curve.range(tLo, tHi)
                .isFlat(SPATIAL_EPSILON))
        }
    }


    // post-processing
    fun round(n: Double, epsilon: Double): Double {
        return if (Scalars.equals(n, 0.0, epsilon)) {
            0.0
        } else if (Scalars.equals(n, 1.0, epsilon)) {
            1.0
        } else {
            n
        }
    }

    //
    fun intersections(a: Curve2, b: Curve2): Array<Vec2> {
        if (!a.bounds().expand(SPATIAL_EPSILON).intersects(b.bounds())) {
            return emptyArray()
        }
        return if (a is Line2) {
            normalize(lineCurve(a, b))
        } else if (b is Line2) {
            val result: Array<Vec2> = normalize(
                lineCurve(b, a)
            )
            for (i in result.indices) {
                result[i] = result[i].swap()
            }
            result
        } else {
            //return subdivisionCurveCurve(a, b);
            fatLineCurveCurve(a, b)
        }
    }

    // fatline

    // fat lines (faster, but more temperamental)
    // This is adapted from Sederberg's "Curve Intersection Using Bezier Clipping", but the algorithm as described
    // gets unstable when one curve is clipped small enough, causing it to over-clip the other curve, causing us to miss
    // intersection points.  To address this, we quantize the curve sub-ranges using FAT_LINE_PARAMETRIC_RESOLUTION,
    // preventing them from getting too small, and expand the width of our clipping regions by FAT_LINE_SPATIAL_EPSILON.
    fun signedDistance(p: Vec2, a: Vec2, b: Vec2): Double {
        val d: Vec2 = b.sub(a)
        return (Vec2.cross(p, d) + Vec2.cross(b, a)) / d.length()
    }

    fun fatLineWidth(c: Curve2): Interval {
        return if (c is Line2) {
            Interval.interval(0.0, 0.0)
        } else if (c is Bezier2.QuadraticBezier2) {
            val b: Bezier2.QuadraticBezier2 = c
            Interval.interval(0.0, signedDistance(b.p1, b.p0, b.p2) / 2)
        } else if (c is Bezier2.CubicBezier2) {
            val b: Bezier2.CubicBezier2 = c
            val d1 = signedDistance(b.p1, b.p0, b.p3)
            val d2 = signedDistance(b.p2, b.p0, b.p3)
            val k = if (d1 * d2 < 0) 4 / 9.0 else 3 / 4.0
            Interval.interval(
                min(
                    0.0,
                    min(d1, d2)
                ) * k,
                max(0.0, max(d1, d2)) * k
            )
        } else {
            throw IllegalStateException()
        }
    }

    fun convexHull(
        a: Vec2,
        b: Vec2,
        c: Bezier2.QuadraticBezier2
    ): Array<Vec2> {
        val p0 = Vec2(0.0, signedDistance(c.p0, a, b))
        val p1 = Vec2(1 / 2.0, signedDistance(c.p1, a, b))
        val p2 = Vec2(1.0, signedDistance(c.p2, a, b))
        return arrayOf(p0, p1, p2, p0)
    }

    fun convexHull(
        a: Vec2,
        b: Vec2,
        c: Bezier2.CubicBezier2
    ): Array<Vec2> {
        val p0 = Vec2(0.0, signedDistance(c.p0, a, b))
        val p1 = Vec2(1 / 3.0, signedDistance(c.p1, a, b))
        val p2 = Vec2(2 / 3.0, signedDistance(c.p2, a, b))
        val p3 = Vec2(1.0, signedDistance(c.p3, a, b))
        val d1 = signedDistance(p1, p0, p3)
        val d2 = signedDistance(p2, p0, p3)
        return if (d1 * d2 < 0) {
            arrayOf(p0, p1, p3, p2, p0)
        } else {
            val k = d1 / d2
            if (k >= 2) {
                arrayOf(p0, p1, p3, p0)
            } else if (k <= 0.5) {
                arrayOf(p0, p2, p3, p0)
            } else {
                arrayOf(p0, p1, p2, p3, p0)
            }
        }
    }

    fun convexHull(
        a: Vec2,
        b: Vec2,
        c: Curve2
    ): Array<Vec2> {
        return when (c) {
            is Bezier2.QuadraticBezier2 -> {
                convexHull(a, b, c)
            }
            is Bezier2.CubicBezier2 -> {
                convexHull(a, b, c)
            }
            else -> {
                throw IllegalStateException()
            }
        }
    }

    fun clipHull(fatLine: Interval, hull: Array<Vec2>): Interval {
        var lo = Double.POSITIVE_INFINITY
        var hi = Double.NEGATIVE_INFINITY
        for (i in 0 until hull.size - 1) {
            if (fatLine.contains(hull[i].y)) {
                lo = min(lo, hull[i].x)
                hi = max(hi, hull[i].x)
            }
        }
        for (y in doubleArrayOf(fatLine.lo, fatLine.hi)) {
            for (i in 0 until hull.size - 1) {
                val a: Vec2 = hull[i]
                val b: Vec2 = hull[i + 1]
                if (Interval.interval(a.y, b.y).contains(y)) {
                    if (a.y == b.y) {
                        lo = min(lo, min(a.x, b.x))
                        hi = max(
                            lo, max(a.x, b.x)
                        )
                    } else {
                        val t = Scalars.lerp(a.x, b.x, (y - a.y) / (b.y - a.y))
                        lo = min(lo, t)
                        hi = max(hi, t)
                    }
                }
            }
        }
        return if (hi < lo) {
            Interval.EMPTY
        } else {
            Interval.interval(lo, hi)
        }
    }

    fun quantize(t: Interval): Interval {
        val resolution: Double = FAT_LINE_PARAMETRIC_RESOLUTION
        val lo: Double = min(
            1 - resolution,
            floor(t.lo / resolution) * resolution
        )
        val hi: Double = max(
            lo + resolution,
            ceil(t.hi / resolution) * resolution
        )
        return Interval.interval(lo, hi)
    }

    fun addIntersections(
        a: FatLine,
        b: FatLine,
        acc: MutableList<Vec2>
    ) {
        val la: Line2 = a.line()
        val lb: Line2 = b.line()
        val av: Vec2 = la.end().sub(la.start())
        val bv: Vec2 = lb.end().sub(lb.start())
        val asb: Vec2 = la.start().sub(lb.start())
        val d: Double = Vec2.cross(av, bv)
        val i = Vec2(
            Vec2.cross(bv, asb) / d,
            Vec2.cross(av, asb) / d
        )
        if (PARAMETRIC_BOUNDS.expand(0.1).contains(i)) {
            acc.add(Box.box(a.t, b.t).lerp(i))
        }
    }

    fun clipFatline(
        subject: FatLine,
        clipper: FatLine
    ): FatLine? {
        val hull = convexHull(clipper.range.start(), clipper.range.end(), subject.range)

        val expanded = clipper._line.expand(FAT_LINE_SPATIAL_EPSILON)
        val normalized = clipHull(expanded, hull)
        return if (normalized.isEmpty) null else FatLine(
            subject.curve,
            subject.t.lerp(normalized)
        )
    }

    class FatLine internal constructor(curve: Curve2, t: Interval) {
        val curve: Curve2
        val range: Curve2
        val t: Interval
        val _line: Interval
        fun mid(): Double {
            return t.lerp(0.5)
        }

        val isFlat: Boolean
            get() = t.size() < PARAMETRIC_EPSILON || _line.size() <= SPATIAL_EPSILON

        fun bounds(): Box2 {
            return Box.box(range.start(), range.end())
        }

        fun intersects(l: FatLine): Boolean {
            return bounds().expand(SPATIAL_EPSILON * 10).intersects(l.bounds())
        }

        fun split(): Array<FatLine> {
            return if (isFlat) {
                arrayOf(this)
            } else arrayOf(
                FatLine(curve, Interval.interval(t.lo, mid())),
                FatLine(curve, Interval.interval(mid(), t.hi))
            )
        }

        fun line(): Line2 {
            return Line2.line(range.start(), range.end())
        }

        override fun toString(): String {
            return "FatLine(curve=$curve, range=$range, t=$t, _line=$_line, isFlat=$isFlat)"
        }


        companion object {
            fun from(c: Curve2): Array<FatLine> {
                val ts: DoubleArray = c.inflections()
                ts.sort()
                return if (ts.isEmpty()) {
                    arrayOf(FatLine(c, Interval.interval(0.0, 1.0)))
                } else {
                    val result = arrayOfNulls<FatLine>(ts.size + 1)
                    for (i in result.indices) {
                        val lo: Double = if (i == 0) 0.0 else ts[i - 1]
                        val hi: Double = if (i == result.size - 1) 1.0 else ts[i]
                        result[i] = FatLine(c, Interval.interval(lo, hi))
                    }
                    @Suppress("UNCHECKED_CAST")
                    result as Array<FatLine>
                }
            }
        }

        init {
            this.curve = curve
            this.t = quantize(t)
            range = curve.range(this.t)
            _line = fatLineWidth(range)
        }
    }

//    fun fatLineWidth(c: Curve2): Interval? {
//        return if (c is Line2) {
//            Interval.interval(0.0, 0.0)
//        } else if (c is Bezier2.QuadraticBezier2) {
//            val b: Bezier2.QuadraticBezier2 = c as Bezier2.QuadraticBezier2
//            Interval.interval(
//                0.0,
//                Intersections.signedDistance(b.p1, b.p0, b.p2) / 2
//            )
//        } else if (c is Bezier2.CubicBezier2) {
//            val b: Bezier2.CubicBezier2 = c as Bezier2.CubicBezier2
//            val d1: Double = Intersections.signedDistance(b.p1, b.p0, b.p3)
//            val d2: Double = Intersections.signedDistance(b.p2, b.p0, b.p3)
//            val k = if (d1 * d2 < 0) 4 / 9.0 else 3 / 4.0
//            Interval.interval(
//                min(
//                    0.0,
//                    min(d1, d2)
//                ) * k,
//                max(0.0, max(d1, d2)) * k
//            )
//        } else {
//            throw IllegalStateException()
//        }
//    }

//    fun quantize(t: Interval): Interval? {
//        val resolution: Double = Intersections.FAT_LINE_PARAMETRIC_RESOLUTION
//        val lo: Double = min(
//            1 - resolution,
//            floor(t.lo / resolution) * resolution
//        )
//        val hi: Double = max(
//            lo + resolution,
//            ceil(t.hi / resolution) * resolution
//        )
//        return Interval.interval(lo, hi)
//    }

    fun fatLineCurveCurve(a: Curve2, b: Curve2): Array<Vec2> {
        val queue = ArrayDeque<FatLine>()
        val `as` = FatLine.from(a)
        val bs = FatLine.from(b)
        for (ap in `as`) {
            for (bp in bs) {
                queue.apply {
                    addLast(ap)
                    addLast(bp)
                }
            }
        }
        var iterations = 0
        var collinearCheck = false
        val acc = ArrayDeque<Vec2>()
        while (queue.size > 0) {
            // if it's taking a while, check once (and only once) if they're collinear
            if (iterations > 32 && !collinearCheck) {
                collinearCheck = true
                val `is`: Array<Vec2> =
                    collinearIntersection(a, b)
                if (isCollinear(a, b, `is`)) {
                    return `is`
                }
            }
            var lb = queue.removeLast()
            var la = queue.removeLast()
            while (true) {
                iterations++
                if (!la.intersects(lb)) {
                    break
                }
                if (la.isFlat && lb.isFlat) {
                    addIntersections(la, lb, acc)
                    break
                }
                val aSize: Double = la.t.size()
                val bSize: Double = lb.t.size()

                // use a to clip b
                val lbPrime = clipFatline(lb, la) ?: break
                lb = lbPrime

                // use b to clip a
                val laPrime: FatLine = clipFatline(la, lb) ?: break
                la = laPrime
                val ka: Double = la.t.size() / aSize
                val kb: Double = lb.t.size() / bSize
                if (max(ka, kb) > 0.8) {
                    // TODO: switch over to subdivision at some point?
                    for (ap in la.split()) {
                        for (bp in lb.split()) {
                            queue.apply {
                                addLast(ap)
                                addLast(bp)
                            }
                        }
                    }
                    break
                }
            }
        }
        return normalize(acc.toTypedArray())
    }

    private fun isCollinear(
        a: Curve2,
        b: Curve2,
        `is`: Array<Vec2>
    ): Boolean {
        if (`is`.size != 2) {
            return false
        }
        for (i in 0 until MAX_CUBIC_CUBIC_INTERSECTIONS + 1) {
            val t: Double = i.toDouble() / MAX_CUBIC_CUBIC_INTERSECTIONS
            val pa: Vec2 = a.position(Scalars.lerp(`is`[0].x, `is`[1].x, t))
            val pb: Vec2 = b.position(Scalars.lerp(`is`[0].y, `is`[1].y, t))
            if (!Vec.equals<Vec2>(
                    pa,
                    pb,
                    SPATIAL_EPSILON
                )
            ) {
                return false
            }
        }
        return true
    }
}