package org.openrndr.collections

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

fun <T, R> Iterable<T>.pmap(transform: suspend (T) -> R): List<R> {
    return runBlocking {
        this@pmap.map {
            GlobalScope.async {
                transform(it)
            }
        }.map { it.await() }
    }
}

fun <T, R> Iterable<T>.pflatMap(transform: suspend (T) -> Iterable<R>): List<R> {
    return runBlocking {
        this@pflatMap.map {
            GlobalScope.async {
                transform(it)
            }
        }.flatMap { it.await() }
    }
}


fun <T, R : Any> Iterable<T>.pmapNotNull(transform: suspend (T) -> R?): List<R> {
    return runBlocking {
        this@pmapNotNull.map {
            GlobalScope.async {
                transform(it)
            }
        }.mapNotNull { it.await() }
    }
}

fun <T, R : Any> Iterable<T>.pforEach(transform: suspend (T) -> R?) {
    runBlocking {
        this@pforEach.map {
            GlobalScope.async {
                transform(it)
            }
        }.map { it.await() }
    }
}
