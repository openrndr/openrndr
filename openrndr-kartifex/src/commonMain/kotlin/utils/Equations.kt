package org.openrndr.kartifex.utils

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

private const val DISCRIMINANT_EPSILON = 1e-10
private const val SOLUTION_EPSILON = 1e-8

/**
 * Trims a given array of doubles (`acc`) to the specified length (`len`).
 *
 * If the specified length matches the size of the input array,
 * the original array is returned. If the specified length is zero,
 * an empty array is returned. Otherwise, a new array with the specified
 * length containing the initial elements of the input array is returned.
 *
 * @param acc The input array of doubles to be trimmed.
 * @param len The desired length of the resulting trimmed array.
 * @return A trimmed array of doubles with the specified length.
 */
// adapted from https://github.com/paperjs/paper.js/blob/develop/src/util/Numerical.js
private fun trim(acc: DoubleArray, len: Int): DoubleArray {
    return when (len) {
        acc.size -> {
            acc
        }

        0 -> {
            DoubleArray(0)
        }

        else -> {
            val result = DoubleArray(len)
            //java.lang.System.arraycopy(acc, 0, result, 0, len)
            acc.copyInto(result, 0, 0, len)
            result
        }
    }
}

/**
 * Splits the input double into two components: a higher-order part and a lower-order part.
 * The split is performed in a way that the sum of these two parts equals the original number
 * and minimizes precision loss.
 *
 * @param n the double value to split
 * @return a DoubleArray where the first element is the higher-order part and the second element is the lower-order part
 */
private fun split(n: Double): DoubleArray {
    val x = n * 134217729
    val y = n - x
    val hi = y + x
    val lo = n - hi
    return doubleArrayOf(hi, lo)
}

/**
 * Calculates the discriminant of a quadratic equation with coefficients `a`, `b`, and `c`.
 * The discriminant determines the nature of the roots of the quadratic equation.
 *
 * @param a the coefficient of the quadratic term (x^2)
 * @param b the coefficient of the linear term (x)
 * @param c the constant term
 * @return the computed discriminant value
 */
private fun discriminant(a: Double, b: Double, c: Double): Double {
    var D = b * b - a * c
    val E = b * b + a * c
    if (abs(D) * 3 < E) {
        val ad = split(a)
        val bd = split(b)
        val cd = split(c)
        val p = b * b
        val dp = bd[0] * bd[0] - p + 2 * bd[0] * bd[1] + bd[1] * bd[1]
        val q = a * c
        val dq = ad[0] * cd[0] - q + ad[0] * cd[1] + ad[1] * cd[0] + ad[1] * cd[1]
        D = p - q + (dp - dq)
    }
    return D
}

/**
 * Solves a linear equation of the form ax + b = 0 and stores the solution in the provided array.
 *
 * @param a the coefficient of x in the equation.
 * @param b the constant term in the equation.
 * @param acc an array where the solution will be stored if it exists. The solution will be stored at index 0.
 * @return the number of solutions: 0 if there are no solutions, or 1 if there is exactly one solution.
 */
fun solveLinear(a: Double, b: Double, acc: DoubleArray): Int {
    return if (abs(a) < SCALAR_EPSILON) {
        0
    } else {
        acc[0] = -b / a
        1
    }
}

/**
 * Solves a linear equation of the form ax + b = 0 and returns the solution(s) as an array of doubles.
 *
 * @param a the coefficient of x in the equation.
 * @param b the constant term in the equation.
 * @return a double array containing the solution(s) to the linear equation. The array will be empty if there are no solutions,
 *         or it will contain one solution if a valid solution exists.
 */
fun solveLinear(a: Double, b: Double): DoubleArray {
    val acc = DoubleArray(1)
    return trim(acc, solveLinear(a, b, acc))
}

@Suppress("NAME_SHADOWING")
fun solveQuadratic(a: Double, b: Double, c: Double, acc: DoubleArray): Int {
    var a = a
    var b = b
    var c = c
    if (abs(a) < SCALAR_EPSILON) {
        return solveLinear(b, c, acc)
    }
    b *= -0.5
    val k = normalizationFactor(a, b, c)
    a *= k
    b *= k
    c *= k
    val D = discriminant(a, b, c)
    return if (D >= -DISCRIMINANT_EPSILON) {
        val Q: Double = if (D < 0) 0.0 else sqrt(D)
        val R = b + if (b < 0) -Q else Q
        if (R == 0.0) {
            acc[0] = c / a
            acc[1] = -c / a
        } else {
            acc[0] = R / a
            acc[1] = c / R
        }
        var writeIdx = 0
        for (readIdx in 0..1) {
            val x = acc[readIdx]

            // since the tolerance for the discriminant is fairly large, we check our work
            val y = a * x * x + -2 * b * x + c
            if (abs(y) < SOLUTION_EPSILON) {
                acc[writeIdx++] = x
            }
        }
        writeIdx
    } else {
        0
    }
}

fun solveQuadratic(a: Double, b: Double, c: Double): DoubleArray {
    val acc = DoubleArray(2)
    return trim(acc, solveQuadratic(a, b, c, acc))
}

@Suppress("NAME_SHADOWING")
fun solveCubic(a: Double, b: Double, c: Double, d: Double, acc: DoubleArray): Int {
    var a = a
    var b = b
    var c = c
    var d = d
    val k = normalizationFactor(a, b, c, d)
    a *= k
    b *= k
    c *= k
    d *= k
    var x: Double
    var b1: Double
    var c2: Double
    var qd: Double
    var q: Double
    if (abs(a) < SCALAR_EPSILON) {
        return solveQuadratic(b, c, d, acc)
    } else if (abs(d) < SCALAR_EPSILON) {
        b1 = b
        c2 = c
        x = 0.0
    } else {
        x = -(b / a) / 3
        b1 = a * x + b
        c2 = b1 * x + c
        qd = (a * x + b1) * x + c2
        q = c2 * x + d
        val t = q / a
        val r: Double = abs(t).pow(1 / 3.0)
        val s: Double = if (t < 0) -1.0 else 1.0
        val td = -qd / a
        val rd = if (td > 0) 1.324717957244746 * max(r, sqrt(td)) else r
        var x0 = x - s * rd
        if (x0 != x) {
            do {
                x = x0
                b1 = a * x + b
                c2 = b1 * x + c
                qd = (a * x + b1) * x + c2
                q = c2 * x + d
                x0 = if (qd == 0.0) x else x - q / (qd / (1 + MACHINE_EPSILON))
            } while (s * x0 > s * x)
            if (abs(a) * x * x > abs(d / x)) {
                c2 = -d / x
                b1 = (c2 - c) / x
            }
        }
    }
    var solutions = solveQuadratic(a, b1, c2, acc)
    for (i in 0 until solutions) {
        if (acc[i] == x) {
            return solutions
        }
    }
    val y = a * x * x * x + b * x * x + c * x + d
    if (abs(y) < SOLUTION_EPSILON) {
        acc[solutions++] = x
    }
    return solutions
}

/**
 * Solves a cubic equation of the form ax^3 + bx^2 + cx + d = 0 and returns its real roots.
 *
 * @param a Coefficient of the cubic term (x^3).
 * @param b Coefficient of the quadratic term (x^2).
 * @param c Coefficient of the linear term (x).
 * @param d Constant term.
 * @return A DoubleArray containing the real roots of the cubic equation. The number of roots
 *         in the array will depend on the nature of the cubic equation.
 */
fun solveCubic(a: Double, b: Double, c: Double, d: Double): DoubleArray {
    val acc = DoubleArray(3)
    return trim(acc, solveCubic(a, b, c, d, acc))
}



