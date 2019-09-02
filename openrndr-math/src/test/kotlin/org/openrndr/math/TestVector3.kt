package org.openrndr.math

import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TestVector3 : Spek({

    val maxError = 0.0000001

    describe("Vector3 Operations") {

        it("should normalize 0 length") {
            Vector3.ZERO.normalized.closeTo(Vector3.ZERO, maxError)
        }

        it("should support 0 length") {
            Vector3.ZERO.length.closeTo(0.0, maxError)
        }
    }
})