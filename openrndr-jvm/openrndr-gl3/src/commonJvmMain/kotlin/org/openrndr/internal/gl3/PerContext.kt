package org.openrndr.internal.gl3

/**
 * Values kept per GL context, created by [create] when a context is first seen. A thread that keeps using the same
 * context finds its value without a map lookup, the context id is not boxed. Values can be looked up from any thread.
 */
internal class PerContext<V : Any>(private val create: (context: Long) -> V) {
    private class Entry<V>(val context: Long, val value: V) {
        @Volatile
        var removed = false
    }

    private val entries = HashMap<Long, Entry<V>>()
    private val last = ThreadLocal<Entry<V>?>()

    /** the value of [context], created when there is none */
    operator fun get(context: Long): V {
        val hit = last.get()
        if (hit != null && hit.context == context && !hit.removed) {
            return hit.value
        }
        val entry = synchronized(entries) { entries.getOrPut(context) { Entry(context, create(context)) } }
        last.set(entry)
        return entry.value
    }

    /** the value of [context], null when there is none */
    fun getOrNull(context: Long): V? {
        val hit = last.get()
        if (hit != null && hit.context == context && !hit.removed) {
            return hit.value
        }
        return synchronized(entries) { entries[context] }?.value
    }

    /** removes the value of [context], a context created later with the same id gets a new value */
    fun remove(context: Long): V? {
        val entry = synchronized(entries) { entries.remove(context) } ?: return null
        entry.removed = true
        return entry.value
    }
}
