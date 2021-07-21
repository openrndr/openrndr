import io.kotest.matchers.collections.shouldBeEmpty
import io.lacuna.artifex.Vec2
import io.lacuna.bifurcan.DirectedGraph
import org.openrndr.kartifex.utils.graphs.DirectedGraph as KDirectedGraph
import org.junit.jupiter.api.Test
import org.openrndr.kartifex.Vec2 as KVec2

class TestDirectedGraph {
    @Test
    fun testBasic() {
        val dg = DirectedGraph<Vec2, Int>().linear()
        val kdg = KDirectedGraph<KVec2, Int>()

        val va = Vec2(0.0, 0.0)
        dg.add(va)
        dg.out(va).shouldBeEmpty()
        dg.out(Vec2(0.0, 0.0)).shouldBeEmpty()

        val kva = KVec2(0.0, 0.0)
        kdg.add(kva)

        kdg.out(kva).shouldBeEmpty()
        kdg.out(KVec2(0.0, 0.0)).shouldBeEmpty()
    }
}