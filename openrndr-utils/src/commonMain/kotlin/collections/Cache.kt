package org.openrndr.utils.collections

/**
 * A cache of at most [capacity] values that evicts the least recently used values when it is full. Reads and writes
 * of present keys do not allocate.
 */
class LRUCache<K, V>(val capacity: Int = 1_000) {
    private class Entry<V>(var value: V, var stamp: Long)

    private val entries = HashMap<K, Entry<V>>()
    private var clock = 0L

    /** the number of values in the cache */
    val size: Int get() = entries.size

    fun get(key: K): V? {
        val entry = entries[key] ?: return null
        entry.stamp = ++clock
        return entry.value
    }

    fun set(key: K, value: V) {
        val entry = entries[key]
        if (entry != null) {
            entry.value = value
            entry.stamp = ++clock
            return
        }
        if (entries.size >= capacity) {
            evict()
        }
        entries[key] = Entry(value, ++clock)
    }

    /** removes the least recently used quarter of the values, so that evictions are rare */
    private fun evict() {
        val stamps = LongArray(entries.size)
        var i = 0
        for (entry in entries.values) stamps[i++] = entry.stamp
        stamps.sort()
        val keep = stamps[stamps.size / 4]
        val iterator = entries.values.iterator()
        while (iterator.hasNext()) {
            if (iterator.next().stamp < keep) iterator.remove()
        }
        if (entries.size >= capacity) {
            // all stamps are distinct, this only happens for a capacity below 4
            entries.remove(entries.entries.minByOrNull { it.value.stamp }!!.key)
        }
    }

    fun remove(key: K): V? = entries.remove(key)?.value

    fun clear() = entries.clear()

    /** the value of [key], made by [valueFunction] and cached when it is not present */
    inline fun getOrSet(key: K, valueFunction: () -> V): V {
        return get(key) ?: valueFunction().also { set(key, it) }
    }

    /** the value of [key], made by [valueFunction] and cached when it is not present or when [forceSet] is true */
    inline fun getOrSet(key: K, forceSet: Boolean, valueFunction: () -> V): V {
        val v = if (forceSet) null else get(key)
        return v ?: valueFunction().also { set(key, it) }
    }
}
