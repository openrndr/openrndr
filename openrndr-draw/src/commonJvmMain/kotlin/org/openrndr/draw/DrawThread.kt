package org.openrndr.draw

import kotlinx.coroutines.*
import org.openrndr.internal.Driver
import kotlin.coroutines.CoroutineContext

interface DrawThread {
    val drawer: Drawer
    val dispatcher: CoroutineDispatcher
}

/**
 * launches a coroutine on the [DrawThread]
 */
@OptIn(DelicateCoroutinesApi::class)
fun DrawThread.launch(
        context: CoroutineContext = this.dispatcher,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> Unit
): Job = GlobalScope.launch(context, start, block)

/**
 * creates and starts a DrawThread
 */
fun drawThread(): DrawThread {
    return Driver.instance.createDrawThread()
}