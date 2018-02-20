@file:Suppress("unused")

package org.openrndr.math

/**
 * Linearly maps a value, which is given in the before domain to a value in the after domain
 * @param beforeLeft the left value of the before domain
 * @param beforeRight the right value of the before domain
 * @param afterLeft the left value of the after domain
 * @param afterRight the right value of the after domain
 * @param value the value to map from the before domain to the after domain
 * @return a value in the after domain
 */
fun map(beforeLeft: Double, beforeRight: Double, afterLeft: Double, afterRight: Double, value: Double): Double {
    val n = (value - beforeLeft) / (beforeRight - beforeLeft)
    return afterLeft + n * (afterRight - afterLeft)
}

fun linearstep(edge0: Double, edge1: Double, x: Double): Double {
    return saturate((x - edge0) / (edge1 - edge0))
}

/**
 * Smoothstep
 * @param edge0
 * @param edge1
 * @param x
 * @return a mapped value in the interval [0, 1]
 */
fun smoothstep(edge0: Double, edge1: Double, x: Double): Double {
    // Scale, bias and saturate x to 0..1 range
    val u = saturate((x - edge0) / (edge1 - edge0))
    // Evaluate polynomial
    return u * u * (3 - 2 * u)
}

/**
 * Smoothstep
 * @param edge0
 * @param edge1
 * @param x
 * @return a mapped value in the interval [0, 1]
 */
fun smoothstepIn(edge0: Double, edge1: Double, x: Double): Double {

    // Scale, bias and saturate x to 0..1 range
    val u = saturate((x - edge0) / (edge1 - edge0))
    // Evaluate polynomial

    return if (x < 0.5)
        u * u * (3 - 2 * u)
    else {
        u
    }
}


/**
 * Smootherstep
 * @param edge0
 * @param edge1
 * @param x
 * @return a mapped value in the interval [0, 1]
 */
fun smootherstep(edge0: Double, edge1: Double, x: Double): Double {

    // Scale, bias and saturate x to 0..1 range
    val u = saturate((x - edge0) / (edge1 - edge0))
    // Evaluate polynomial
    return u * u * u * (u * (u * 6 - 15) + 10)
}

fun saturate(x: Double): Double {
    return Math.max(0.0, Math.min(1.0, x))
}


fun mix(left: Double, right: Double, x: Double): Double {
    return left * (1.0 - x) + right * x
}