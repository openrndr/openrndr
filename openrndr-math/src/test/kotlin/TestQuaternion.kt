import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.openrndr.math.Quaternion
import org.openrndr.math.fromAngles

object TestQuaternion : Spek({

    describe("a quaternion") {
        val q = Quaternion.IDENTITY


        it("should result in an identity quaternion") {
            val q0 = Quaternion.IDENTITY
            val q1= Quaternion.IDENTITY

            val qm = q0 * q1
            assert(qm.x == 0.0)
            assert(qm.y == 0.0)
            assert(qm.z == 0.0)
            assert(qm.w == 1.0)

            println(qm.matrix)

        }

        it("should behave nice") {
            val q0 = fromAngles(Math.PI,0.0, 0.0)
            println("q: ${q0.x} ${q0.y} ${q0.z} ${q0.w}")
            println(q0.matrix)


        }

    }
})