package org.openrndr.collections

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

actual fun <T, R> Iterable<T>.pmap(transform: (T) -> R): List<R> {
    return runBlocking {
        this@pmap.map {
            GlobalScope.async {
                transform(it)
            }
        }.map { it.await() }
    }
}

actual fun <T, R> Iterable<T>.pflatMap(transform: (T) -> Iterable<R>): List<R> {
    return runBlocking {
        this@pflatMap.map {
            GlobalScope.async {
                transform(it)
            }
        }.flatMap { it.await() }
    }
}


actual fun <T, R : Any> Iterable<T>.pmapNotNull(transform: (T) -> R?): List<R> {
    return runBlocking {
        this@pmapNotNull.map {
            GlobalScope.async {
                transform(it)
            }
        }.mapNotNull { it.await() }
    }
}

actual fun <T : Any> Iterable<T>.pforEach(transform: (T) -> Unit) {
    runBlocking {
        this@pforEach.map {
            GlobalScope.async {
                transform(it)
            }
        }.map { it.await() }
    }
}