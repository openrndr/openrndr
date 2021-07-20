package org.openrndr.kartifex.utils

object Combinatorics {
    val MAX_RESULTS = 32
    inline fun <reified V> permutations(values: List<V>): List<List<V>> {
        // if exhaustive searching is out of the question, put your trust in the RNG
        if (values.size > 4) {
            List(MAX_RESULTS) {
                values.shuffled()
            }
        }
        val result: MutableList<List<V>> = mutableListOf()
        val ary: Array<V> = values.toTypedArray()
        val c = IntArray(ary.size)
        var i = 0
        result.add(values)
        while (i < ary.size) {
            if (c[i] < i) {
                swap(ary, if (i % 2 == 0) 0 else c[i], i)
                result.add(ary.toList())
                c[i]++
                i = 0
            } else {
                c[i] = 0
                i++
            }
        }
        return result
    }

    fun <V> swap(ary: Array<V>, i: Int, j: Int) {
        val tmp = ary[i]
        ary[i] = ary[j]
        ary[j] = tmp
    }


    /**
     * Given a list of potential values at each index in a list, returns all possible combinations of those values.
     */
    fun <V> combinations(paths: List<List<V>>): List<List<V>> {
        val count = paths.map { obj -> obj.size }
            .fold(1) { a, b -> a * b }
        if (count == 0) {
            return emptyList()
        } else if (count == 1) {
            return listOf(paths
                .map { obj -> obj.first() }
            )
        } else if (count > MAX_RESULTS) {
            return (0 until MAX_RESULTS)
                .map { _ ->
                    paths
                        .map { list ->
                            list.random()
                        }
                }
        }
        val indices = IntArray(paths.size)
        val result = mutableListOf<List<V>>()
        while (indices[0] < paths.first().size) {
            val path = mutableListOf<V>()
            for (i in indices.indices) {
                path.add(paths[i][indices[i]])
            }
            result.add(path)
            for (i in indices.indices.reversed()) {
                if (++indices[i] < paths[i].size) {
                    break
                } else if (i > 0) {
                    indices[i] = 0
                }
            }
        }
        return result
    }

}