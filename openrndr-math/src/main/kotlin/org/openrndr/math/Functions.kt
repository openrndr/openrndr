@file:Suppress("unused")

package org.openrndr.math

fun mod(a: Double, b: Double) = ((a % b) + b) % b
fun mod(a: Int, b: Int) = ((a % b) + b) % b
fun mod(a: Float, b: Float) = ((a % b) + b) % b
fun mod(a: Long, b: Long) = ((a % b) + b) % b

fun clamp(value: Double, min: Double, max: Double) = Math.max(min, Math.min(max, value))
