package org.openrndr.collections

expect fun <T, R> Iterable<T>.pmap(transform:  (T) -> R): List<R>

expect fun <T, R> Iterable<T>.pflatMap(transform:  (T) -> Iterable<R>): List<R>

expect fun <T, R : Any> Iterable<T>.pmapNotNull(transform:  (T) -> R?): List<R>

expect fun <T : Any> Iterable<T>.pforEach(transform:  (T) -> Unit)