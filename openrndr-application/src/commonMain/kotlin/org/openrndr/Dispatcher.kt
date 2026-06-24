package org.openrndr


import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext


import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private val logger = KotlinLogging.logger {}

private var lastRunTime = -1L

@OptIn(ExperimentalTime::class)
private fun currentTimeMillis(): Long {
    return Clock.System.now().toEpochMilliseconds()
}

@OptIn(InternalCoroutinesApi::class)
class Dispatcher : MainCoroutineDispatcher(), Delay {
    private val toRun = mutableListOf<Runnable>()
    private val toRunAfter = mutableListOf<Pair<Long, Runnable>>()
    private val toContinueAfter = mutableListOf<Pair<Long, CancellableContinuation<Unit>>>()

    private val toContinueAfterLock = SynchronizedObject()
    private val toRunAfterLock = SynchronizedObject()
    private val toRunLock = SynchronizedObject()

    override fun isDispatchNeeded(context: CoroutineContext): Boolean = true
    override fun toString(): String {
        return "MainCoroutineDispatcher"
    }

    override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>) {
        synchronized(toContinueAfterLock) {
            logger.trace { "scheduleResume $timeMillis $continuation" }
            toContinueAfter.add(Pair(currentTimeMillis() + timeMillis, continuation))
        }
    }


     override val immediate: MainCoroutineDispatcher
        get() = this

     override fun dispatch(context: CoroutineContext, block: Runnable) {
        synchronized(toRunLock) {
            logger.trace { "dispatching $block" }
            toRun.add(block)
        }
    }

    val shouldExecute : Boolean
        get() {
            val time = currentTimeMillis()
            return toRun.isNotEmpty() || toRunAfter.any { it.first <= time } || toContinueAfter.any { it.first <= time }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun execute() {
        lastRunTime = currentTimeMillis()
        synchronized(toRunLock) {
            val copy = toRun + emptyList()
            toRun.clear()
            copy.forEach {
                logger.trace { "running $it" }
                it.run()
            }
        }

        val time = currentTimeMillis()
        synchronized(toRunAfterLock) {
            val toDo = toRunAfter.filter { it.first <= time }
            if (toDo.isNotEmpty()) {
                toRunAfter.removeAll { it.first <= time }
            }
            for ((_, runnable) in toDo) {
                logger.trace { "running $runnable" }
                runnable.run()
            }
        }

        synchronized(toContinueAfterLock) {
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



@OptIn(DelicateCoroutinesApi::class)
fun Dispatcher.launch(start: CoroutineStart = CoroutineStart.DEFAULT,
                      block: suspend CoroutineScope.() -> Unit
): Job = GlobalScope.launch(this, start, block)

