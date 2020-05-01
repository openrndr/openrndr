package org.openrndr.math

import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TestVector2 : Spek({

    val maxError = 0.0000001

    describe("Vector2 Operations") {

        it("should normalize 0 length") {
            Vector2.ZERO.normalized.closeTo(Vector2.ZERO, maxError)
        }

        it("should support 0 length") {
            Vector2.ZERO.length.closeTo(0.0, maxError)
        }
    }

    describe("Vector2 mix") {

        it("should .mix towards first component") {
            Vector2.ONE.mix(Vector2.ZERO, 0.0).closeTo(Vector2.ONE, maxError)
        }

        it("should .mix towards second component") {
            Vector2.ONE.mix(Vector2.ZERO, 1.0).closeTo(Vector2.ZERO, maxError)
        }

        it("should mix() towards first component") {
            mix(Vector2.ONE, Vector2.ZERO, 0.0).closeTo(Vector2.ONE, maxError)
        }

        it("should mix() towards second component") {
            mix(Vector2.ONE, Vector2.ZERO, 1.0).closeTo(Vector2.ZERO, maxError)
        }
    }
})