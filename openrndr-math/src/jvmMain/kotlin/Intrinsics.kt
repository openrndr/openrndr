package org.openrndr.math

import java.lang.Math.fma as fma_


actual inline fun fma(a: Double, b: Double, c: Double): Double = fma_(a, b, c)