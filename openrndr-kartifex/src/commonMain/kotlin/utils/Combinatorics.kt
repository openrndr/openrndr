package org.openrndr.kartifex.utils

const val MAX_PERMUTATION_RESULTS = 32
/**
 * Generates permutations of the given list of values. If the size of the list exceeds a certain limit, it returns a
 * fixed number of randomly shuffled permutations. Otherwise, it calculates all possible permutations.
 *
 * @param V the type of elements in the input list
 * @param values the list of elements for which permutations are to be generated
 * @return a list of lists representing the permutations of the input list
 */
inline fun <reified V> permutations(values: List<V>): List<List<V>> {
    // if exhaustive searching is out of the question, put your trust in the RNG
    if (values.size > 4) {
        return List(MAX_PERMUTATION_RESULTS) {
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

/**
 * Swaps the elements at two specified indices in the given array.
 *
 * @param ary the array in which the elements are to be swapped
 * @param i the index of the first element to be swapped
 * @param j the index of the second element to be swapped
 */
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
        return listOf(
            paths
                .map { obj -> obj.first() }
        )
    } else if (count > MAX_PERMUTATION_RESULTS) {
        return (0 until MAX_PERMUTATION_RESULTS)
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
