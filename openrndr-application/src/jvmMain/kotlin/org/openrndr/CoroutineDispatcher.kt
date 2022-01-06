package org.openrndr

import kotlinx.coroutines.*
import mu.KotlinLogging
import kotlin.coroutines.CoroutineContext

private val logger = KotlinLogging.logger {}


private var lastRunTime = -1L

@OptIn(InternalCoroutinesApi::class)
actual class Dispatcher : MainCoroutineDispatcher(), Delay {
    private val toRun = mutableListOf<Runnable>()
    private val toRunAfter = mutableListOf<Pair<Long, Runnable>>()
    private val toContinueAfter = mutableListOf<Pair<Long, CancellableContinuation<Unit>>>()


    override fun isDispatchNeeded(context: CoroutineContext): Boolean = true
    override fun toString(): String {
        return "MainCoroutineDispatcher"
    }

    override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>) {
        synchronized(toContinueAfter) {
            logger.trace { "scheduleResume $timeMillis $continuation" }
            toContinueAfter.add(Pair(System.currentTimeMillis() + timeMillis, continuation))
        }
    }


    override val immediate: MainCoroutineDispatcher
        get() = this

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        synchronized(toRun) {
            logger.trace { "dispatching $block" }
            toRun.add(block)
        }
    }


    fun execute() {
        lastRunTime = System.currentTimeMillis()
        synchronized(toRun) {
            val copy = toRun + emptyList()
            toRun.clear()
            copy.forEach {
                logger.trace { "running $it" }
                it.run()
            }
        }

        val time = System.currentTimeMillis()
        synchronized(toRunAfter) {
            val toDo = toRunAfter.filter { it.first <= time }
            if (!toDo.isEmpty()) {
                toRunAfter.removeAll { it.first <= time }
            }
            for ((_, runnable) in toDo) {
                logger.trace { "running $runnable" }
                runnable.run()
            }
        }

        synchronized(toContinueAfter) {
            val toDo = toContinueAfter.filter { it.first <= time }
            if (toDo.isNotEmpty()) {
                toContinueAfter.removeAll { it.first <= time }
            }
            for ((_, continuation) in toDo) {
                with(continuation) {
                    logger.trace { "resuming $continuation" }
                    @Suppress("EXPERIMENTAL_API_USAGE")
                    resumeUndispatched(Unit)
                }
            }
        }
    }
}

suspend fun throttle(timeMillis: Long) {
    if ((System.currentTimeMillis() - lastRunTime) > timeMillis) {
        delay(1)
    }
}

@Suppress("EXPERIMENTAL_API_USAGE")
fun Dispatcher.launch(start: CoroutineStart = CoroutineStart.DEFAULT,
                      block: suspend CoroutineScope.() -> Unit
): Job = GlobalScope.launch(this, start, block)

