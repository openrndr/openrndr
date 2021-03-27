package org.openrndr.math

import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import org.openrndr.math.test.it
import kotlin.test.Test

class TestMatrix33 {

    private val maxError = 0.0000001

    @Test
    fun doMatrix33Operations() {

        it("trace of identity should be 3") {
            Matrix33.IDENTITY.trace shouldBe (3.0 plusOrMinus maxError)
        }

        it("trace of identity minus identity should be 0") {
            (Matrix33.IDENTITY - Matrix33.IDENTITY).trace shouldBe (0.0 plusOrMinus maxError)
        }

        it("determinant of identity") {
            Matrix33.IDENTITY.determinant shouldBe (1.0 plusOrMinus maxError)
        }

        it("determinant of collinear points should be 0") {
            Matrix33.fromColumnVectors(
                Vector3.UNIT_X, Vector3.UNIT_X * 2.0, Vector3.UNIT_X * 3.0
            ).determinant shouldBe (0.0 plusOrMinus maxError)
        }

        it("inverse of identity") {
            val inversed = Matrix33.IDENTITY.inversed
            inversed.trace shouldBe (3.0 plusOrMinus maxError)
            inversed.determinant shouldBe (1.0 plusOrMinus maxError)
        }

        it("inverse of non-identity diagonal") {
            val d = Matrix33.IDENTITY * 3.0
            val inversed = d.inversed
            inversed.trace shouldBe (1.0 plusOrMinus maxError)
        }
        it("inverse of non-diagonal") {
            val nd = Matrix33.fromColumnVectors(
                Vector3(1.0, 0.0, 0.0),
                Vector3(0.0, 1.0, 0.0),
                Vector3(1.0, 1.0, 1.0)
            )
            val inversed = nd.inversed
            (nd * inversed).trace shouldBe (3.0 plusOrMinus maxError)
        }
    }

}
