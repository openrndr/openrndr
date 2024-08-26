package org.openrndr.kartifex.utils.graphs

import org.openrndr.collections.PriorityQueue
import kotlin.jvm.JvmRecord
import kotlin.math.max
import kotlin.math.min

interface IEdge<V, E> {
    fun from(): V
    fun to(): V
    fun value(): E
}

@JvmRecord
data class Edge<V, E>(val _value: E, val _from: V, val _to: V) : IEdge<V, E> {
    override fun from(): V {
        return _from
    }

    override fun to(): V {
        return _to
    }

    override fun value(): E {
        return _value
    }
}

fun <E> Set<E>.indexOf(e: E): Int {
    val i = iterator()
    var index = 0
    while (i.hasNext()) {
        if (i.next() == e) {
            return index
        }
        index++
    }
    return -1
}

class DirectedGraph<V, E>(
    val out: MutableMap<V, MutableMap<V, E>> = mutableMapOf(),
    val `in`: MutableMap<V, MutableSet<V>> = mutableMapOf()
) {
    fun indexOf(vertex: V): Int {
        return out.keys.indexOf(vertex)
    }


    fun vertices(): Set<V> {
        return out.keys
    }

    fun edges(): Iterable<Edge<V, E>> {
        return Iterable {
            out.entries
                .flatMap { outer ->
                    outer.value
                        .entries
                        .map { inner -> Edge(inner.value, inner.key, outer.key) }
                }
                .iterator()
        }
    }

    fun edge(from: V, to: V): E {
        val m = out[from] ?: error("no such edge")
        val e = m[to] ?: error("no such edge")
        return e
    }

    fun `in`(vertex: V): Set<V> {
        val s: Set<V>? = `in`[vertex]
        return if (s == null) {
            if (out.contains(vertex)) {
                emptySet()
            } else {
                error("no such vertex")
            }
        } else {
            s
        }
    }

    fun out(vertex: V): Set<V> = out[vertex]?.keys ?: error("no such vertex $vertex")

    fun link(from: V, to:V, edge: E) = link(from, to, edge) { _, b -> b }

    fun link(from: V, to: V, edge: E, merge: (E, E) -> E) {
        add(from)
        add(to)
        val e: E? = (out[from] ?: error("no from vertex"))[to]
        if (e == null) {
            out[from]!![to] = edge
        } else {
            out[from]!![to] = merge(e, edge)
        }
        out.getOrPut(to) { mutableMapOf() }
        `in`.getOrPut(to) { mutableSetOf() }.add(from)
    }

    fun unlink(from: V, to: V) {
        (out[from] ?: error("no from vertex")).remove(to)
        (`in`[to]!!.remove(from))
    }

    fun add(vertex: V): DirectedGraph<V, E> {
        return if (out.contains(vertex)) {
            this
        } else {
            out[vertex] = mutableMapOf()
            `in`[vertex] = mutableSetOf()
            this
        }
    }


//    fun remove(vertex: V): DirectedGraph<V, E> {
//
//
//        if (out.contains(vertex)) {
//            for (v in out[vertex]!!.keys) {
//                out[v]?.remove(vertex)
//            }
//            out.remove(vertex)
//
//        }
//        `in`.remove(vertex)
//        return this
//    }

    fun select(selection: Set<V>): DirectedGraph<V, E> {
        val newOut = mutableMapOf<V, MutableMap<V,E>>()
        val newIn = mutableMapOf<V, MutableSet<V>>()
        for (entry in out.entries) {
            if (entry.key in selection) {
                newOut[entry.key] = entry.value.filterKeys { key -> key in selection }.toMutableMap()
            }
        }
        for (entry in `in`.entries) {
            if (entry.key in selection) {
                newIn[entry.key] = entry.value.filter { it in selection }.toMutableSet()
            }
        }
        return DirectedGraph(newOut, newIn)
    }

//    fun transpose(): DirectedGraph<V, E> {
//        return DirectedGraph(
//
//            out.mapValues({ u: V, x: io.lacuna.bifurcan.Map<V, E>? ->
//                `in`.get(u, EMPTY_SET as io.lacuna.bifurcan.Set<V>).map.mapValues(
//                    java.util.function.BiFunction<V, Void, E> { v: V, y: Void? -> edge(v, u) })
//            }),
//            out.mapValues(java.util.function.BiFunction<V, io.lacuna.bifurcan.Map<V, E>, io.lacuna.bifurcan.Set<V>> { x: V, m: io.lacuna.bifurcan.Map<V, E> -> m.keys() })
//        )
//    }


    override fun hashCode(): Int {
        return out.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return if (other is DirectedGraph<*, *>) {
            other.out == out
        } else {
            false
        }
    }

}

/// directed graphs
private class TarjanState(val index: Int) {
    var lowlink: Int
    var onStack: Boolean

    init {
        lowlink = index
        onStack = true
    }
}

object Graphs {
    fun <V> stronglyConnectedComponents(
        graph: DirectedGraph<V, *>,
        includeSingletons: Boolean
    ): Set<Set<V>> {

        // algorithmic state
        val state = mutableMapOf<V, TarjanState>()

        val stack = ArrayDeque<V>()

        // call-stack state
        val path = mutableListOf<V>()
        val branches = mutableListOf<Iterator<V>>()
        val result = mutableSetOf<Set<V>>()
        for (seed in graph.vertices()) {
            if (state.contains(seed)) {
                continue
            }
            branches.add(mutableListOf(seed).iterator())
            do {
                // traverse deeper
                if (branches.last().hasNext()) {
                    val w: V = branches.last().next()
                    var ws: TarjanState? = state[w]
                    if (ws == null) {
                        ws = TarjanState(state.size)
                        state[w] = ws
                        stack.addLast(w)
                        path.add(w)
                        branches.add(graph.out(w).iterator())
                    } else if (ws.onStack) {
                        val vs: TarjanState = state[path.last()]!!
                        vs.lowlink = min(vs.lowlink, ws.index)
                    }

                    // return
                } else {
                    branches.removeLast()
                    val w: V = path.removeLast()
                    val ws: TarjanState = state[w]!!

                    // update predecessor's lowlink, if they exist
                    if (path.size > 0) {
                        val v: V = path.last()
                        val vs: TarjanState = state[v]!!
                        vs.lowlink = min(vs.lowlink, ws.lowlink)
                    }

                    // create a new group
                    if (ws.lowlink == ws.index) {
                        if (!includeSingletons && stack.last() === w) {
                            stack.removeLast()
                            state[w]!!.onStack = false
                        } else {
                            val group = mutableSetOf<V>()
                            while (true) {
                                val x: V = stack.removeLast()
                                group.add(x)
                                state[x]!!.onStack = false
                                if (x === w) {
                                    break
                                }
                            }
                            result.add(group)
                        }
                    }
                }
            } while (path.size > 0)
        }
        return result
    }


    fun <V, E> stronglyConnectedSubgraphs(
        graph: DirectedGraph<V, E>,
        includeSingletons: Boolean
    ): List<DirectedGraph<V, E>> {
        val result = mutableListOf<DirectedGraph<V, E>>()
        stronglyConnectedComponents(graph, includeSingletons)
            .forEach { s ->
                result.add(
                    graph.select(s)
                )
            }
        return result
    }


    fun <V, E> cycles(graph: DirectedGraph<V, E>): List<List<V>> {
        // traversal
        val path = mutableListOf<V>()
        val branches = mutableListOf<Iterator<V>>()

        //state
        val blocked = mutableSetOf<V>()
        val blocking = mutableMapOf<V, MutableSet<V>>()
        val result = mutableListOf<List<V>>()
        for (subgraph in stronglyConnectedSubgraphs(graph, true)) {
            // simple rings are a pathological input for this algorithm, and also very common
            if (subgraph.vertices()
                    .all { v: V -> subgraph.out(v).size == 1 }
            ) {
                val seed: V = subgraph.vertices().iterator().next()
                subgraph.out(seed)
                result.add(
                    bfsVertices(seed) { vertex: V -> subgraph.out(vertex) }.asSequence().toList()+ listOf(seed)
                )
                continue
            }
            for (seed in subgraph.vertices()) {


                val threshold = subgraph.indexOf(seed)
                path.add(seed)
                branches.add(subgraph.out(seed).iterator())
                blocked.clear()
                blocking.clear()
                var depth = 1
                do {
                    // traverse deeper
                    if (branches.last().hasNext()) {
                        val v: V = branches.last().next()
                        if (subgraph.indexOf(v) < threshold) {
                            continue
                        }
                        if (seed == v) {
                            result.add(path + listOf(seed))
                            depth = 0
                        } else if (!blocked.contains(v)) {
                            path.add(v)
                            depth++
                            branches.add(subgraph.out(v).iterator())
                        }
                        blocked.add(v)
                        // return
                    } else {
                        val v: V = path.removeLast()
                        depth = max(-1, depth - 1)
                        if (depth < 0) {
                            val stack = ArrayDeque<V>().apply { addFirst(v) }
                            while (stack.size > 0) {
                                val u: V = stack.removeLast()
                                if (blocked.contains(u)) {
                                    blocked.remove(u)
                                    blocking[u] ?: emptySet<V>()
                                        .forEach { value: V -> stack.addLast(value) }
                                    blocking.remove(u)
                                }
                            }
                        } else {
                            graph.out(v).forEach { u: V -> blocking.getOrPut(u) { mutableSetOf() }.add(v) }
                        }
                        branches.removeLast()
                    }
                } while (path.size > 0)
            }
        }
        return result
    }

    /// traversal

    /// traversal
    fun <V> bfsVertices(start: V, adjacent: (V) -> Iterable<V>): Iterator<V> {
        return bfsVertices(listOf(start), adjacent)
    }

    fun <V> bfsVertices(start: Iterable<V>, adjacent: (V) -> Iterable<V>): Iterator<V> {
        val queue = ArrayDeque<V>()
        val traversed = mutableSetOf<V>()
        start.forEach { value: V -> queue.add(value) }

        return object : Iterator<V> {
            override fun hasNext(): Boolean {
                return queue.size > 0
            }

            override fun next(): V {
                val v: V = queue.removeFirst()
                traversed.add(v)
                adjacent(v).forEach { w: V ->
                    if (!traversed.contains(w)) {
                        queue.addLast(w)
                    }
                }
                return v
            }
        }
    }

    /// search
    private class ShortestPathState<V> {
        val origin: V
        val node: V
        val prev: ShortestPathState<V>?
        val distance: Double

        constructor(origin: V) {
            this.origin = origin
            prev = null
            node = origin
            distance = 0.0
        }

        constructor(node: V, prev: ShortestPathState<V>, edge: Double) {
            origin = prev.origin
            this.node = node
            this.prev = prev
            distance = prev.distance + edge
        }

        fun path(): List<V> {
            val result = ArrayDeque<V>()
            var curr: ShortestPathState<V>? = this
            while (true) {
                result.addFirst(curr!!.node)
                if (curr.node == curr.origin) {
                    break
                }
                curr = curr.prev
            }
            return result
        }
    }


    /**
     * @return the shortest path, if one exists, between a starting vertex and an accepted vertex, excluding trivial
     * solutions where a starting vertex is accepted.
     */
    fun <V, E> shortestPath(
        graph: DirectedGraph<V, E>,
        start: Iterable<V>,
        accept: (V) -> Boolean,
        cost: (IEdge<V, E>) -> Double
    ): List<V>? {
        val originStates = mutableMapOf<V, MutableMap<V, ShortestPathState<V>>>()
        val queue = PriorityQueue<ShortestPathState<V>>(compareBy { it.distance })

        for (v in start) {
            if (graph.vertices().contains(v)) {
                val init = ShortestPathState(v)
                (originStates.getOrPut(v) { mutableMapOf() })[v] = init
                queue.add(init)
            }
        }
        var curr: ShortestPathState<V>?
        while (true) {
            curr = queue.poll()
            if (curr == null) {
                return null
            }
            val states: MutableMap<V, ShortestPathState<V>> = originStates[curr.origin] ?: error("no state")
            if (states[curr.node] !== curr) {
                continue
            } else if (curr.prev != null && accept(curr.node)) {
                return curr.path()
            }
            for (v in graph.out(curr.node)) {
                val edge: Double =
                    cost(Edge(graph.edge(curr.node, v), curr.node, v))
                require(edge >= 0) { "negative edge weights are unsupported" }
                var next: ShortestPathState<V>? = states[v]
                next = if (next == null) {
                    ShortestPathState(v, curr, edge)
                } else if (curr.distance + edge < next.distance) {
                    ShortestPathState(v, curr, edge)
                } else {
                    continue
                }
                states[v] = next
                queue.add(next)
            }
        }
    }
}