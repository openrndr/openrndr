package org.openrndr.math

import org.amshove.kluent.`should equal`
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it

object TestQuaternion : Spek({

    describe("a quaternion") {

        it("IDENTITY times IDENTITY should result in IDENTITY") {
            val q0 = Quaternion.IDENTITY
            val q1 = Quaternion.IDENTITY

            val qm = q0 * q1

            qm.x `should equal` 0.0
            qm.y `should equal` 0.0
            qm.z `should equal` 0.0
            qm.w `should equal` 1.0
        }

        it("should behave nice") {
            //            val q0 = fromAngles(Math.PI,0.0, 0.0)
//            println("q: ${q0.x} ${q0.y} ${q0.z} ${q0.w}")
//            println(q0.matrix)
        }

    }
})