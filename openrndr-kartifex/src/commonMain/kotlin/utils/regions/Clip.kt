package org.openrndr.kartifex.utils.regions

import org.openrndr.kartifex.Curve2
import org.openrndr.kartifex.Region2
import org.openrndr.kartifex.Ring2
import org.openrndr.kartifex.Vec2
import org.openrndr.kartifex.utils.Combinatorics
import org.openrndr.kartifex.utils.graphs.DirectedGraph
import org.openrndr.kartifex.utils.graphs.Graphs
import kotlin.math.E
import kotlin.math.abs

object Clip {
    // The approach used here is described at https://ideolalia.com/2018/08/28/artifex.html.  The "simplest" approach would
    // be to represent the unused segments as a multi-graph (since there can be multiple segments connecting any pair of vertices),
    // but the graph data structure used here is *not* a multi-graph, so instead we model it as a graph which only includes
    // the shortest edge between the vertices, and we just iterate over it multiple times.  Empirically 2-3 times should
    // always suffice, but we give ourselves a bit of breathing room because mostly we just want to preclude an infinite loop.
    private const val MAX_REPAIR_ATTEMPTS = 10

    private enum class Operation {
        UNION, INTERSECTION, DIFFERENCE
    }

    private enum class Type {
        OUTSIDE, INSIDE, SAME_EDGE, DIFF_EDGE
    }

    private fun operation(
        ra: Region2,
        rb: Region2,
        operation: Operation,
        aPredicate: (Type) -> Boolean,
        bPredicate: (Type) -> Boolean
    ): Region2 {
        val split: Split.Result = Split.split(ra, rb)
        val a: Region2 = split.a
        val b: Region2 = split.b

        // Partition rings into arcs separated at intersection points
        val pa: List<Arc> = partition(a, split.splits)
        var pb: List<Arc> = partition(b, split.splits)
        if (operation == Operation.DIFFERENCE) {
            pb = pb.map { obj: Arc -> obj.reverse() }
        }

        // Filter out arcs which are to be ignored, per our operation
        var arcs: MutableSet<Arc> = mutableSetOf()
        pa.filter { arc: Arc -> aPredicate(classify(b, arc)) }.forEach { value -> arcs.add(value) }
        pb.filter { arc: Arc -> bPredicate(classify(a, arc)) }.forEach { value -> arcs.add(value) }

        /*
    describe("split", split.splits.elements());
    describe("arcs", arcs.elements().stream().map(Arc::vertices).toArray(IList[]::new));
    VERTICES.forEach(v -> System.out.println(VERTICES.indexOf(v) + " " + v));
    // */
        val result = mutableListOf<Ring2>()
        val consumed = mutableSetOf<Arc>()

        // First we're going to extract complete cycles, and then try to iteratively repair the graph
        for (i in 0 until MAX_REPAIR_ATTEMPTS) {
            // Construct a graph where the edges are the set of all arcs connecting the vertices
            val graph = DirectedGraph<Vec2, Set<Arc>>()
            arcs.forEach { arc: Arc ->
                graph.link(
                    arc.head(), arc.tail(), mutableSetOf(arc)
                ) { obj, s -> obj.union(s) }
            }

            //graph.vertices().forEach(v -> System.out.println(VERTICES.indexOf(v) + " " + graph.out(v).stream().map(VERTICES::indexOf).collect(Lists.linearCollector())));
            if (i > 0) {
                for (path in repairGraph(
                    graph,
                    (pa + pb) - arcs - consumed
                )) {
                    for (arc in path) {
                        // if the graph currently contains the arc, remove it
                        if (arcs.contains(arc)) {
                            //describe("remove", arc.vertices());
                            graph.unlink(arc.head(), arc.tail())
                            arcs.remove(arc)

                            // if the graph doesn't contain the arc, add it
                        } else {
                            //describe("add", arc.vertices());
                            graph.link(arc.head(), arc.tail(), mutableSetOf(arc))
                            arcs.add(arc)
                        }
                    }
                }
            }

            // find every cycle in the graph, and then expand those cycles into every possible arc combination, yielding a bunch
            // of rings ordered from largest to smallest
            val cycles: List<List<Arc>> = Graphs.cycles(graph)
                .map { cycle ->
                    edges(cycle
                    ) { x, y -> graph.edge(x, y).toList() }
                }
                .map { paths -> Combinatorics.combinations(paths) }
                .flatten()
                .sortedBy { area -> area(area) }
                .reversed()

            // extract as many cycles as possible without using the same arc twice
            for (cycle in cycles) {
                //describe("cycle", cycle.stream().map(Arc::vertices).toArray(IList[]::new));
                if (cycle.any { value -> consumed.contains(value) }
                ) {
                    continue
                }
                cycle.forEach { value -> consumed.add(value) }
                result.add(ring(cycle))
            }
            arcs = (arcs - consumed).toMutableSet()
            if (arcs.size == 0) {
                break
            }
        }
        //assert(arcs.size() == 0L)
        return Region2(result)
    }

    private fun isTop(c: Curve2): Boolean {
//        if (c == null) {
//            return false
//        }
        val delta: Double = c.end().x - c.start().x
        return if (delta == 0.0) {
            c.end().y > c.start().y
        } else delta < 0
    }


    private fun classify(
        region: Region2,
        arc: Arc
    ): Type {
        // we want some point near the middle of the arc which is unlikely to coincide with a vertex, because those
        // sometimes sit ambiguously on the edge of the other region
        val result: Ring2.Result = region.test(arc.position(1.0 / E))
        return if (!result.inside) {
            Type.OUTSIDE
        } else if (result.curve == null) {
            Type.INSIDE
        } else {
            if (isTop(arc.first()) == isTop(result.curve))
                Type.SAME_EDGE else Type.DIFF_EDGE
        }
    }

    /**
     * Cuts the rings of a region at the specified vertices, yielding a list of arcs that will serve as the edges of our
     * graph.
     */
    private fun partition(
        region: Region2,
        vertices: Set<Vec2>
    ): List<Arc> {
        val result: MutableList<Arc> = mutableListOf()
        for (r in region.rings) {
            val cs: Array<Curve2> = r.curves
            var offset = 0
            while (offset < cs.size) {
                if (vertices.contains(cs[offset].start())) {
                    break
                }
                offset++
            }
            if (offset == cs.size) {
                result.add(Arc(cs.toMutableList()))
            } else {
                var acc = Arc()
                for (i in offset until cs.size) {
                    val c: Curve2 = cs[i]
                    if (vertices.contains(c.start())) {
                        if (acc.size > 0) {
                            result.add(acc)
                        }
                        acc = Arc(mutableListOf(c))
                    } else {
                        acc.add(c)
                    }
                }
                for (i in 0 until offset) {
                    acc.add(cs[i])
                }
                if (acc.size > 0) {
                    result.add(acc)
                }
            }
        }
        return result
    }

    private val SHORTEST_ARC = { x: Arc, y: Arc -> if (x.length() < y.length()) x else y }

    private fun repairGraph(
        graph: DirectedGraph<Vec2, Set<Arc>>,
        unused: Iterable<Arc>
    ): List<List<Arc>> {

        // create a graph of all the unused arcs
        val search = DirectedGraph<Vec2, Arc>()
        for (arc in unused) {
            search.link(arc.head(), arc.tail(), arc, SHORTEST_ARC)
        }

        // add in the existing arcs as reversed edges, so we can potentially retract them
        for (e in graph.edges()) {
            val arc: Arc = e.value().minByOrNull(
                { obj -> obj.length() })!!

            search.link(arc.tail(), arc.head(), arc, SHORTEST_ARC)
        }

        //search.vertices().forEach(v -> System.out.println(VERTICES.indexOf(v) + " " + search.out(v).stream().map(VERTICES::indexOf).collect(Lists.linearCollector())));
        //graph.vertices().forEach(v -> System.out.println(VERTICES.indexOf(v) + " " + graph.out(v).stream().map(VERTICES::indexOf).collect(Lists.linearCollector())));
        val `in` = graph.vertices()
            .filter { v -> graph.`in`(v).isEmpty() }.toSet()
        val out = graph.vertices()
            .filter { v -> graph.out(v).isEmpty() }.toSet()
        val currIn = (`in` + setOf()).toMutableSet()
        val currOut = (out + setOf()).toMutableSet()

        // attempt to greedily pair our outs and ins
        val result = mutableSetOf<List<Arc>>()
        while (currIn.size > 0 && currOut.size > 0) {
            val path = Graphs.shortestPath(search, currOut,
                { value -> `in`.contains(value) },
                { e -> e.value().length() }
            )
            // if our search found a vertex that was previously claimed, we need something better than a greedy search
            if (path == null || !currIn.contains(path.last())) {
                break
            } else {
                currOut.remove(path.first())
                currIn.remove(path.last())
                result.add(edges(path) { from, to -> search.edge(from, to) })
            }
        }
        return if (currIn.size == 0 || currOut.size == 0) {
            result.toList()
        } else Combinatorics.permutations(out.toList())
            .map { vs -> greedyPairing(search, vs, `in`) }
            .minByOrNull { path -> path.sumOf { arcs -> length(arcs) } }
            .orEmpty()

        // Do greedy pairings with every possible vertex ordering, and choose the one that results in the shortest aggregate
        // paths. If `out` is sufficiently large, `permutations` will just return a subset of random shufflings, and it's
        // possible we won't find a single workable solution this time around.
    }

    private fun greedyPairing(
        graph: DirectedGraph<Vec2, Arc>,
        out: List<Vec2>,
        `in`: Set<Vec2>
    ): List<List<Arc>> {
        val result: MutableList<List<Arc>> = mutableListOf()
        val currIn = (`in` + setOf()).toMutableSet()
        for (v in out) {
            // this will only happen if a vertex needs to have multiple edges added/removed, but we'll just get it on the
            // next time around
            if (currIn.size == 0) {
                break
            }
            val path: List<Vec2> = Graphs.shortestPath(graph, listOf(v),
                { value -> currIn.contains(value) },
                { e -> e.value().length() }) ?: (return emptyList())
            currIn.remove(path.last())
            result.add(edges(path) { from, to -> graph.edge(from, to) })
        }
        return result
    }

    private fun <U, V> edges(vertices: List<U>, edge: (U, U) -> V): List<V> {
        val result = mutableListOf<V>()
        for (i in 0 until vertices.size - 1) {
            result.add(edge(vertices[i], vertices[(i + 1)]))
        }
        return result
    }

    private fun area(arcs: List<Arc>): Double {
        return abs(arcs.sumOf { arc -> arc.signedArea() })
    }

    private fun length(arcs: List<Arc>): Double {
        return abs(arcs.sumOf { arc -> arc.length() })
    }

    private fun ring(arcs: List<Arc>): Ring2 {
        val acc: MutableList<Curve2> = mutableListOf()
        arcs.forEach { arc ->
            arc.forEach { value -> acc.add(value) }
        }
        return Ring2(acc)
    }

    ///
    fun union(a: Region2, b: Region2): Region2 {
        return operation(a, b,
            Operation.UNION,
            { t -> t == Type.OUTSIDE || t == Type.SAME_EDGE },
             { t: Type -> t == Type.OUTSIDE })
    }

    fun intersection(a: Region2, b: Region2): Region2 {
        return operation(a, b,
            Operation.INTERSECTION,
             { t: Type -> t == Type.INSIDE || t == Type.SAME_EDGE },
             { t: Type -> t == Type.INSIDE })
    }

    fun difference(a: Region2, b: Region2): Region2 {
        return operation(a, b,
            Operation.DIFFERENCE,
             { t: Type -> t == Type.OUTSIDE || t == Type.DIFF_EDGE },
             { t: Type -> t == Type.INSIDE })
    }
}