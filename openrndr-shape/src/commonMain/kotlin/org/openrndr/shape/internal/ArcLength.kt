package org.openrndr.shape.internal

import kotlin.math.abs

/** Positive abscissae of the 8-point Gauss-Legendre rule on [-1, 1]. */
private val GL_X = doubleArrayOf(0.1834346424956498, 0.5255324099163290, 0.7966664774136267, 0.9602898564975363)

/** Weights belonging to [GL_X]. */
private val GL_W = doubleArrayOf(0.3626837833783620, 0.3137066458778873, 0.2223810344533745, 0.1012285362903763)

/** Relative accuracy of [arcLength] with respect to the size of the curve. */
private const val ARC_LENGTH_ACCURACY = 1E-10

/** Maximum subdivision depth of [arcLength], only reached near cusps. */
private const val ARC_LENGTH_MAX_DEPTH = 16

private fun gaussLegendre(speed: (Double) -> Double, t0: Double, t1: Double): Double {
    val mid = (t0 + t1) * 0.5
    val half = (t1 - t0) * 0.5
    var sum = 0.0
    for (i in GL_X.indices) {
        sum += GL_W[i] * (speed(mid + half * GL_X[i]) + speed(mid - half * GL_X[i]))
    }
    return sum * half
}

private fun adaptiveGaussLegendre(
    speed: (Double) -> Double,
    t0: Double,
    t1: Double,
    whole: Double,
    accuracy: Double,
    depth: Int
): Double {
    val mid = (t0 + t1) * 0.5
    val left = gaussLegendre(speed, t0, mid)
    val right = gaussLegendre(speed, mid, t1)
    return if (depth == 0 || abs(left + right - whole) <= accuracy) {
        left + right
    } else {
        adaptiveGaussLegendre(speed, t0, mid, left, accuracy * 0.5, depth - 1) +
                adaptiveGaussLegendre(speed, mid, t1, right, accuracy * 0.5, depth - 1)
    }
}

/**
 * Arc length of a curve on t in [0, 1], computed by adaptive Gauss-Legendre integration of its [speed].
 *
 * @param size a measure of the size of the curve, such as the length of its control polygon, the
 * result is accurate to roughly `size * 1E-10`
 * @param speed the length of the derivative of the curve at t
 */
internal fun arcLength(size: Double, speed: (Double) -> Double): Double {
    if (size == 0.0) {
        return 0.0
    }
    return adaptiveGaussLegendre(
        speed, 0.0, 1.0, gaussLegendre(speed, 0.0, 1.0),
        size * ARC_LENGTH_ACCURACY, ARC_LENGTH_MAX_DEPTH
    )
}
