@file:JvmName("IntrinsicsCommon")
@file:Suppress("NOTHING_TO_INLINE")

package org.openrndr.math


import kotlin.jvm.JvmName

/**
 * Performs a fused multiply-add operation, which computes `(a * b) + c` with only one rounding step.
 * This provides better precision compared to performing the operations separately.
 *
 * @param a The first operand to be multiplied.
 * @param b The second operand to be multiplied.
 * @param c The operand to be added.
 * @return The result of `(a * b) + c` computed with higher precision.
 */
expect inline fun fma(a: Double, b: Double, c: Double): Double

/**
 * Computes a fused multiply-add operation and sums the results of two multiplications.
 * Specifically, it calculates `(a1 * b1 + a0 * b0)` in a way that ensures higher precision.
 *
 * @param a0 The first multiplicand in the second multiplication.
 * @param b0 The second multiplicand in the second multiplication.
 * @param a1 The first multiplicand in the first multiplication.
 * @param b1 The second multiplicand in the first multiplication.
 * @return The result of `(a1 * b1) + (a0 * b0)` computed with enhanced precision for the first product using fused multiply-add.
 */
inline fun fmaDot(a0: Double, b0: Double, a1: Double, b1: Double): Double {
    return fma(a1, b1, a0 * b0)
}

/**
 * Computes a fused multiply-add operation for two pairs of values and a constant offset.
 * This function calculates `a0 * b0 + c`, then uses the result as the offset for `a1 * b1`.
 * The `fma` operation ensures higher precision by performing the operations in a single
 * rounding step.
 *
 * @param a0 The first operand of the first multiplication.
 * @param b0 The second operand of the first multiplication.
 * @param a1 The first operand of the second multiplication.
 * @param b1 The second operand of the second multiplication.
 * @param c The constant to be added to the result of the first multiplication.
 * @return The result of the fused multiply-add operation for the two pairs of values and the constant offset.
 */
inline fun fmaDotC(a0: Double, b0: Double, a1: Double, b1: Double, c: Double): Double {
    val x = fma(a0, b0, c)
    return fma(a1, b1, x)
}

/**
 * Computes the dot product of two 3-dimensional vectors using fused multiply-add operations
 * to enhance precision. The vectors are represented as pairs of coordinates,
 * where the first vector's components are `(a0, a1, a2)` and the second vector's components
 * are `(b0, b1, b2)`.
 *
 * @param a0 The first component of the first vector.
 * @param b0 The first component of the second vector.
 * @param a1 The second component of the first vector.
 * @param b1 The second component of the second vector.
 * @param a2 The third component of the first vector.
 * @param b2 The third component of the second vector.
 * @return The computed dot product of the two vectors with improved precision due to fused multiply-add operations.
 */
inline fun fmaDot(a0: Double, b0: Double, a1: Double, b1: Double, a2: Double, b2: Double): Double {
    val x = fma(a1, b1, a0 * b0)
    return fma(a2, b2, x)
}

/**
 * Computes the dot product of three vector components combined with an additional constant term,
 * using fused multiply-add operations for improved precision.
 *
 * @param a0 The first component of the first vector.
 * @param b0 The first component of the second vector.
 * @param a1 The second component of the first vector.
 * @param b1 The second component of the second vector.
 * @param a2 The third component of the first vector.
 * @param b2 The third component of the second vector.
 * @param c The constant term to be added to the dot product.
 * @return The result of `a0 * b0 + a1 * b1 + a2 * b2 + c`, computed with higher precision using fused multiply-add operations.
 */
inline fun fmaDotC(a0: Double, b0: Double, a1: Double, b1: Double, a2: Double, b2: Double, c: Double): Double {
    var x = fma(a0, b0, c)
    x = fma(a1, b1, x)
    return fma(a2, b2, x)
}

/**
 * Computes the dot product of two 4-dimensional vectors using fused multiply-add operations
 * for enhanced precision. The result is calculated as:
 * `(a0 * b0) + (a1 * b1) + (a2 * b2) + (a3 * b3)` using fused operations.
 *
 * @param a0 The first component of the first vector.
 * @param b0 The first component of the second vector.
 * @param a1 The second component of the first vector.
 * @param b1 The second component of the second vector.
 * @param a2 The third component of the first vector.
 * @param b2 The third component of the second vector.
 * @param a3 The fourth component of the first vector.
 * @param b3 The fourth component of the second vector.
 * @return The dot product of the two vectors, computed using fused multiply-add operations.
 */
inline fun fmaDot(a0: Double, b0: Double, a1: Double, b1: Double, a2: Double, b2: Double, a3: Double, b3: Double): Double {
    var x = fma(a1, b1, a0 * b0)
    x = fma(a2, b2, x)
    return fma(a3, b3, x)
}

/**
 * Computes the dot product of four (a, b) pairs with an additional constant value `c`.
 * This operation is performed using fused multiply-add operations for improved precision.
 *
 * @param a0 The first operand of the first pair.
 * @param b0 The second operand of the first pair.
 * @param a1 The first operand of the second pair.
 * @param b1 The second operand of the second pair.
 * @param a2 The first operand of the third pair.
 * @param b2 The second operand of the third pair.
 * @param a3 The first operand of the fourth pair.
 * @param b3 The second operand of the fourth pair.
 * @param c The initial constant value to be added to the result.
 * @return The fused multiply-add result of the dot product calculation.
 */
inline fun fmaDotC(a0: Double, b0: Double, a1: Double, b1: Double, a2: Double, b2: Double, a3: Double, b3: Double, c: Double): Double {
    var x = fma(a0, b0, c)
    x = fma(a1, b1, x)
    x = fma(a2, b2, x)
    return fma(a3, b3, x)
}

/**
 * Computes the dot product of three 3-dimensional vectors, combined with fused-multiply-add (FMA)
 * operations for increased precision by avoiding intermediate rounding errors.
 *
 * The formula applies FMA repeatedly for the components of the vectors:
 * result = fma(a0, b0 * c0, fma(a1, b1 * c1, ... fma(a5, b5 * c5, 0.0) ...))
 *
 * @param a0 The first component of the first vector.
 * @param b0 The first component of the second vector.
 * @param c0 The first component of the third vector.
 * @param a1 The second component of the first vector.
 * @param b1 The second component of the second vector.
 * @param c1 The second component of the third vector.
 * @param a2 The third component of the first vector.
 * @param b2 The third component of the second vector.
 * @param c2 The third component of the third vector.
 * @param a3 The fourth component of the first vector.
 * @param b3 The fourth component of the second vector.
 * @param c3 The fourth component of the third vector.
 * @param a4 The fifth component of the first vector.
 * @param b4 The fifth component of the second vector.
 * @param c4 The fifth component of the third vector.
 * @param a5 The sixth component of the first vector.
 * @param b5 The sixth component of the second vector.
 * @param c5 The sixth component of the third vector.
 * @return The computed dot product of the vectors using fused-multiply-add precision.
 */
inline fun fmaDot3(a0: Double, b0: Double, c0: Double,
                   a1: Double, b1: Double, c1: Double,
                   a2: Double, b2: Double, c2: Double,
                   a3: Double, b3: Double, c3: Double,
                   a4: Double, b4: Double, c4: Double,
                   a5: Double, b5: Double, c5: Double,

): Double {
    var x = fma(a1, b1 * c1, a0 * b0 * c0)
    x = fma(a2, b2 * c2, x)
    x = fma(a3, b3 * c3, x)
    x = fma(a4, b4 * c4, x)
    return fma(a5, b5 * c5, x)
}

/**
 * Computes a fused multiply-add dot product operation across four sets of inputs.
 * Each set performs multiplications, computes fused additions, and sums them into a single result,
 * ensuring improved precision through the usage of fused multiply-add operations.
 *
 * @param a0 The first factor of the first term.
 * @param b0 The second factor of the first term.
 * @param c0 The third factor of the first term.
 * @param d0 The fourth factor of the first term.
 * @param a1 The first factor of the second term.
 * @param b1 The second factor of the second term.
 * @param c1 The third factor of the second term.
 * @param d1 The fourth factor of the second term.
 * @param a2 The first factor of the third term.
 * @param b2 The second factor of the third term.
 * @param c2 The third factor of the third term.
 * @param d2 The fourth factor of the third term.
 * @param a3 The first factor of the fourth term.
 * @param b3 The second factor of the fourth term.
 * @param c3 The third factor of the fourth term.
 * @param d3 The fourth factor of the fourth term.
 * @return The result of the fused multiply-add dot product calculation.
 */
inline fun fmaDot4(a0: Double, b0: Double, c0: Double, d0: Double,
                   a1: Double, b1: Double, c1: Double, d1: Double,
                   a2: Double, b2: Double, c2: Double, d2: Double,
                   a3: Double, b3: Double, c3: Double, d3: Double,

                   ): Double {
    var x = fma(a1 * b1, c1 * d1, a0 * b0 * c0 * d0)
    x = fma(a2 * b2, c2 * d2, x)
    return fma(a3 * b3, c3 * d3, x)
}
