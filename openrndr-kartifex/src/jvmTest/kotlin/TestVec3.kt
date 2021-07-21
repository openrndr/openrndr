import io.kotest.matchers.types.shouldHaveSameHashCodeAs
import io.lacuna.artifex.Vec3
import kotlin.test.Test
import org.openrndr.kartifex.Vec3 as KVec3

class TestVec3 {
    @Test
    fun testHash() {
        run {
            val v3 = Vec3(0.0, 0.0, 0.0)
            val kv3 = KVec3(0.0, 0.0, 0.0)
            v3.shouldHaveSameHashCodeAs(kv3)
        }
        run {
            val v3 = Vec3(2.0, 2.1231,1.3293)
            val kv3 = KVec3(2.0, 2.1231,1.3293)
            v3.shouldHaveSameHashCodeAs(kv3)
        }
    }
}