package org.openrndr

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

/**
 * launch a coroutine in the [Program] context
 */
@Suppress("EXPERIMENTAL_API_USAGE")
fun Program.launch(
    context: CoroutineContext = dispatcher,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
): Job = GlobalScope.launch(context, start, block)
