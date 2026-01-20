package org.openrndr.collections

actual fun <T, R> Iterable<T>.pmap(transform: (T) -> R): List<R> {
    return map(transform)
}

actual fun <T, R> Iterable<T>.pflatMap(transform: (T) -> Iterable<R>): List<R> {
    return flatMap(transform)
}

actual fun <T, R : Any> Iterable<T>.pmapNotNull(transform: (T) -> R?): List<R> {
    return pmapNotNull(transform)
}

actual fun <T : Any> Iterable<T>.pforEach(transform: (T) -> Unit) {
    return forEach(transform)
}