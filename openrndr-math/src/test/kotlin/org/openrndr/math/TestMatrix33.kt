package org.openrndr.math

import org.amshove.kluent.`should be in range`
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TestMatrix33 : Spek({

    val maxError = 0.0000001

    describe("Matrix33 Operations") {

        it("trace of identity should be 3") {
            Matrix33.IDENTITY.trace.`should be in range`(3.0-maxError,3.0+maxError)
        }

        it("trace of identity minus identity should be 0") {
            (Matrix33.IDENTITY - Matrix33.IDENTITY).trace.`should be in range`(0.0-maxError,0.0+maxError)
        }

        it("determinant of identity ") {
            Matrix33.IDENTITY.determinant.`should be in range`(1.0-maxError, 1.0+maxError)
        }

        it("determinant of collinear points should be 0 ") {
            Matrix33.fromColumnVectors(Vector3.UNIT_X, Vector3.UNIT_X*2.0, Vector3.UNIT_X*3.0 ).determinant.`should be in range`(0.0-maxError, 0.0+maxError)
        }

        it("inverse of identity") {
            val inversed = Matrix33.IDENTITY.inversed
            inversed.trace.`should be in range`(3.0-maxError,3.0+maxError)
            inversed.determinant.`should be in range`(1.0-maxError,1.0+maxError)
        }

        it("inverse of non-identity diagonal") {
            val d = Matrix33.IDENTITY * 3.0
            val inversed = d.inversed
            inversed.trace.`should be in range`(1.0-maxError,1.0+maxError)
        }
        it("inverse of non-diagonal") {
            val nd = Matrix33.fromColumnVectors(Vector3(1.0, 0.0, 0.0), Vector3(0.0, 1.0, 0.0), Vector3(1.0, 1.0, 1.0))
            val inversed = nd.inversed
            (nd * inversed).trace.`should be in range`(1.0-maxError,1.0+maxError)
        }
    }
})