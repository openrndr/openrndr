import io.kotest.matchers.doubles.shouldBeExactly
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.types.shouldHaveSameHashCodeAs
import io.lacuna.artifex.Vec2
import kotlin.test.Test
import org.openrndr.kartifex.Vec2 as KVec2

class TestVec2 {
    @Test
    fun testHash() {
        run {
            val v2 = Vec2(0.0, 0.0)
            val kv2 = KVec2(0.0, 0.0)
            v2.shouldHaveSameHashCodeAs(kv2)
        }
        run {
            val v2 = Vec2(2.0, 2.1231)
            val kv2 = KVec2(2.0, 2.1231)
            v2.shouldHaveSameHashCodeAs(kv2)
        }
    }

    @Test
    fun testCompare() {
        val v2 = Vec2(0.0, 0.0)
        val kv2 = KVec2(0.0, 0.0)
        v2.compareTo(v2).shouldBeExactly(kv2.compareTo(kv2))

        val v2a = Vec2(1.0, 0.0)
        val kv2a = KVec2(1.0, 0.0)

        v2a.compareTo(v2).shouldBeExactly(kv2a.compareTo(kv2))
        v2.compareTo(v2a).shouldBeExactly(kv2.compareTo(kv2a))
    }

    @Test
    fun testPseudoNorm() {
        val v2 = Vec2(2100.0, 3220.0)
        val pn = v2.pseudoNorm()

        val kv2 = KVec2(2100.0, 3220.0)
        val kpn = kv2.pseudoNorm()
        pn.x.shouldBeExactly(kpn.x)
        pn.y.shouldBeExactly(kpn.y)
    }
}