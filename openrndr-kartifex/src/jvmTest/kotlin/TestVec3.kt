import io.lacuna.artifex.Vec3
import kotlin.test.Test
import kotlin.test.assertEquals
import org.openrndr.kartifex.Vec3 as KVec3

class TestVec3 {
    @Test
    fun testHash() {
        run {
            val v3 = Vec3(0.0, 0.0, 0.0)
            val kv3 = KVec3(0.0, 0.0, 0.0)
            assertEquals(v3.hashCode(), kv3.hashCode())
        }
        run {
            val v3 = Vec3(2.0, 2.1231, 1.3293)
            val kv3 = KVec3(2.0, 2.1231, 1.3293)
            assertEquals(v3.hashCode(), kv3.hashCode())
        }
    }
}