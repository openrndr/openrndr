package org.openrndr.math

import org.openrndr.math.test.Equations
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class TestEquations {

    @Test
    fun shouldSolveAQuadraticEquation() {
        val a = 2.0
        val b = 4.0
        val c = 1.0
        solveQuadratic(a, b, c) shouldBe Equations.solveQuadratic(a, b, c)
    }

    @Test
    fun shouldSolveACubicEquation() {
        val a = 2.0
        val b = 4.0
        val c = 1.0
        val d = 3.0
        solveCubic(a, b, c, d) shouldBe Equations.solveCubic(a, b, c, d)
    }

}
