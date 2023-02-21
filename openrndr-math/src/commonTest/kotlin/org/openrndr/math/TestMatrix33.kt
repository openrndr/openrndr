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

        it("multiplication by a scalar") {
            val m = Matrix33(
                1.0, 2.0, 3.0,
                4.0, 5.0, 6.0,
                7.0, 8.0, 9.0
            )
            val expectedResult = Matrix33(
                2.0, 4.0, 6.0,
                8.0, 10.0, 12.0,
                14.0, 16.0, 18.0
            )

            val operationResult = m * 2.0
            operationResult.closeTo(expectedResult, maxError)
        }

        it("division by a scalar") {
            val m = Matrix33(
                1.0, 2.0, 3.0,
                4.0, 5.0, 6.0,
                7.0, 8.0, 9.0
            )
            val expectedResult = Matrix33(
                0.5, 1.0, 1.5,
                2.0, 2.5, 3.0,
                3.5, 4.0, 4.5
            )

            val operationResult = m / 2.0
            operationResult.closeTo(expectedResult, maxError)
        }
    }

}
