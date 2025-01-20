package org.openrndr.kartifex.utils


import org.openrndr.collections.PriorityQueue
import kotlin.math.max
import kotlin.math.min

private fun copySign(magnitude: Double, sign: Double): Double {
    return if (sign < 0.0) {
        if (magnitude < 0.0) {
            magnitude
        } else {
            -magnitude
        }
    } else {
        if (magnitude > 0.0) {
            magnitude
        } else {
            -magnitude
        }
    }
}

class SweepQueue<T> {
    class Event<T> internal constructor(val key: Double, val value: T, val type: Int) {
        companion object {
            val COMPARATOR: Comparator<Event<*>> =
                Comparator { a: Event<*>, b: Event<*> ->
                    val diff = a.key - b.key
                    if (diff == 0.0) {
                        a.type - b.type
                    } else {
                        copySign(1.0, diff).toInt()
                    }
                }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private val queue: PriorityQueue<Event<T>> = PriorityQueue(Event.COMPARATOR as Comparator<Event<T>>)
    private val set = mutableSetOf<T>()
    fun add(value: T, a: Double, b: Double) {
        queue.add(Event(min(a, b) - SCALAR_EPSILON, value, OPEN))
        queue.add(Event(max(a, b) + SCALAR_EPSILON, value, CLOSED))
    }

    fun peek(): Double {
        return if (queue.isEmpty()) Double.MAX_VALUE else queue.peek().key
    }

    operator fun next(): Event<T> {
        val e: Event<T> = queue.poll() ?: error("queue empty")
        if (e.type == CLOSED) {
            set.remove(e.value)
        } else {
            set.add(e.value)
        }
        return e
    }

    fun take(): T? {
        while (!queue.isEmpty()) {
            val e = next()
            if (e.type == OPEN) {
                return e.value
            }
        }
        return null
    }

    fun active(): Set<T> {
        return set
    }

    companion object {
        const val OPEN = 0
        const val CLOSED = 1
        private fun <T> compare(a: SweepQueue<T>, b: SweepQueue<T>): Int {
            return Event.COMPARATOR.compare(a.queue.peek(), b.queue.peek())
        }

        fun <T> next(vararg queues: SweepQueue<T>): Int {
            while (true) {
                var minIdx = 0
                for (i in 1 until queues.size) {
                    if (queues[minIdx].queue.isEmpty() || !queues[i].queue.isEmpty() && compare(
                            queues[i], queues[minIdx]
                        ) < 0
                    ) {
                        minIdx = i
                    }
                }
                val q = queues[minIdx]
                if (q.queue.isEmpty() || q.queue.peek().type == OPEN) {
                    return minIdx
                } else {
                    q.next()
                }
            }
        }
    }
}
