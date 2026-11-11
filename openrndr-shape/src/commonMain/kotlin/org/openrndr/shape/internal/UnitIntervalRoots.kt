package org.openrndr.shape.internal

import kotlin.math.abs

/**
 * Find the roots in [0, 1] of the polynomial with the given [coefficients], lowest degree first.
 *
 * The polynomial is written in the Bernstein basis, whose coefficients bound it on an interval: an interval whose
 * coefficients have no sign change holds no root, one with a single sign change holds exactly one. Intervals with more
 * sign changes are halved until each root is isolated, the isolated roots are found with Newton steps that stay inside
 * their interval. Roots that lie closer together than about 2^-40 are reported once.
 */
internal fun unitIntervalRoots(coefficients: DoubleArray): List<Double> {
    val n = coefficients.size - 1
    val roots = mutableListOf<Double>()
    if (n < 1) return roots

    // b_i = sum over j <= i of C(i, j) / C(n, j) * c_j
    val bernstein = DoubleArray(n + 1)
    for (i in 0..n) {
        var sum = 0.0
        var cij = 1.0 // C(i, j)
        var cnj = 1.0 // C(n, j)
        for (j in 0..i) {
            sum += cij / cnj * coefficients[j]
            cij = cij * (i - j) / (j + 1)
            cnj = cnj * (n - j) / (j + 1)
        }
        bernstein[i] = sum
    }
    isolate(coefficients, bernstein, 0.0, 1.0, 0, roots)
    return roots
}

private fun signChanges(b: DoubleArray): Int {
    var changes = 0
    var previous = 0.0
    for (v in b) {
        if (v != 0.0) {
            if (previous != 0.0 && (v > 0.0) != (previous > 0.0)) changes++
            previous = v
        }
    }
    return changes
}

private fun isolate(coefficients: DoubleArray, b: DoubleArray, t0: Double, t1: Double, depth: Int, roots: MutableList<Double>) {
    val changes = signChanges(b)
    if (changes == 0) return
    if (changes == 1 || depth == 40) {
        roots.add(refine(coefficients, t0, t1))
        return
    }
    // split the Bernstein coefficients at the middle of the interval with de Casteljau's algorithm
    val n = b.size - 1
    val left = DoubleArray(n + 1)
    val right = DoubleArray(n + 1)
    val w = b.copyOf()
    left[0] = w[0]
    right[n] = w[n]
    for (k in 1..n) {
        for (i in 0..n - k) {
            w[i] = (w[i] + w[i + 1]) * 0.5
        }
        left[k] = w[0]
        right[n - k] = w[n - k]
    }
    val tm = (t0 + t1) * 0.5
    isolate(coefficients, left, t0, tm, depth + 1, roots)
    isolate(coefficients, right, tm, t1, depth + 1, roots)
}

private fun evaluate(coefficients: DoubleArray, t: Double): Double {
    var value = 0.0
    for (i in coefficients.indices.reversed()) {
        value = value * t + coefficients[i]
    }
    return value
}

private fun refine(coefficients: DoubleArray, t0: Double, t1: Double): Double {
    var lo = t0
    var hi = t1
    val fLo = evaluate(coefficients, lo)
    if (fLo == 0.0) return lo
    val fHi = evaluate(coefficients, hi)
    if (fHi == 0.0) return hi
    if ((fLo > 0.0) == (fHi > 0.0)) return (lo + hi) * 0.5
    val loPositive = fLo > 0.0
    // Newton steps that stay inside the bracket, halving it when a step would leave it
    var t = lo - fLo * (hi - lo) / (fHi - fLo)
    while (true) {
        var f = 0.0
        var df = 0.0
        for (i in coefficients.indices.reversed()) {
            df = df * t + f
            f = f * t + coefficients[i]
        }
        if (f == 0.0) return t
        if ((f > 0.0) == loPositive) lo = t else hi = t
        if (hi - lo <= 1e-15) return t
        var next = t - f / df
        if (!(next > lo && next < hi)) next = (lo + hi) * 0.5
        if (abs(next - t) <= 1e-15) return next
        t = next
    }
}
