package org.openrndr.math

import org.amshove.kluent.`should be in range`
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

        it ("matrix to quaternion to matrix") {
            val q0 = Quaternion.fromMatrix(Matrix33.IDENTITY)
            val m0 = q0.matrix
            m0 `should equal` Matrix33.IDENTITY
        }

        it ("quaternion look +Z") {
            val q0 = Quaternion.fromLookAt(Vector3.ZERO, Vector3(0.0, 0.0, 1.0), Vector3.UNIT_Y)
            val v0 = q0 * Vector3.UNIT_Z
            v0.x.`should be in range` (-0.0001, 0.0001)
            v0.y.`should be in range` (-0.0001, 0.0001)
            v0.z.`should be in range` (1-0.0001,1+ 0.0001)
        }

        it ("quaternion look -Z") {
            val q0 = Quaternion.fromLookAt(Vector3.ZERO, Vector3(0.0, 0.0, -1.0), Vector3.UNIT_Y)
            val v0 = q0 * Vector3.UNIT_Z
            v0.x.`should be in range` (-0.0001, 0.0001)
            v0.y.`should be in range` (-0.0001, 0.0001)
            v0.z.`should be in range` (-1-0.0001,-1+ 0.0001)
        }

        it ("quaternion look +X") {
            val q0 = Quaternion.fromLookAt(Vector3.ZERO, Vector3(1.0, 0.0, 0.0), Vector3.UNIT_Y)
            val v0 = q0 * Vector3.UNIT_Z
            v0.x.`should be in range` (1-0.0001,1+ 0.0001)
            v0.y.`should be in range` (-0.0001, 0.0001)
            v0.z.`should be in range` (-0.0001, 0.0001)
        }

        it ("quaternion look -X") {
            val q0 = Quaternion.fromLookAt(Vector3.ZERO, Vector3(-1.0, 0.0, 0.0), Vector3.UNIT_Y)
            val v0 = q0 * Vector3.UNIT_Z
            v0.x.`should be in range` (-1-0.0001,-1+ 0.0001)
            v0.y.`should be in range` (-0.0001, 0.0001)
            v0.z.`should be in range` (-0.0001, 0.0001)
        }

        it("quaternion.identity * vector3") {
            Quaternion.IDENTITY * Vector3.UNIT_X `should equal` Vector3.UNIT_X
            Quaternion.IDENTITY * Vector3.UNIT_Y `should equal` Vector3.UNIT_Y
            Quaternion.IDENTITY * Vector3.UNIT_Z `should equal` Vector3.UNIT_Z
        }
    }
})