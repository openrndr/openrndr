package org.openrndr.openal

internal class Queue<T>(val maxSize: Int) {
    private val array = arrayOfNulls<Any>(maxSize)
    private var head = 0
    private var tail = 0

    fun push(element: T) {
        if ((tail + 1) % maxSize == head)
            throw Error("queue overflow: tail: $tail head: $head. $maxSize")
        array[tail] = element
        tail = (tail + 1) % maxSize
    }

    @Suppress("UNCHECKED_CAST")
    fun pop(): T {
        if (tail == head)
            throw Error("queue underflow")
        val result = array[head] as T
        array[head] = null
        head = (head + 1) % maxSize
        return result
    }

    @Suppress("UNCHECKED_CAST")
    fun peek() : T? {
        if (isEmpty()) return null
        return array[head] as T
    }

    fun size() = if (tail >= head) tail - head else maxSize - (head - tail)

    fun isEmpty() = head == tail

    fun popOrNull(): T? = if (isEmpty()) null else pop()
}