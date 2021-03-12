package org.openrndr.math

import io.lacuna.artifex.utils.Equations
import org.amshove.kluent.`should be equal to`
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TestEquations : Spek({

    describe("a quadratic equation") {
        val a = 2.0
        val b = 4.0
        val c = 1.0
        val solutions = Equations.solveQuadratic(a, b, c)
        val solutions2 = solveQuadratic(a, b, c)
        solutions `should be equal to` solutions2
    }

    describe("a cubic equation") {
        val a = 2.0
        val b = 4.0
        val c = 1.0
        val d = 3.0
        val solutions = Equations.solveCubic(a, b, c, d)
        val solutions2 = solveCubic(a, b, c, d)
        solutions `should be equal to` solutions2
    }

})