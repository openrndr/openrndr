import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.longs.shouldBeExactly
import io.lacuna.artifex.Vec2
import io.lacuna.bifurcan.DirectedGraph
import io.lacuna.bifurcan.Graphs
import io.lacuna.bifurcan.LinearSet
import org.openrndr.kartifex.utils.graphs.Graphs as KGraphs
import org.openrndr.kartifex.utils.graphs.DirectedGraph as KDirectedGraph
import org.junit.jupiter.api.Test
import java.util.function.ToDoubleFunction
import org.openrndr.kartifex.Vec2 as KVec2

class TestDirectedGraph {
    @Test
    fun testBasic() {
        val dg = DirectedGraph<Vec2, Int>().linear()
        val kdg = KDirectedGraph<KVec2, Int>()

        val va = Vec2(0.0, 0.0)
        val vb = Vec2(1.0, 2.0)

        dg.add(va)
        dg.out(va).shouldBeEmpty()
        dg.out(Vec2(0.0, 0.0)).shouldBeEmpty()

        val kva = KVec2(0.0, 0.0)
        val kvb = KVec2(1.0, 2.0)
        kdg.add(kva)

        kdg.out(kva).shouldBeEmpty()
        kdg.out(KVec2(0.0, 0.0)).shouldBeEmpty()

        dg.link(va, vb, 0)
        dg.out(va).shouldContain(vb)
        dg.`in`(vb).shouldContain(va)
        dg.out(vb).shouldBeEmpty()
        dg.edge(va, vb).shouldBeExactly(0)

        kdg.link(kva, kvb, 0)
        kdg.out(kva).shouldContain(kvb)
        kdg.`in`(kvb).shouldContain(kva)
        kdg.out(kvb).shouldBeEmpty()
        kdg.edge(kva, kvb).shouldBeExactly(0)

        dg.unlink(va, vb)
        dg.out(va).shouldBeEmpty()
        dg.`in`(vb).shouldBeEmpty()
        dg.out(vb).shouldBeEmpty()

        kdg.unlink(kva, kvb)
        kdg.out(kva).shouldBeEmpty()
        kdg.`in`(kvb).shouldBeEmpty()
        kdg.out(kvb).shouldBeEmpty()
    }

    @Test
    fun testSelect() {
        val dg = DirectedGraph<Vec2, Int>().linear()
        val kdg = KDirectedGraph<KVec2, Int>()

        val va = Vec2(0.0, 0.0)
        val vb = Vec2(1.0, 2.0)
        val vc = Vec2(3.0, 4.0)
        dg.link(va, vb, 0)
        dg.link(va, vc, 1)

        val kva = KVec2(0.0, 0.0)
        val kvb = KVec2(1.0, 2.0)
        val kvc = KVec2(3.0, 4.0)
        kdg.link(kva, kvb, 0)
        kdg.link(kva, kvc, 1)

        dg.select(LinearSet.of(vb, vc)).vertices().size().shouldBeExactly(2)
        dg.select(LinearSet.of(va, vb)).vertices().size().shouldBeExactly(2)
        dg.select(LinearSet.of(va, vb)).edge(va, vb).shouldBeExactly(0)
        dg.select(LinearSet.of(va)).vertices().size().shouldBeExactly(1)
        dg.select(LinearSet.of()).vertices().size().shouldBeExactly(0)

        kdg.select(setOf(kvb, kvc)).vertices().size.shouldBeExactly(2)
        kdg.select(setOf(kva, kvb)).vertices().size.shouldBeExactly(2)
        kdg.select(setOf(kva, kvb)).edge(kva, kvb).shouldBeExactly(0)
        kdg.select(setOf(kva)).vertices().size.shouldBeExactly(1)
        kdg.select(setOf()).vertices().size.shouldBeExactly(0)
    }

    @Test
    fun testCycles() {
        val dg = DirectedGraph<Vec2, Int>().linear()
        val kdg = KDirectedGraph<KVec2, Int>()

        val va = Vec2(0.0, 0.0)
        val vb = Vec2(1.0, 2.0)
        val vc = Vec2(3.0, 4.0)
        val vd = Vec2(3.0, 5.0)

        dg.link(va, vb, 0)
        dg.link(vb, vc, 1)
        dg.link(vc, vd, 2)
        dg.link(vd, va, 3)

        val cs = Graphs.cycles(dg)
        cs.size().shouldBeExactly(1)
        cs.nth(0).size().shouldBeExactly(5)

        val kva = KVec2(0.0, 0.0)
        val kvb = KVec2(1.0, 2.0)
        val kvc = KVec2(3.0, 4.0)
        val kvd = KVec2(3.0, 5.0)

        kdg.link(kva, kvb, 0)
        kdg.link(kvb, kvc, 1)
        kdg.link(kvc, kvd, 2)
        kdg.link(kvd, kva, 3)

        val kcs = KGraphs.cycles(kdg)
        kcs.size.shouldBeExactly(1)
        kcs[0].size.shouldBeExactly(5)

        val scc = Graphs.stronglyConnectedComponents(dg, false)
        val kscc = KGraphs.stronglyConnectedComponents(kdg, false)
        scc.size().toInt().shouldBeExactly(kscc.size)
        scc.nth(0).size().toInt().shouldBeExactly(kscc.toList()[0].size)

        val scsg = Graphs.stronglyConnectedSubgraphs(dg, false)
        val kscsg = KGraphs.stronglyConnectedSubgraphs(kdg, false)
        scsg.size().toInt().shouldBeExactly(kscsg.size)
    }

    @Test
    fun testShortestPath() {
        val dg = DirectedGraph<Vec2, Int>().linear()
        val kdg = KDirectedGraph<KVec2, Int>()

        val va = Vec2(0.0, 0.0)
        val vb = Vec2(1.0, 2.0)
        val vc = Vec2(3.0, 4.0)
        val vd = Vec2(3.0, 5.0)

        dg.link(va, vb, 0)
        dg.link(vb, vc, 1)
        dg.link(vc, vd, 2)
        dg.link(vd, va, 3)

        val path = Graphs.shortestPath(dg, va, { it == vd }, { it.value().toDouble() }).get()
        path.size().shouldBeExactly(4)

        val kva = KVec2(0.0, 0.0)
        val kvb = KVec2(1.0, 2.0)
        val kvc = KVec2(3.0, 4.0)
        val kvd = KVec2(3.0, 5.0)

        kdg.link(kva, kvb, 0)
        kdg.link(kvb, kvc, 1)
        kdg.link(kvc, kvd, 2)
        kdg.link(kvd, kva, 3)

        val kpath = KGraphs.shortestPath(kdg, listOf(kva), { it == kvd }, { it.value().toDouble() })!!
        kpath.size.shouldBeExactly(4)

    }
}
