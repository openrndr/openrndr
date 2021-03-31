package org.openrndr.shape.internal

import org.openrndr.math.Vector2
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2

internal class BezierQuadraticSampler2D {
    private val recursionLimit = 8

    var distanceTolerance = 0.5
    private var distanceToleranceSquare = 0.0
    private val angleToleranceEpsilon = 0.01
    private val angleTolerance = 0.0

    internal var points: MutableList<Vector2> = mutableListOf()
    internal var direction = mutableListOf<Vector2>()

    private fun sample(x1: Vector2, x2: Vector2, x3: Vector2, level: Int) {
        if (level > recursionLimit) {
            return
        }

        val x12 = (x1 + x2) * 0.5
        val x23 = (x2 + x3) * 0.5
        val x123 = (x12 + x23) * 0.5

        val dx = x3.x - x1.x
        val dy = x3.y - x1.y
        var d = abs((x2.x - x3.x) * dy - (x2.y - x3.y) * dx)
        var da: Double

        if (d > colinearityEpsilon) {
            // Regular case
            //-----------------
            if (d * d <= distanceToleranceSquare * (dx * dx + dy * dy)) {
                // If the curvature doesn't exceed the distance_tolerance value
                // we tend to finish subdivisions.
                //----------------------
                if (angleTolerance < angleToleranceEpsilon) {
                    if (points.last().squaredDistanceTo(x123) > 0.0) {
                        direction.add(x3 - x1)
                        points.add(x123)
                    }
                    return
                }

                // Angle & Cusp Condition
                //----------------------
                da = abs(atan2(x3.y - x2.y, x3.x - x2.x) - atan2(x2.y - x1.y, x2.x - x1.x))
                if (da >= PI) da = 2 * PI - da

                if (da < angleTolerance) {
                    // Finally we can stop the recursion
                    //----------------------
                    if (points.last().squaredDistanceTo(x123) > 0.0) {
                        direction.add(x3 - x1)
                        points.add(x123)
                    }
                    return
                }
            }
        } else {
            // Collinear case
            //------------------
            da = dx * dx + dy * dy
            if (da == 0.0) {
                d = squaredDistance(x1.x, x1.y, x2.x, x2.y)
            } else {
                d = ((x2.x - x1.x) * dx + (x2.y - x1.y) * dy) / da
                if (d > 0 && d < 1) {
                    // Simple collinear case, 1---2---3
                    // We can leave just two endpoints
                    return
                }
                if (d <= 0)
                    d = squaredDistance(x2.x, x2.y, x1.x, x1.y)
                else if (d >= 1)
                    d = squaredDistance(x2.x, x2.y, x3.x, x3.y)
                else
                    d = squaredDistance(x2.x, x2.y, x1.x + d * dx, x1.y + d * dy)
            }
            if (d < distanceToleranceSquare) {
                if (points.last().squaredDistanceTo(x2) > 0.0) {
                    direction.add(x3 - x1)
                    points.add(x2)
                }
                return
            }
        }

        // Continue subdivision
        //----------------------
        sample(x1, x12, x123, level + 1)
        sample(x123, x23, x3, level + 1)
    }

    fun sample(x1: Vector2, x2: Vector2, x3: Vector2): Pair< List<Vector2>, List<Vector2> > {
        distanceToleranceSquare = distanceTolerance * distanceTolerance
        points.clear()
        direction.clear()
        points.add(x1); direction.add(x2-x1)
        sample(x1, x2, x3, 0)
        if (points.last().squaredDistanceTo(x3) > 0.0) {
            points.add(x3); direction.add(x3 - x2)
        }
        return Pair(points, direction)
    }

    companion object {
        private const val colinearityEpsilon = 1e-30

        private fun squaredDistance(x1: Double, y1: Double, x2: Double, y2: Double): Double {
            val dx = x2 - x1
            val dy = y2 - y1
            return dx * dx + dy * dy
        }
    }
}
