package org.openrndr.kartifex

import org.openrndr.kartifex.utils.Intersections
import kotlin.math.abs
import kotlin.math.sqrt


class Ring2 {
    class Result private constructor(val inside: Boolean, val curve: Curve2?) {

        constructor(curve: Curve2) : this(true, curve)

        override fun toString(): String {
            return if (inside) {
                if (curve == null) "INSIDE" else "EDGE: $curve"
            } else {
                "OUTSIDE"
            }
        }

        companion object {
            val INSIDE: Result = Result(true, null)
            val OUTSIDE: Result = Result(false, null)
        }
    }

    val curves: Array<Curve2>
    val bounds: Box2
    val isClockwise: Boolean
    val area: Double

    private constructor(
        curves: Array<Curve2>,
        bounds: Box2,
        isClockwise: Boolean,
        area: Double
    ) {
        this.curves = curves
        this.bounds = bounds
        this.isClockwise = isClockwise
        this.area = area
    }

    constructor(cs: Iterable<Curve2>) {
        // TODO: dedupe collinear adjacent lines
        var bounds: Box2 = Box2.EMPTY
        var signedArea = 0.0
        val list: ArrayDeque<Curve2> = ArrayDeque()
        for (a in cs) {
            for (b in a.split(a.inflections())) {
                list.addLast(b)
                bounds = bounds.union(b.start()).union(b.end())
                signedArea += b.signedArea()
            }
        }
        isClockwise = signedArea < 0
        area = abs(signedArea)
        this.bounds = bounds
        curves = list.toTypedArray()
        for (i in 0 until curves.size - 1) {
            curves[i] = curves[i].endpoints(curves[i].start(), curves[i + 1].start())
        }
        val lastIdx = curves.size - 1
        curves[lastIdx] = curves[lastIdx].endpoints(curves[lastIdx].start(), curves[0].start())
    }

    fun region(): Region2 {
        return Region2(listOf(this))
    }

    fun test(p: Vec2): Result {
        if (!bounds.expand(Intersections.SPATIAL_EPSILON).contains(p)) {
            return Result.OUTSIDE
        }
        val ray: Line2 = Line2.line(p, Vec2(bounds.ux + 1, p.y))
        var count = 0

        // since our curves have been split at inflection points, there can only
        // be a single ray/curve intersection unless the curve is collinear
        for (c in curves) {
            val b: Box2 = c.bounds()
            val flat = b.height() == 0.0

            //System.out.println(p + " " + b + " " + c);

            // it's to our right
            if (p.x < b.lx) {
                // check if we intersect within [bottom, top)
                if (p.y >= b.ly && p.y < b.uy) {
                    //System.out.println("right, incrementing");
                    count++
                }

                // we're inside the bounding box
            } else if (b.expand(Vec2(Intersections.SPATIAL_EPSILON, 0.0))
                    .contains(p)
            ) {
                val i: Vec2? =
                    Intersections.lineCurve(ray, c)
                        .map { v: Vec2 ->
                            v.map { n: Double ->
                                Intersections.round(
                                    n,
                                    Intersections.PARAMETRIC_EPSILON
                                )
                            }
                        }
                        .filter { v ->  Intersections.PARAMETRIC_BOUNDS.contains(v) }
                        .minByOrNull { v: Vec2 -> v.x }

                if (i != null) {
                    //System.out.println(i);
                    if (i.x == 0.0) {
                        return Result(c)
                    } else if (!flat && p.y < b.uy) {
                        //System.out.println("intersected, incrementing");
                        count++
                    }
                } else {
                    //System.out.println("no intersection");
                }
            }
        }

        //System.out.println(count);
        return if (count % 2 == 1) Result.INSIDE else Result.OUTSIDE
    }

    fun transform(m: Matrix3): Ring2 = Ring2(curves.map { c -> c.transform(m) })

    companion object {
        fun of(vararg cs: Curve2): Ring2 {
            return Ring2(cs.toList())
        }

        /**
         * @return a unit square from [0, 0] to [1, 1]
         */
        fun square(): Ring2 {
            return Box.box(Vec2(0.0, 0.0), Vec2(1.0, 1.0))
                .outline()
        }

        /**
         * @return a unit circle with radius of 1, centered at [0, 0]
         */
        fun circle(): Ring2 {
            // taken from http://whizkidtech.redprince.net/bezier/circle/kappa/
            val k: Double = 4.0 / 3.0 * (sqrt(2.0) - 1)
            return of(
                Bezier2.curve(
                    Vec2(1.0, 0.0),
                    Vec2(1.0, k),
                    Vec2(k, 1.0),
                    Vec2(0.0, 1.0)
                ),
                Bezier2.curve(
                    Vec2(0.0, 1.0),
                    Vec2(-k, 1.0),
                    Vec2(-1.0, k),
                    Vec2(-1.0, 0.0)
                ),
                Bezier2.curve(
                    Vec2(-1.0, 0.0),
                    Vec2(-1.0, -k),
                    Vec2(-k, -1.0),
                    Vec2(0.0, -1.0)
                ),
                Bezier2.curve(
                    Vec2(0.0, -1.0),
                    Vec2(k, -1.0),
                    Vec2(1.0, -k),
                    Vec2(1.0, 0.0)
                )
            )
        }
    }
}