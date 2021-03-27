package org.openrndr.math

import io.kotest.matchers.doubles.between
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.openrndr.math.test.it
import kotlin.test.Test

class TestQuaternion {

    @Test
    fun shouldDoOperationsOnAQuaternion() {

        it("IDENTITY times IDENTITY should result in IDENTITY") {
            val q0 = Quaternion.IDENTITY
            val q1 = Quaternion.IDENTITY
            val qm = q0 * q1

            qm.x shouldBe 0.0
            qm.y shouldBe 0.0
            qm.z shouldBe 0.0
            qm.w shouldBe 1.0
        }

        it ("matrix to quaternion to matrix") {
            val q0 = Quaternion.fromMatrix(Matrix33.IDENTITY)
            val m0 = q0.matrix
            m0 shouldBe Matrix33.IDENTITY
        }

        it ("quaternion look +Z") {
            val q0 = Quaternion.fromLookAt(Vector3.ZERO, Vector3(0.0, 0.0, 1.0), Vector3.UNIT_Y)
            val v0 = q0 * Vector3.UNIT_Z
            v0.x shouldBe between(-0.0001, 0.0001, 0.0)
            v0.y shouldBe between(-0.0001, 0.0001, 0.0)
            v0.z shouldBe between(1-0.0001, 1+0.0001, 0.0)
        }

        it ("quaternion look -Z") {
            val q0 = Quaternion.fromLookAt(Vector3.ZERO, Vector3(0.0, 0.0, -1.0), Vector3.UNIT_Y)
            val v0 = q0 * Vector3.UNIT_Z
            v0.x shouldBe between(-0.0001, 0.0001, 0.0)
            v0.y shouldBe between(-0.0001, 0.0001, 0.0)
            v0.z shouldBe between(-1-0.0001, -1+0.0001, 0.0)
        }

        it ("quaternion look +X") {
            val q0 = Quaternion.fromLookAt(Vector3.ZERO, Vector3(1.0, 0.0, 0.0), Vector3.UNIT_Y)
            val v0 = q0 * Vector3.UNIT_Z
            v0.x shouldBe between(1-0.0001, 1+0.0001, 0.0)
            v0.y shouldBe between(-0.0001, 0.0001, 0.0)
            v0.z shouldBe between(-0.0001, 0.0001, 0.0)
        }

        it ("quaternion look -X") {
            val q0 = Quaternion.fromLookAt(Vector3.ZERO, Vector3(-1.0, 0.0, 0.0), Vector3.UNIT_Y)
            val v0 = q0 * Vector3.UNIT_Z
            v0.x shouldBe between(-1-0.0001, -1+0.0001, 0.0)
            v0.y shouldBe between(-0.0001, 0.0001, 0.0)
            v0.z shouldBe between(-0.0001, 0.0001, 0.0)
        }

        it("quaternion.identity * vector3") {
            Quaternion.IDENTITY * Vector3.UNIT_X shouldBe Vector3.UNIT_X
            Quaternion.IDENTITY * Vector3.UNIT_Y shouldBe Vector3.UNIT_Y
            Quaternion.IDENTITY * Vector3.UNIT_Z shouldBe Vector3.UNIT_Z
        }
    }

    @Test
    fun shouldSerialize() {

        it("should serialize ZERO to JSON") {
            Json.encodeToString(Quaternion.ZERO) shouldMatch """\{"x":0(\.0)?,"y":0(\.0)?,"z":0(\.0)?,"w":0(\.0)?\}"""
        }

        it("should serialize IDENTITY to JSON") {
            Json.encodeToString(Quaternion.IDENTITY) shouldMatch """\{"x":0(\.0)?,"y":0(\.0)?,"z":0(\.0)?,"w":1(\.0)?\}"""
        }

    }

}
