package org.openrndr.math

class LinearRange<T : LinearType<T>>(val start: T, val end: T) {
    fun value(t: Double) = start * (1.0 - t) + end * t
    infix fun steps(count :Int) : Sequence<T> = sequence {
        for (i in 0 until count) {
            val t = i / (count-1.0)
            yield(value(t))
        }
    }
}

private operator fun <T : LinearType<T>> LinearType<T>.rangeTo(end: T) : LinearRange<T> {
    return LinearRange(this as T, end)
}