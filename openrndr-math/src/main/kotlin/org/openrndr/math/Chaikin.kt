package org.openrndr.math

/**
 * Chaikin's corner cutting algorithm generates an approximating curve from a [polyline]
 *
 * [Interactive Demo](https://observablehq.com/@infowantstobeseen/chaikins-curves)
 *
 * @param polyline a list of vectors describing the polyline
 * @param iterations the number of times to approximate
 * @param closed when the polyline is supposed to be a closed shape
 * @param bias a value above 0.0 and below 0.5 controlling
 * where new vertices are located. Lower values produce vertices near
 * existing vertices. Values near 0.5 produce biases new vertices towards
 * the mid point between existing vertices.
 */
tailrec fun chaikinSmooth(
    polyline: List<Vector2>,
    iterations: Int = 1,
    closed: Boolean = false,
    bias: Double = 0.25
): List<Vector2> {
    if (iterations <= 0 || polyline.size < 2) {
        return polyline
    }

    val result = if (closed) {
        (if (polyline.first() == polyline.last()) polyline else
            (polyline + polyline.first())).zipWithNext { p0, p1 ->
            listOf(
                p0.mix(p1, bias),
                p0.mix(p1, 1 - bias)
            )
        }.flatten()
    } else {
        listOf(polyline.first().copy()) +
                polyline.zipWithNext { p0, p1 ->
                    listOf(
                        p0.mix(p1, bias),
                        p0.mix(p1, 1 - bias)
                    )
                }.flatten() +
                polyline.last().copy()
    }

    return chaikinSmooth(result, iterations - 1, closed, bias)
}
