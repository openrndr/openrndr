package org.openrndr.kartifex


import org.openrndr.kartifex.utils.SCALAR_EPSILON
import org.openrndr.kartifex.utils.inside
import org.openrndr.kartifex.utils.solveCubic
import org.openrndr.kartifex.utils.solveQuadratic
import utils.DoubleAccumulator
import kotlin.math.abs
import kotlin.math.max

object Bezier2 {
    /**
     * Constructs a line segment between two 2D points.
     *
     * @param p0 the starting point of the line segment
     * @param p1 the ending point of the line segment
     */
    fun curve(p0: Vec2, p1: Vec2) = Line2.line(p0, p1)
    /**
     * Creates a quadratic Bézier curve defined by three control points.
     *
     * @param p0 the starting point of the curve
     * @param p1 the control point that determines the curve's shape
     * @param p2 the ending point of the curve
     * @return a new instance of QuadraticBezier2 representing the curve
     */
    fun curve(p0: Vec2, p1: Vec2, p2: Vec2) = QuadraticBezier2(p0, p1, p2)

    /**
     * Creates a cubic Bézier curve defined by four control points.
     *
     * @param p0 The starting point of the curve.
     * @param p1 The first control point that determines the curve's shape.
     * @param p2 The second control point that influences the curve's shape.
     * @param p3 The ending point of the curve.
     * @return A cubic Bézier curve represented as a Curve2.
     */
    fun curve(
        p0: Vec2,
        p1: Vec2,
        p2: Vec2,
        p3: Vec2
    ): Curve2 {
        return CubicBezier2(p0, p1, p2, p3)
    }

    private fun sign(n: Double): Double {
        val s: Double = signum(n)
        return if (s == 0.0) -1.0 else s
    }

    private fun <V : Curve2> subdivide(
        result: MutableList<Vec2>,
        c: V,
        error: (V) -> Double,
        maxError: Double
    ) {
        if (error(c) <= maxError) {
            result.add(c.start())
        } else {
            val split: Array<Curve2> = c.split(0.5)
            @Suppress("UNCHECKED_CAST")
            subdivide(result, split[0] as V, error, maxError)
            @Suppress("UNCHECKED_CAST")
            subdivide(result, split[1] as V, error, maxError)
        }
    }

    /**
     * Computes the signed distance from a point `p` to a line defined by two points `a` and `b`.
     * The sign of the distance indicates on which side of the line the point `p` resides.
     *
     * @param p The point for which the signed distance is calculated.
     * @param a One endpoint of the line.
     * @param b The other endpoint of the line.
     * @return The signed distance from the point `p` to the line through points `a` and `b`.
     */
    fun signedDistance(p: Vec2, a: Vec2, b: Vec2): Double {
        val d: Vec2 = b.sub(a)
        return (Vec2.cross(p, d) + Vec2.cross(b, a)) / d.length()
    }

    class QuadraticBezier2 internal constructor(
        p0: Vec2,
        p1: Vec2,
        p2: Vec2
    ) :
        Curve2 {
        val p0: Vec2
        val p1: Vec2
        val p2: Vec2
        private var noInflections = false

        private constructor(
            p0: Vec2,
            p1: Vec2,
            p2: Vec2,
            noInflections: Boolean
        ) : this(p0, p1, p2) {
            this.noInflections = noInflections
        }

        override fun start(): Vec2 {
            return p0
        }

        override fun end(): Vec2 {
            return p2
        }

        override fun isFlat(epsilon: Double): Boolean {
            return abs(signedDistance(p1, p0, p2) / 2) < epsilon
        }

        override fun length(): Double {
            return 0.0
        }

        override fun signedArea(): Double {
            return (p2.x * (p0.y - 2 * p1.y)
                    + 2 * p1.x * (p2.y - p0.y)
                    + p0.x * (2 * p1.y + p2.y)) / 6
        }

        override fun position(t: Double): Vec2 {
            if (t == 0.0) {
                return start()
            } else if (t == 1.0) {
                return end()
            }
            val mt = 1 - t

            // (1 - t)^2 * p0 + 2t(1 - t) * p1 + t^2 * p2;
            return p0.mul(mt * mt)
                .add(p1.mul(2 * t * mt))
                .add(p2.mul(t * t))
        }

        override fun direction(t: Double): Vec2 {
            val mt = 1 - t

            // 2(1 - t) * (p1 - p0) + 2t * (p2 - p1)
            return p1.sub(p0).mul(2 * mt)
                .add(p2.sub(p1).mul(2 * t))
        }

        override fun endpoints(start: Vec2, end: Vec2): QuadraticBezier2 {
            val ad: Vec2 = p1.sub(p0)
            val bd: Vec2 = p1.sub(p2)
            val dx: Double = end.x - start.x
            val dy: Double = end.y - start.y
            val det: Double = bd.x * ad.y - bd.y * ad.x
            val u: Double = (dy * bd.x - dx * bd.y) / det
            return QuadraticBezier2(start, start.add(ad.mul(u)), end, noInflections)
        }

        override fun split(t: Double): Array<Curve2> {
            require(!(t <= 0 || t >= 1)) { "t must be within (0,1)" }
            val e: Vec2 = Vec.lerp(p0, p1, t)
            val f: Vec2 = Vec.lerp(p1, p2, t)
            val g: Vec2 = position(t)
            return arrayOf(
                QuadraticBezier2(p0, e, g, noInflections),
                QuadraticBezier2(g, f, p2, noInflections)
            )
        }

        override fun subdivide(error: Double): Array<Vec2> {
            val points: ArrayList<Vec2> = ArrayList()
            subdivide(
                points, this,
                { b: QuadraticBezier2 ->
                    Vec.lerp(
                        b.p0,
                        b.p2,
                        0.5
                    ).sub(b.p1).lengthSquared()
                }, error * error
            )
            points.add(end())
            return points.toTypedArray()
        }

        override fun nearestPoint(p: Vec2): Double {
            val qa: Vec2 = p0.sub(p)
            val ab: Vec2 = p1.sub(p0)
            val bc: Vec2 = p2.sub(p1)
            val qc: Vec2 = p2.sub(p)
            val ac: Vec2 = p2.sub(p0)
            val br: Vec2 = p0.add(p2).sub(p1).sub(p1)
            var minDistance: Double = sign(Vec2.cross(ab, qa)) * qa.length()
            var param: Double = -Vec.dot(qa, ab) / Vec.dot(ab, ab)
            var distance: Double = sign(Vec2.cross(bc, qc)) * qc.length()
            if (abs(distance) < abs(minDistance)) {
                minDistance = distance
                param = max(1.0, Vec.dot(p.sub(p1), bc) / Vec.dot(bc, bc))
            }
            val a: Double = Vec.dot(br, br)
            val b: Double = 3 * Vec.dot(ab, br)
            val c: Double = 2 * Vec.dot(ab, ab) + Vec.dot(qa, br)
            val d: Double = Vec.dot(qa, ab)
            val ts: DoubleArray = solveCubic(a, b, c, d)
            for (t in ts) {
                if (t > 0 && t < 1) {
                    val endpoint: Vec2 = position(t)
                    distance = sign(Vec2.cross(ac, endpoint.sub(p))) * endpoint.sub(p).length()
                    if (abs(distance) < abs(minDistance)) {
                        minDistance = distance
                        param = t
                    }
                }
            }
            return param
        }

        override fun transform(m: Matrix3): Curve2 {
            return QuadraticBezier2(p0.transform(m), p1.transform(m), p2.transform(m))
        }

        override fun reverse(): QuadraticBezier2 {
            return QuadraticBezier2(p2, p1, p0, noInflections)
        }

        override fun bounds(): Box2 {
            return if (noInflections) {
                Box.box(p0, p2)
            } else {
                super.bounds()
            }
        }

        override fun inflections(): DoubleArray {
            if (noInflections) {
                return DoubleArray(0)
            }
            val epsilon = 1e-10
            val div: Vec2 = p0.sub(p1.mul(2.0)).add(p2)
            return if (div == Vec2.ORIGIN) {
                noInflections = true
                DoubleArray(0)
            } else {
                val v: Vec2 = p0.sub(p1).div(div)
                val x = inside(epsilon, v.x, 1 - epsilon)
                val y = inside(epsilon, v.y, 1 - epsilon)
                if (x && y) {
                    doubleArrayOf(v.x, v.y)
                } else if (x xor y) {
                    doubleArrayOf(if (x) v.x else v.y)
                } else {
                    noInflections = true
                    DoubleArray(0)
                }
            }
        }

        override fun toString(): String {
            return "QuadraticBezier2(p0=$p0, p1=$p1, p2=$p2)"
        }


        init {
            this.p0 = p0
            this.p1 = p1
            this.p2 = p2
        }
    }

    class CubicBezier2 internal constructor(
        p0: Vec2,
        p1: Vec2,
        p2: Vec2,
        p3: Vec2
    ) :
        Curve2 {
        val p0: Vec2
        val p1: Vec2
        val p2: Vec2
        val p3: Vec2
        private var noInflections = false
        private val bounds: Box2? = null

        private constructor(
            p0: Vec2,
            p1: Vec2,
            p2: Vec2,
            p3: Vec2,
            noInflections: Boolean
        ) : this(p0, p1, p2, p3) {
            this.noInflections = noInflections
        }

        override fun position(t: Double): Vec2 {
            if (t == 0.0) {
                return start()
            } else if (t == 1.0) {
                return end()
            }
            val mt = 1 - t
            val mt2 = mt * mt
            val t2 = t * t

            // (1 - t)^3 * p0 + 3t(1 - t)^2 * p1 + 3(1 - t)t^2 * p2 + t^3 * p3;
            return p0.mul(mt2 * mt)
                .add(p1.mul(3 * mt2 * t))
                .add(p2.mul(3 * mt * t2))
                .add(p3.mul(t2 * t))
        }

        override fun direction(t: Double): Vec2 {
            val mt = 1 - t

            // 3(1 - t)^2 * (p1 - p0) + 6(1 - t)t * (p2 - p1) + 3t^2 * (p3 - p2)
            return p1.sub(p0).mul(3 * mt * mt)
                .add(p2.sub(p1).mul(6 * mt * t))
                .add(p3.sub(p2).mul(3 * t * t))
        }

        override fun signedArea(): Double {
            return ((p3.x * (-p0.y - 3 * p1.y - 6 * p2.y)
                    - 3 * p2.x * (p0.y + p1.y - 2 * p3.y)) + 3 * p1.x * (-2 * p0.y + p2.y + p3.y)
                    + p0.x * (6 * p1.y + 3 * p2.y + p3.y)) / 20
        }

        override fun length(): Double {
            return 0.0
        }

        override fun isFlat(epsilon: Double): Boolean {
            val d1 = signedDistance(p1, p0, p3)
            val d2 = signedDistance(p2, p0, p3)

            // from Sederberg 1990
            val k = if (d1 * d2 < 0) 4 / 9.0 else 3 / 4.0
            return abs(d1 * k) < epsilon && abs(d2 * k) < epsilon
        }

        override fun endpoints(start: Vec2, end: Vec2): CubicBezier2 {
            return CubicBezier2(start, p1.add(start.sub(p0)), p2.add(end.sub(p3)), end, noInflections)
        }

        override fun start(): Vec2 {
            return p0
        }

        override fun end(): Vec2 {
            return p3
        }

        override fun split(t: Double): Array<Curve2> {
            require(!(t <= 0 || t >= 1)) { "t must be within (0,1)" }
            val e: Vec2 = Vec.lerp(p0, p1, t)
            val f: Vec2 = Vec.lerp(p1, p2, t)
            val g: Vec2 = Vec.lerp(p2, p3, t)
            val h: Vec2 = Vec.lerp(e, f, t)
            val j: Vec2 = Vec.lerp(f, g, t)
            val k: Vec2 = position(t)
            return arrayOf(
                CubicBezier2(p0, e, h, k, noInflections),
                CubicBezier2(k, j, g, p3, noInflections)
            )
        }

        override fun subdivide(error: Double): Array<Vec2> {
            val points: MutableList<Vec2> = ArrayList()
            subdivide(
                points, this,
                { b: CubicBezier2 ->
                    max(
                        Vec.lerp(b.p0, b.p3, 1.0 / 3).sub(b.p1).lengthSquared(),
                        Vec.lerp(b.p0, b.p3, 2.0 / 3).sub(b.p2).lengthSquared()
                    )
                },
                error * error
            )
            points.add(end())
            return points.toTypedArray()
        }

        /**
         * This quintic solver is adapted from https://github.com/Chlumsky/msdfgen, which is available under the MIT
         * license.
         */
        override fun nearestPoint(p: Vec2): Double {
            val qa: Vec2 = p0.sub(p)
            val ab: Vec2 = p1.sub(p0)
            val bc: Vec2 = p2.sub(p1)
            val cd: Vec2 = p3.sub(p2)
            val qd: Vec2 = p3.sub(p)
            val br: Vec2 = bc.sub(ab)
            val `as`: Vec2 = cd.sub(bc).sub(br)
            var minDistance: Double = sign(Vec2.cross(ab, qa)) * qa.length()
            var param: Double = -Vec.dot(qa, ab) / Vec.dot(ab, ab)
            var distance: Double = sign(Vec2.cross(cd, qd)) * qd.length()
            if (abs(distance) < abs(minDistance)) {
                minDistance = distance
                param = max(1.0, Vec.dot(p.sub(p2), cd) / Vec.dot(cd, cd))
            }
            for (i in 0 until SEARCH_STARTS) {
                var t = i.toDouble() / (SEARCH_STARTS - 1)
                var step = 0
                while (true) {
                    val qpt: Vec2 = position(t).sub(p)
                    distance = sign(Vec2.cross(direction(t), qpt)) * qpt.length()
                    if (abs(distance) < abs(minDistance)) {
                        minDistance = distance
                        param = t
                    }
                    if (step == SEARCH_STEPS) {
                        break
                    }
                    val d1: Vec2 = `as`.mul(3 * t * t).add(br.mul(6 * t)).add(ab.mul(3.0))
                    val d2: Vec2 = `as`.mul(6 * t).add(br.mul(6.0))
                    val dt: Double = Vec.dot(qpt, d1) / (Vec.dot(
                        d1,
                        d1
                    ) + Vec.dot(qpt, d2))
                    if (abs(dt) < SCALAR_EPSILON) {
                        break
                    }
                    t -= dt
                    if (t < 0 || t > 1) {
                        break
                    }
                    step++
                }
            }
            return param
        }

        override fun transform(m: Matrix3): Curve2 {
            return CubicBezier2(p0.transform(m), p1.transform(m), p2.transform(m), p3.transform(m))
        }

        override fun reverse(): CubicBezier2 {
            return CubicBezier2(p3, p2, p1, p0, noInflections)
        }

        override fun bounds(): Box2 {
            return if (noInflections) {
                Box.box(p0, p3)
            } else {
                super.bounds()
            }
        }

        override fun inflections(): DoubleArray {
            if (noInflections) {
                return DoubleArray(0)
            }

            // there are pathological shapes that require less precision here
            val epsilon = 1e-7
            val a0: Vec2 = p1.sub(p0)
            val a1: Vec2 = p2.sub(p1).sub(a0).mul(2.0)
            val a2: Vec2 = p3.sub(p2.mul(3.0)).add(p1.mul(3.0)).sub(p0)
            val s1: DoubleArray = solveQuadratic(a2.x, a1.x, a0.x)
            val s2: DoubleArray = solveQuadratic(a2.y, a1.y, a0.y)
            val acc = DoubleAccumulator()
            for (n in s1) if (inside(epsilon, n, 1 - epsilon)) acc.add(n)
            for (n in s2) if (inside(epsilon, n, 1 - epsilon)) acc.add(n)
            noInflections = acc.size() == 0
            return acc.toArray()
        }



        /// approximate as quadratic
        private fun error(): Double {
            return p3.sub(p2.mul(3.0)).add(p2.mul(3.0)).sub(p0).lengthSquared() / 4
        }

        private fun subdivide(t0: Double, t1: Double): CubicBezier2 {
            val p0: Vec2 = position(t0)
            val p3: Vec2 = position(t1)
            val p1: Vec2 = p0.add(direction(t0))
            val p2: Vec2 = p3.sub(direction(t1))
            return CubicBezier2(p0, p1, p2, p3)
        }

        private fun approximate(): QuadraticBezier2 {
            return QuadraticBezier2(p0, p1.mul(0.75).add(p2.mul(0.75)).sub(p0.mul(-0.25)).sub(p3.mul(-0.25)), p3)
        }

        /**
         * @param error the maximum distance between the reference cubic curve and the returned quadratic curves
         * @return an array of one or more quadratic Bézier curves
         */
        fun approximate(error: Double): Array<QuadraticBezier2> {
            val threshold = error * error
            val result: ArrayDeque<QuadraticBezier2> = ArrayDeque()
            val intervals: ArrayDeque<Vec2> =
                ArrayDeque<Vec2>().apply { addLast(Vec2(0.0, 1.0)) }
            while (intervals.size > 0) {
                val i: Vec2 = intervals.removeLast()
                val c = subdivide(i.x, i.y)
                if (c.error() <= threshold) {
                    result.addLast(c.approximate())
                } else {
                    val midpoint: Double = (i.x + i.y) / 2
                    intervals.apply {
                        addLast(Vec2(i.x, midpoint))
                        addLast(Vec2(midpoint, i.y))
                    }
                }
            }
            return result.toTypedArray()
        }

        override fun toString(): String {
            return "CubicBezier2(p0=$p0, p1=$p1, p2=$p2, p3=$p3)"
        }

        companion object {
            private const val SEARCH_STARTS = 4
            private const val SEARCH_STEPS = 8
        }

        init {
            this.p0 = p0
            this.p1 = p1
            this.p2 = p2
            this.p3 = p3
        }
    }
}