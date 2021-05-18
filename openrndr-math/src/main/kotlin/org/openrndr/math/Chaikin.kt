package org.openrndr.math

/**
 * Chaikin's corner cutting algorithm generates an approximating curve from a [polyline]
 *
 * [Interactive Demo](https://observablehq.com/@infowantstobeseen/chaikins-curves)
 *
 * The code has been tweaked for performance
 * instead of brevity or being idiomatic.
 *
 * @param polyline a list of vectors describing the polyline
 * @param iterations the number of times to approximate
 * @param closed when the polyline is supposed to be a closed shape
 * @param bias a value above 0.0 and below 0.5 controlling
 * where new vertices are located. Lower values produce vertices near
 * existing vertices. Values near 0.5 biases new vertices towards
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

    val biasInv = 1 - bias
    val result = ArrayList<Vector2>(polyline.size * 2)

    if (closed) {

        val sz = polyline.size
        for (i in 0 until sz) {
            val p0 = polyline[i] // `if` is here faster than `%`
            val p1 = polyline[if (i + 1 == sz) 0 else i + 1]

            val (p0x, p0y) = p0
            val (p1x, p1y) = p1

            result.apply {
                add(
                    Vector2(
                        biasInv * p0x + bias * p1x,
                        biasInv * p0y + bias * p1y
                    )
                )
                add(
                    Vector2(
                        bias * p0x + biasInv * p1x,
                        bias * p0y + biasInv * p1y
                    )
                )
            }
        }

    } else {

        // make sure it starts at point 0
        result.add(polyline[0].copy())
        val sz = polyline.size - 1
        for (i in 0 until sz) {
            val p0 = polyline[i]
            val p1 = polyline[i + 1]

            val (p0x, p0y) = p0
            val (p1x, p1y) = p1

            result.apply {
                add(
                    Vector2(
                        biasInv * p0x + bias * p1x,
                        biasInv * p0y + bias * p1y
                    )
                )
                add(
                    Vector2(
                        bias * p0x + biasInv * p1x,
                        bias * p0y + biasInv * p1y
                    )
                )
            }
        }
        // make sure it ends at the last point
        result.add(polyline[sz].copy())

    }
    return chaikinSmooth(result, iterations - 1, closed, bias)
}