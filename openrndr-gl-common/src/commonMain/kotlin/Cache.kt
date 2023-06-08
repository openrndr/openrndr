package org.openrndr.internal.glcommon

class LRUCache<K, V>(val capacity: Int = 1_000) {
    val map = mutableMapOf<K, V>()
    val order = ArrayDeque<K>()

    fun get(key: K): V? {
        return map[key]
    }

    fun set(key: K, value: V) {
        if (map.size >= capacity) {
            map.remove(order.removeFirst())
        }
        map[key] = value
        order.addLast(key)
    }

    fun getOrSet(key: K, forceSet: Boolean, valueFunction: () -> V): V {
        val v = get(key)
        return if (forceSet || v == null) {
            val n = valueFunction()
            set(key, n)
            n
        } else {
            v
        }
    }
}
