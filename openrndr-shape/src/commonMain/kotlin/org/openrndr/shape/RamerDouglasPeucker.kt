package org.openrndr.shape

import org.openrndr.math.Vector2

/**
 * The [Ramer–Douglas–Peucker algorithm](https://en.wikipedia.org/wiki/Ramer–Douglas–Peucker_algorithm),
 * is an algorithm that decimates a curve composed of line segments to a similar curve with fewer points.
 *
 * When the epsilon is less than the distance between two points, [simplify] is applied recursively.
 *
 * @author Edwin Jakobs
 *
 * @param epsilon Maximum distance two points can have without simplifying them.
 */
fun simplify(points: List<Vector2>, epsilon: Double): List<Vector2> {
    // Find the point with the maximum distance

    val startEndDistance = points.first().squaredDistanceTo(points.last())

    val endIndex = if (startEndDistance < 1E-6) points.size-2 else points.size-1

    var dMax = 0.0
    var index = 0
    val end = points.size
    for (i in 1..(end - 2)) {
        val ls = LineSegment(points[0], points[endIndex]).extend(1000000.0)
        val d = ls.distance(points[i])
        if (d > dMax) {
            index = i
            dMax = d
        }

    }
    // If max distance is greater than epsilon, recursively org.openrndr.shape.simplify
    return if (dMax > epsilon) {
        // Recursive call
        val recResults1 = simplify(points.subList(0, index + 1), epsilon)
        val recResults2 = simplify(points.subList(index, end), epsilon)
        // Build the result list
        listOf(recResults1.subList(0, recResults1.lastIndex), recResults2).flatMap { it.toList() }
    } else {
        listOf(points[0], points[end - 1])
    }
}