import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.longs.shouldBeExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.lacuna.artifex.Vec2
import io.lacuna.artifex.utils.SweepQueue
import org.openrndr.kartifex.Vec2 as KVec2
import org.openrndr.kartifex.utils.SweepQueue as KSweepQueue
import kotlin.test.Test

class TestSweepQueue {
    @Test
    fun testBasic() {
        val sq = SweepQueue<Vec2>()
        val ksq = KSweepQueue<KVec2>()

        sq.add(Vec2(0.0, 0.0), 0.0, 100.0)
        sq.active().size().shouldBeExactly(0)
        val e0 = sq.next()
        e0.shouldNotBe(null)
        e0.type.shouldBe(SweepQueue.OPEN)
        sq.active().size().shouldBeExactly(1)
        val e1 = sq.next()
        e1.shouldNotBe(null)
        e1.type.shouldBe(SweepQueue.CLOSED)

        ksq.add(KVec2(0.0, 0.0), 0.0, 100.0)
        ksq.active().size.shouldBeExactly(0)
        val ke0 = ksq.next()
        ke0.shouldNotBe(null)
        ke0.type.shouldBe(KSweepQueue.OPEN)
        ksq.active().size.shouldBeExactly(1)
        val ke1 = ksq.next()
        ke1.shouldNotBe(null)
        ke1.type.shouldBe(KSweepQueue.CLOSED)
    }

}