package org.openrndr

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class BlockingDispatcher: CoroutineDispatcher() {
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        block.run()
    }
}

private val blockingDispatcher by lazy { BlockingDispatcher() }

fun <T> Program.runBlocking(block: suspend CoroutineScope.() -> T): T {
    var result: T? = null
    CoroutineScope(blockingDispatcher).launch {
        result = block()
    }
    return result ?: error("runBlocking failed to return")

}
