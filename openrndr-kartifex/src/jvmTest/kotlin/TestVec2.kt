import io.lacuna.artifex.Vec2
import kotlin.test.Test
import kotlin.test.assertEquals
import org.openrndr.kartifex.Vec2 as KVec2

class TestVec2 {
    @Test
    fun testHash() {
        run {
            val v2 = Vec2(0.0, 0.0)
            val kv2 = KVec2(0.0, 0.0)
            assertEquals(v2.hashCode(), kv2.hashCode())
        }
        run {
            val v2 = Vec2(2.0, 2.1231)
            val kv2 = KVec2(2.0, 2.1231)
            assertEquals(v2.hashCode(), kv2.hashCode())
        }
    }

    @Test
    fun testCompare() {
        val v2 = Vec2(0.0, 0.0)
        val kv2 = KVec2(0.0, 0.0)
        assertEquals(v2.compareTo(v2), kv2.compareTo(kv2))

        val v2a = Vec2(1.0, 0.0)
        val kv2a = KVec2(1.0, 0.0)

        assertEquals(v2a.compareTo(v2), kv2a.compareTo(kv2))
        assertEquals(v2.compareTo(v2a), kv2.compareTo(kv2a))
    }

    @Test
    fun testPseudoNorm() {
        val v2 = Vec2(2100.0, 3220.0)
        val pn = v2.pseudoNorm()

        val kv2 = KVec2(2100.0, 3220.0)
        val kpn = kv2.pseudoNorm()
        assertEquals(pn.x, kpn.x)
        assertEquals(pn.y, kpn.y)
    }
}