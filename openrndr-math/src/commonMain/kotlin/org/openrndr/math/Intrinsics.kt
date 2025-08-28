package org.openrndr.math

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