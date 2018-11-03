package org.openrndr.draw

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

interface DrawThread {
    val drawer: Drawer
    val dispatcher: CoroutineDispatcher
}

fun DrawThread.launch(
        context: CoroutineContext = this.dispatcher,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> Unit
): Job = GlobalScope.launch(context, start, block)
