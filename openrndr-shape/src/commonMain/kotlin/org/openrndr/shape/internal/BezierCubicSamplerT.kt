package org.openrndr.shape.internal

import org.openrndr.math.EuclideanVector
import kotlin.math.PI
import kotlin.math.absoluteValue

internal typealias Tt<T> = Pair<T, Double>
private operator fun <T:EuclideanVector<T>> Tt<T>.plus(other: Tt<T>) = Tt(first + other.first, second + other.second)
private operator fun <T:EuclideanVector<T>> Tt<T>.times(scale: Double) = Tt(first * scale, second * scale)
private fun <T:EuclideanVector<T>> Tt<T>.squaredDistanceTo(other: Tt<T>) = first.squaredDistanceTo(other.first)
private fun <T:EuclideanVector<T>> Tt<T>.squaredDistanceTo(other: T) = first.squaredDistanceTo(other)

internal class BezierCubicSamplerT<T:EuclideanVector<T>> {
    private val points = mutableListOf<Tt<T>>()

    private var distanceToleranceSquare = 0.0
    private val angleTolerance = 0.0
    private val cuspLimit = 0.0
    var distanceTolerance = 0.5

    fun sample(x1: T, x2: T, x3: T, x4: T): List<Tt<T>> {
        distanceToleranceSquare = distanceTolerance * distanceTolerance
        points.clear()
        points.add(x1 to 0.0)
        sample(x1 to 0.0, x2 to 1.0/3.0, x3 to 2.0/3.0, x4 to 1.0, 0)
        if (points.last().first.squaredDistanceTo(x4) > 0.0) {
            points.add(x4 to 1.0)
        }
        return points
    }

    private fun sample(vt1: Tt<T>, vt2 : Tt<T>, vt3: Tt<T>, vt4: Tt<T>, level: Int) {
        val x1 = vt1.first
        val x2 = vt2.first
        val x3 = vt3.first
        val x4 = vt4.first

        if (level > recursionLimit) {
            return
        }

        val vt12 = (vt1 + vt2) * 0.5
        val vt23 = (vt2 + vt3) * 0.5
        val vt34 = (vt3 + vt4) * 0.5

        val vt123 = (vt12 + vt23) * 0.5
        val vt234 = (vt23 + vt34) * 0.5

        val vt1234 = (vt123 + vt234) * 0.5

        val d = x4 - x1
        var d2 = (x2-x4).areaBetween(d)
        var d3 = (x3-x4).areaBetween(d)

        val p1 = d2 > colinearityEpsilon
        val p0 = d3 > colinearityEpsilon
        val p = (if (p1) 2 else 0) + if (p0) 1 else 0

        var k: Double
        when (p) {
            0 -> {
                k = d.squaredLength
                if (k == 0.0) {
                    d2 = x1.squaredDistanceTo(x2)
                    d3 = x3.squaredDistanceTo(x4)
                } else {
                    k = 1 / k

                    d2 = run {
                        val dl = x2 - x1
                        k * d.dot(dl)
                    }

                    d3 = run {
                        val dl = x3 - x1
                        k * d.dot(dl)
                    }
                    if (d2 > 0 && d2 < 1 && d3 > 0 && d3 < 1) {
                        // Simple collinear case, 1---2---3---4
                        // We can leave just two endpoints
                        return
                    }

                    d2 = when {
                        d2 <= 0 -> x1.squaredDistanceTo(x2)
                        d2 >= 1 -> x2.squaredDistanceTo(x4)
                        else -> x2.squaredDistanceTo(x1 + d * d2) // squareDistance(x2.x, x2.y, x1.x + d2 * dx, x1.y + d2 * dy)
                    }

                    d3 = when {
                        d3 <= 0 -> x3.squaredDistanceTo(x1)
                        d3 >= 1 -> x3.squaredDistanceTo(x4)
                        else -> x3.squaredDistanceTo(x1 + d * d3) //squareDistance(x3.x, x3.y, x1.x + d3 * dx, x1.y + d3 * dy)
                    }

                }
                if (d2 > d3) {
                    if (d2 < distanceToleranceSquare) {
                        if (points.last().first.squaredDistanceTo(x2) > 0.0) {
                            points.add(vt2)
                        }
                        return
                    }
                } else {
                    if (d3 < distanceToleranceSquare) {
                        if (points.last().first.squaredDistanceTo(x3) > 0.0) {
                            points.add(vt3)
                        }
                        return
                    }
                }
            }
            1 ->
                // p1,p2,p4 are collinear, p3 is significant
                //----------------------
                if (d3 * d3 <= distanceToleranceSquare * d.squaredLength) {
                    if (angleTolerance < angleToleranceEpsilon) {
                        if (points.last().squaredDistanceTo(vt23) > 0.0) {
                            points.add(vt23)
                        }
                        return
                    }

                    // Angle Condition
                    //----------------------
                    var da1 = (x4-x3).atan2(x3-x2).absoluteValue // abs(atan2(x4.y - x3.y, x4.x - x3.x) - atan2(x3.y - x2.y, x3.x - x2.x))
                    if (da1 >= PI) da1 = 2 * PI - da1

                    if (da1 < angleTolerance) {
                        if (points.last().squaredDistanceTo(x2) > 0.0) {
                            points.add(vt2)
                        }
                        if (points.last().squaredDistanceTo(x3) > 0.0) {
                            points.add(vt3)
                        }
                        return
                    }

                    if (cuspLimit != 0.0) {
                        if (da1 > cuspLimit) {
                            if (points.last().squaredDistanceTo(x3) > 0.0) {
                                points.add(vt3)
                            }
                            return
                        }
                    }
                }
            2 ->
                // p1,p3,p4 are collinear, p2 is significant
                //----------------------
                if (d2 * d2 <= distanceToleranceSquare * d.squaredLength) {
                    if (angleTolerance < angleToleranceEpsilon) {
                        if (points.last().squaredDistanceTo(vt23) > 0.0) {
                            points.add(vt23)
                        }
                        return
                    }

                    // Angle Condition
                    //----------------------
                    var da1 = (x3-x2).atan2(x2-x1).absoluteValue //abs(atan2(x3.y - x2.y, x3.x - x2.x) - atan2(x2.y - x1.y, x2.x - x1.x))
                    if (da1 >= PI) da1 = 2 * PI - da1

                    if (da1 < angleTolerance) {
                        if (points.last().squaredDistanceTo(vt2) > 0.0) {
                            points.add(vt2)
                        }
                        if (points.last().squaredDistanceTo(vt3) > 0.0) {
                            points.add(vt3)
                        }
                        return
                    }

                    if (cuspLimit != 0.0) {
                        if (da1 > cuspLimit) {
                            if (points.last().squaredDistanceTo(vt2) > 0.0) {
                                points.add(vt2)
                            }
                            return
                        }
                    }
                }
            3 ->
                // Regular case
                //-----------------
                if ((d2 + d3) * (d2 + d3) <= distanceToleranceSquare * d.squaredLength) {
                    // If the curvature doesn't exceed the distance_tolerance value
                    // we tend to finish subdivisions.
                    //----------------------
                    if (angleTolerance < angleToleranceEpsilon) {
                        if (points.last().squaredDistanceTo(vt23) > 0.0) {
                            points.add(vt23)
                        }
                        return
                    }

                    // Angle & Cusp Condition
                    //----------------------
                    var da1 = (x3-x2).atan2(x2-x1).absoluteValue
                    var da2 = (x4-x3).atan2(x3-x2).absoluteValue
                    //
                    if (da1 >= PI) da1 = 2 * PI - da1
                    if (da2 >= PI) da2 = 2 * PI - da2

                    if (da1 + da2 < angleTolerance) {
                        // Finally we can stop the recursion
                        //----------------------
                        if (points.last().squaredDistanceTo(vt23) > 0.0) {
                            points.add(vt23)
                        }
                        return
                    }

                    if (cuspLimit != 0.0) {
                        if (da1 > cuspLimit) {
                            if (points.last().squaredDistanceTo(vt2) > 0.0) {
                                points.add(vt2)
                            }
                            return
                        }

                        if (da2 > cuspLimit) {
                            if (points.last().squaredDistanceTo(vt3) > 0.0) {
                                points.add(vt3)
                            }
                            return
                        }
                    }
                }
        }
        sample(vt1, vt12, vt123, vt1234, level + 1)
        sample(vt1234, vt234, vt34, vt4, level + 1)
    }

    companion object {
        private const val colinearityEpsilon = 1e-30
        private const val angleToleranceEpsilon = 0.01
        private const val recursionLimit = 12
    }
}
