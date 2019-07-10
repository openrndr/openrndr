package org.openrndr.math

import org.amshove.kluent.`should be in range`
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.openrndr.math.Matrix33

object TestMatrix33 : Spek({

    val maxError = 0.0000001

    describe("Matrix33 Operations") {

        it("trace of identity should be 3") {
            Matrix33.IDENTITY.trace.`should be in range`(3.0-maxError,3.0+maxError)
        }

        it("trace of identity minus identity should be 0") {
            (Matrix33.IDENTITY - Matrix33.IDENTITY).trace.`should be in range`(0.0-maxError,0.0+maxError)
        }

    }
})