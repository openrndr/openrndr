package org.openrndr.math

import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TestVector4 : Spek({

    val maxError = 0.0000001

    describe("Vector4 Operations") {

        it("should normalize 0 length") {
            Vector4.ZERO.normalized.closeTo(Vector4.ZERO, maxError)
        }

        it("should support 0 length") {
            Vector4.ZERO.length.closeTo(0.0, maxError)
        }
    }

    describe("Vector4 mix") {

        it("should .mix towards first component") {
            Vector4.ONE.mix(Vector4.ZERO, 0.0).closeTo(Vector4.ONE, maxError)
        }

        it("should .mix towards second component") {
            Vector4.ONE.mix(Vector4.ZERO, 1.0).closeTo(Vector4.ZERO, maxError)
        }

        it("should mix() towards first component") {
            mix(Vector4.ONE, Vector4.ZERO, 0.0).closeTo(Vector4.ONE, maxError)
        }

        it("should mix() towards second component") {
            mix(Vector4.ONE, Vector4.ZERO, 1.0).closeTo(Vector4.ZERO, maxError)
        }
    }
})