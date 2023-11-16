package org.openrndr.shape

import kotlinx.serialization.Serializable
import org.openrndr.math.*
import org.openrndr.shape.internal.BezierCubicSamplerT
import org.openrndr.shape.internal.BezierQuadraticSamplerT
import kotlin.math.max

@Serializable
class Segment1D(val start: Double, val control: Array<Double>, val end: Double): LinearType<Segment1D> {

    /**
     * control points, zero-length iff the segment is linear
     */

    val linear: Boolean get() = control.isEmpty()

    fun position(ut: Double): Double {
        val t = ut.coerceIn(0.0, 1.0)
        return when (control.size) {
            0 -> start * (1.0 - t) + end * t
            1 -> bezier(start, control[0], end, t)
            2 -> bezier(start, control[0], control[1], end, t)
            else -> throw RuntimeException("unsupported number of control points")
        }
    }

    val reverse: Segment1D
        get() {
            return when (control.size) {
                0 -> Segment1D(end, start)
                1 -> Segment1D(end, control, start)
                2 -> Segment1D(end, control.reversed().toTypedArray(), start)
                else -> throw RuntimeException("unsupported number of control points")
            }
        }

    fun sub(t0: Double, t1: Double): Segment1D {
        // ftp://ftp.fu-berlin.de/tex/CTAN/dviware/dvisvgm/src/Bezier.cpp
        var z0 = t0
        var z1 = t1

        if (t0 > t1) {
            z1 = t0
            z0 = t1
        }

        return when {
            z0 == 0.0 -> split(z1)[0]
            z1 == 1.0 -> split(z0).last()
            else -> split(z0).last().split(map(z0, 1.0, 0.0, 1.0, z1))[0]
        }
    }

    fun split(t: Double): Array<Segment1D> {
        val u = t.clamp(0.0, 1.0)
        val splitSigma = 10E-6

        if (u < splitSigma) {
            return arrayOf(Segment1D(start, start), this)
        }

        if (u >= 1.0 - splitSigma) {
            return arrayOf(this, Segment1D(end, end))
        }

        if (linear) {
            val cut = start + (end.minus(start) * u)
            return arrayOf(Segment1D(start, cut), Segment1D(cut, end))
        } else {
            when (control.size) {
                2 -> {
                    @Suppress("UnnecessaryVariable")
                    val z = u
                    val z2 = z * z
                    val z3 = z * z * z
                    val iz = 1 - z
                    val iz2 = iz * iz
                    val iz3 = iz * iz * iz

                    val lsm = Matrix44(
                        1.0, 0.0, 0.0, 0.0,
                        iz, z, 0.0, 0.0,
                        iz2, 2.0 * iz * z, z2, 0.0,
                        iz3, 3.0 * iz2 * z, 3.0 * iz * z2, z3
                    )

                    val px = Vector4(start, control[0], control[1], end)

                    val pl = lsm * px//.multiply(lsm)

                    val pl0 = pl.x
                    val pl1 = pl.y
                    val pl2 = pl.z
                    val pl3 = pl.w

                    val left = Segment1D(pl0, arrayOf(pl1, pl2), pl3)

                    val rsm = Matrix44(
                        iz3, 3.0 * iz2 * z, 3.0 * iz * z2, z3,
                        0.0, iz2, 2.0 * iz * z, z2,
                        0.0, 0.0, iz, z,
                        0.0, 0.0, 0.0, 1.0
                    )

                    val pr = rsm * px

                    val pr0 = pr.x
                    val pr1 = pr.y
                    val pr2 = pr.z
                    val pr3 = pr.w

                    val right = Segment1D(pr0, arrayOf(pr1, pr2), pr3)

                    return arrayOf(left, right)
                }

                1 -> {
                    @Suppress("UnnecessaryVariable")
                    val z = u
                    val iz = 1 - z
                    val iz2 = iz * iz
                    val z2 = z * z

                    val lsm = Matrix44(
                        1.0, 0.0, 0.0, 0.0,
                        iz, z, 0.0, 0.0,
                        iz2, 2.0 * iz * z, z2, 0.0,
                        0.0, 0.0, 0.0, 0.0
                    )

                    val p = Vector4(start, control[0], end, 0.0)

                    val pl = lsm * p

                    val left = Segment1D(
                        pl.x,
                        pl.y,
                        pl.z
                    )

                    val rsm = Matrix44(
                        iz2, 2.0 * iz * z, z2, 0.0,
                        0.0, iz, z, 0.0,
                        0.0, 0.0, 1.0, 0.0,
                        0.0, 0.0, 0.0, 0.0
                    )

                    val pr = rsm * p

                    val rd0 = pr.y - pr.x
                    val rd1 = pr.z - pr.y


                    require(rd0 * rd0 > 0.0) {
                        "Q start/c0 overlap after split on $t $this"
                    }
                    require(rd1 * rd1 > 0.0) {
                        "Q end/c0 overlap after split on $t $this"
                    }

                    val right = Segment1D(pr.x, pr.y, pr.z)

                    return arrayOf(left, right)
                }

                else -> error("unsupported number of control points")
            }
        }
    }

    val quadratic: Segment1D
        get() = when {
            control.size == 1 -> this
            linear -> {
                val delta = end - start
                Segment1D(start, start + delta * (1.0 / 2.0), end)
            }
            else -> error("cannot convert to quadratic segment")
        }

    val cubic: Segment1D
        get() = when {
            control.size == 2 -> this
            control.size == 1 -> {
                Segment1D(
                    start,
                    arrayOf(
                    start * (1.0 / 3.0) + control[0] * (2.0 / 3.0),
                    control[0] * (2.0 / 3.0) + end * (1.0 / 3.0)),
                    end
                )
            }
            linear -> {
                val delta = end - start
                Segment1D(
                    start,
                    arrayOf(
                    start + delta * (1.0 / 3.0),
                    start + delta * (2.0 / 3.0)),
                    end
                )
            }
            else -> error("cannot convert to cubic segment")
        }



    override fun plus(right: Segment1D): Segment1D {
        when(val cmax = max(control.size, right.control.size)) {
            0 -> return Segment1D(start + right.start, end + right.end)
            1 -> {
                val qthis = this.quadratic
                val qright = right.quadratic
                return Segment1D(qthis.start + qright.start, qthis.control[0] + qright.control[0], qthis.end + qright.end)
            }
            2 -> {
                val cthis = this.cubic
                val cright = right.cubic
                return Segment1D(cthis.start + cright.start,
                    arrayOf(
                    cthis.control[0] + cright.control[0],
                    cthis.control[1] + cright.control[1]),
                    cthis.end + cright.end)
            }
            else -> {
                error("number of control points ($cmax) is not supported")
            }
        }
    }

    override fun minus(right: Segment1D): Segment1D {
        when(val cmax = max(control.size, right.control.size)) {
            0 -> return Segment1D(start - right.start, end - right.end)
            1 -> {
                val qthis = this.quadratic
                val qright = right.quadratic
                return Segment1D(qthis.start - qright.start, qthis.control[0] - qright.control[0], qthis.end - qright.end)
            }
            2 -> {
                val cthis = this.cubic
                val cright = right.cubic
                return Segment1D(cthis.start - cright.start,
                    arrayOf(
                    cthis.control[0] - cright.control[0],
                    cthis.control[1] - cright.control[1]),
                    cthis.end - cright.end)
            }
            else -> {
                error("number of control points ($cmax) is not supported")
            }
        }
    }

    override fun times(scale: Double): Segment1D {
        return when (control.size) {
            0 -> Segment1D(start * scale, end * scale)
            1 -> Segment1D(start * scale, control[0] * scale, end * scale)
            2 -> Segment1D(start * scale, control[0] * scale, control[1] * scale, end * scale)
            else -> error("number of control points (${control.size}) is not supported")
        }
    }

    override fun div(scale: Double): Segment1D {
        return when (control.size) {
            0 -> Segment1D(start / scale, end / scale)
            1 -> Segment1D(start / scale, control[0] / scale, end / scale)
            2 -> Segment1D(start / scale, control[0] / scale, control[1] / scale, end / scale)
            else -> error("number of control points (${control.size}) is not supported")
        }
    }

    override fun toString(): String {
        return "Segment1D(start=$start, end=$end, control=${control.contentToString()})"
    }

    fun adaptivePositions(distanceTolerance: Double = 25.0): List<Double> =
        adaptivePositionsWithT(distanceTolerance).map { it.first }

    fun adaptivePositionsWithT(distanceTolerance: Double = 25.0): List<Pair<Double, Double>> = when (control.size) {
        0 -> listOf(start to 0.0, end to 1.0)
        1 -> BezierQuadraticSamplerT<Vector1>().apply { this.distanceTolerance = distanceTolerance }.sample(Vector1(start), Vector1(control[0]), Vector1(end)).map { Pair(it.first.x, it.second) }
        2 -> BezierCubicSamplerT<Vector1>().apply { this.distanceTolerance = distanceTolerance }.sample(Vector1(start), Vector1(control[0]), Vector1(control[1]), Vector1(end)).map { Pair(it.first.x, it.second) }
        else -> throw RuntimeException("unsupported number of control points")
    }

    fun equidistantPositions(pointCount: Int, distanceTolerance: Double = 0.5): List<Double> {
        return sampleEquidistant(adaptivePositions(distanceTolerance).map { Vector1(it) }, pointCount).map { it.x }
    }
}

fun Segment1D(start: Double, end: Double) = Segment1D(
    start,
    emptyArray(),
    end)

fun Segment1D(start: Double, c0: Double, end: Double) = Segment1D(
    start,
    arrayOf(c0),
    end)

fun Segment1D(start: Double, c0: Double, c1: Double, end: Double) = Segment1D(
    start,
    arrayOf(c0, c1),
    end)
