import org.openrndr.animatable.Animatable
import org.openrndr.math.Quaternion
import kotlin.test.Test

class TestQuaternionAnimatable {
    @Test
    fun testQuaternionAnimatable() {
        class A : Animatable() {
            var q = Quaternion.IDENTITY
        }

        val a = A()
        a.updateAnimation()
        a.apply {
            ::q.animate(Quaternion.fromAngles(20.0, 50.0, 40.0), 1000)
        }
    }
}