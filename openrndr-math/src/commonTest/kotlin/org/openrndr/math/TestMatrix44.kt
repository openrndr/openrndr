package org.openrndr.math

import org.openrndr.math.test.it
import kotlin.test.Test

class TestMatrix44 {

    private val maxError = 0.0000001

    @Test
    fun doMatrix44Operations() {

        it("multiplication by a scalar") {
            val m = Matrix44(
                1.0, 2.0, 3.0, 4.0,
                5.0, 6.0, 7.0, 8.0,
                9.0, 10.0, 11.0, 12.0,
                13.0, 14.0, 15.0, 16.0
            )
            val expectedResult = Matrix44(
                2.0, 4.0, 6.0, 8.0,
                10.0, 12.0, 14.0, 16.0,
                18.0, 20.0, 22.0, 24.0,
                26.0, 28.0, 30.0, 32.0
            )

            val operationResult = m * 2.0
            operationResult.closeTo(expectedResult, maxError)
        }

        it("division by a scalar") {
            val m = Matrix44(
                1.0, 2.0, 3.0, 4.0,
                5.0, 6.0, 7.0, 8.0,
                9.0, 10.0, 11.0, 12.0,
                13.0, 14.0, 15.0, 16.0
            )
            val expectedResult = Matrix44(
                0.5, 1.0, 1.5, 2.0,
                2.5, 3.0, 3.5, 4.0,
                4.5, 5.0, 5.5, 6.0,
                6.5, 7.0, 7.5, 8.0
            )

            val operationResult = m / 2.0
            operationResult.closeTo(expectedResult, maxError)
        }
    }

}
