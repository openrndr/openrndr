import io.lacuna.artifex.Vec4
import kotlin.test.Test
import kotlin.test.assertEquals
import org.openrndr.kartifex.Vec4 as KVec4

class TestVec4 {
    @Test
    fun testHash() {
        run {
            val v4 = Vec4(0.0, 0.0, 0.0, 0.0)
            val kv4 = KVec4(0.0, 0.0, 0.0, 0.0)
            assertEquals(v4.hashCode(), kv4.hashCode())
        }
        run {
            val v4 = Vec4(2.0, 2.1231, 1.3293, 4.231)
            val kv4 = KVec4(2.0, 2.1231, 1.3293, 4.231)
            assertEquals(v4.hashCode(), kv4.hashCode())
        }
    }
}