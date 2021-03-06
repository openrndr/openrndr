package org.openrndr.math

/**
 * Chaikin's corner cutting algorithm generates an approximating curve from a [polyline]
 *
 * [Interactive Demo](https://observablehq.com/@infowantstobeseen/chaikins-curves)
 *
 * @param polyline a list of vectors describing the polyline
 * @param iterations the number of times to approximate
 * @param closed when the polyline is supposed to be a closed shape
 */
fun chaikinSmooth(polyline: List<Vector2>, iterations: Int = 1, closed: Boolean = false): List<Vector2> {
    if (iterations <= 0) {
        return polyline
    }

    val output = mutableListOf<Vector2>()

    if (!closed && polyline.isNotEmpty()) {
        output.add(polyline.first().copy())
    }

    val count = if (closed) polyline.size else polyline.size - 1

    for (i in 0 until count) {
        val p0 = polyline[i]
        val p1 = polyline[(i + 1) % polyline.size]

        val p0x = p0.x
        val p0y = p0.y
        val p1x = p1.x
        val p1y = p1.y

        val Q = Vector2(0.75 * p0x + 0.25 * p1x, 0.75 * p0y + 0.25 * p1y)
        val R = Vector2(0.25 * p0x + 0.75 * p1x, 0.25 * p0y + 0.75 * p1y)

        output.add(Q)
        output.add(R)
    }

    if (!closed && polyline.isNotEmpty()) {
        output.add(polyline.last().copy())
    }

    return chaikinSmooth(output, iterations - 1, closed)
}