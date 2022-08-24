package org.openrndr.shape.internal

import org.openrndr.math.EuclideanVector
import kotlin.math.PI
import kotlin.math.absoluteValue

private operator fun <T:EuclideanVector<T>> Tt<T>.plus(other: Tt<T>) = Tt(first + other.first, second + other.second)
private operator fun <T:EuclideanVector<T>> Tt<T>.times(scale: Double) = Tt(first * scale, second * scale)
private fun <T:EuclideanVector<T>> Tt<T>.squaredDistanceTo(other: Tt<T>) = first.squaredDistanceTo(other.first)
private fun <T:EuclideanVector<T>> Tt<T>.squaredDistanceTo(other: T) = first.squaredDistanceTo(other)

internal class BezierQuadraticSamplerT<T : EuclideanVector<T>> {
    private val recursionLimit = 8

    var distanceTolerance = 0.5
    private var distanceToleranceSquare = 0.0
    private val angleToleranceEpsilon = 0.01
    private val angleTolerance = 0.0

    internal var points: MutableList<Tt<T>> = mutableListOf()

    private fun sample(vt1: Tt<T>, vt2: Tt<T>, vt3: Tt<T>, level: Int) {
        if (level > recursionLimit) {
            return
        }

        val vt12 = (vt1 + vt2) * 0.5
        val vt23 = (vt2 + vt3) * 0.5
        val vt123 = (vt12 + vt23) * 0.5

        val x1 = vt1.first
        val x2 = vt2.first
        val x3 = vt3.first

        val d = x3 - x1
        val d1 = (x2-x3).areaBetween(x3-x1)

        if (d1 > colinearityEpsilon) {
            // Regular case
            //-----------------
            if (d1 * d1 <= distanceToleranceSquare * d.squaredLength) {
                // If the curvature doesn't exceed the distance_tolerance value
                // we tend to finish subdivisions.
                //----------------------
                if (angleTolerance < angleToleranceEpsilon) {
                    if (points.last().squaredDistanceTo(vt123) > 0.0) {
                        points.add(vt123)
                    }
                    return
                }

                // Angle & Cusp Condition
                //----------------------
                var da = (x2-x1).atan2(x3-x2).absoluteValue //  abs(atan2(x3.y - x2.y, x3.x - x2.x) - atan2(x2.y - x1.y, x2.x - x1.x))
                if (da >= PI) da = 2 * PI - da

                if (da < angleTolerance) {
                    // Finally we can stop the recursion
                    //----------------------
                    if (points.last().squaredDistanceTo(vt123) > 0.0) {
                        points.add(vt123)
                    }
                    return
                }
            }
        } else {
            // Collinear case
            //------------------
            val da = d.squaredLength
            var dl: Double
            if (da == 0.0) {
                dl = x1.squaredDistanceTo(x2)
            } else {
                dl = (x2-x1).dot(d) / da // ((x2.x - x1.x) * dx + (x2.y - x1.y) * dy) / da
                if (dl > 0 && dl < 1) {
                    // Simple collinear case, 1---2---3
                    // We can leave just two endpoints
                    return
                }
                if (dl <= 0)
                    dl = x2.squaredDistanceTo(x1)
                else if (dl >= 1)
                    dl = x2.squaredDistanceTo(x3)
                else
                    dl = x2.squaredDistanceTo(x1 + d * dl)//squaredDistance(x2.x, x2.y, x1.x + d * dx, x1.y + d * dy)
            }
            if (dl < distanceToleranceSquare) {
                if (points.last().squaredDistanceTo(x2) > 0.0) {
                    points.add(vt2)
                }
                return
            }
        }

        // Continue subdivision
        //----------------------
        sample(vt1, vt12, vt123, level + 1)
        sample(vt123, vt23, vt3, level + 1)
    }

    fun sample(x1: T, x2: T, x3: T): List<Tt<T>> {
        distanceToleranceSquare = distanceTolerance * distanceTolerance
        points.clear()
        points.add(x1 to 0.0)
        sample(x1 to 0.0, x2 to 0.5, x3 to 1.0, 0)
        if (points.last().squaredDistanceTo(x3) > 0.0) {
            points.add(x3 to 1.0)
        }
        return points
    }

    companion object {
        private const val colinearityEpsilon = 1e-30
    }
}
