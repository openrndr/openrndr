@file:Suppress("unused")

package org.openrndr.math

import kotlin.math.max
import kotlin.math.min

fun mod(a: Double, b: Double) = ((a % b) + b) % b
fun mod(a: Int, b: Int) = ((a % b) + b) % b
fun mod(a: Float, b: Float) = ((a % b) + b) % b
fun mod(a: Long, b: Long) = ((a % b) + b) % b

fun clamp(value: Double, min: Double, max: Double) = max(min, min(max, value))
