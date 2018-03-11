package org.openrndr.math

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it

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