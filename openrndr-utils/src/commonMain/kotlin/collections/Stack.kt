package org.openrndr.collections

fun <E> ArrayDeque<E>.push(item: E) : E {
    addLast(item)
    return item
}

fun <E> ArrayDeque<E>.pop() : E {
    return removeLast()
}