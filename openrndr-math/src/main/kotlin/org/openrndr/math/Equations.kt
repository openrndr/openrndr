package org.openrndr.math

private const val DISCRIMINANT_EPSILON = 1e-10
private const val SOLUTION_EPSILON = 1e-8
private val MACHINE_EPSILON = Math.ulp(1.0)
private const val EPSILON = 1e-14

// adapted from https://github.com/paperjs/paper.js/blob/develop/src/util/Numerical.js
// converted from Artifex Equations.java
private fun trim(acc: DoubleArray, len: Int): DoubleArray {
    return if (len == acc.size) {
        acc
    } else if (len == 0) {
        DoubleArray(0)
    } else {
        val result = DoubleArray(len)
        System.arraycopy(acc, 0, result, 0, len)
        result
    }
}

private fun split(n: Double): DoubleArray {
    val x = n * 134217729
    val y = n - x
    val hi = y + x
    val lo = n - hi
    return doubleArrayOf(hi, lo)
}

private fun discriminant(a: Double, b: Double, c: Double): Double {
    var D = b * b - a * c
    val E = b * b + a * c
    if (StrictMath.abs(D) * 3 < E) {
        val ad: DoubleArray = split(a)
        val bd: DoubleArray = split(b)
        val cd: DoubleArray = split(c)
        val p = b * b
        val dp = bd[0] * bd[0] - p + 2 * bd[0] * bd[1] + bd[1] * bd[1]
        val q = a * c
        val dq = ad[0] * cd[0] - q + ad[0] * cd[1] + ad[1] * cd[0] + ad[1] * cd[1]
        D = p - q + (dp - dq)
    }
    return D
}

fun solveLinear(a: Double, b: Double, acc: DoubleArray): Int {
    return if (StrictMath.abs(a) < EPSILON) {
        0
    } else {
        acc[0] = -b / a
        1
    }
}

fun solveLinear(a: Double, b: Double): DoubleArray {
    val acc = DoubleArray(1)
    return trim(acc, solveLinear(a, b, acc))
}

fun solveQuadratic(a: Double, b: Double, c: Double, acc: DoubleArray): Int {
    var a = a
    var b = b
    var c = c
    if (StrictMath.abs(a) < EPSILON) {
        return solveLinear(b, c, acc)
    }
    b *= -0.5
    val k: Double = normalizationFactor(a, b, c)
    a *= k
    b *= k
    c *= k
    val D = discriminant(a, b, c)
    return if (D >= -DISCRIMINANT_EPSILON) {
        val Q: Double = if (D < 0) 0.0 else StrictMath.sqrt(D)
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
            if (StrictMath.abs(y) < SOLUTION_EPSILON) {
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

fun solveCubic(a: Double, b: Double, c: Double, d: Double, acc: DoubleArray): Int {
    var a = a
    var b = b
    var c = c
    var d = d
    val k: Double = normalizationFactor(a, b, c, d)
    a *= k
    b *= k
    c *= k
    d *= k
    var x: Double
    var b1: Double
    var c2: Double
    var qd: Double
    var q: Double
    if (StrictMath.abs(a) < EPSILON) {
        return solveQuadratic(b, c, d, acc)
    } else if (StrictMath.abs(d) < EPSILON) {
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
        val r = StrictMath.pow(StrictMath.abs(t), 1 / 3.0)
        val s = if (t < 0) (-1).toDouble() else 1.toDouble()
        val td = -qd / a
        val rd = if (td > 0) 1.324717957244746 * StrictMath.max(r, StrictMath.sqrt(td)) else r
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
            if (StrictMath.abs(a) * x * x > StrictMath.abs(d / x)) {
                c2 = -d / x
                b1 = (c2 - c) / x
            }
        }
    }
    var solutions: Int = solveQuadratic(a, b1, c2, acc)
    for (i in 0 until solutions) {
        if (acc[i] == x) {
            return solutions
        }
    }
    val y = a * x * x * x + b * x * x + c * x + d
    if (StrictMath.abs(y) < SOLUTION_EPSILON) {
        acc[solutions++] = x
    }
    return solutions
}

fun solveCubic(a: Double, b: Double, c: Double, d: Double): DoubleArray {
    val acc = DoubleArray(3)
    return trim(acc, solveCubic(a, b, c, d, acc))
}