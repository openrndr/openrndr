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

    /**
     * Called on the thread that queues work, after a task is dispatched or a delay is scheduled, so that a loop that
     * waits while it has nothing to draw can wake up and [execute] it
     */
    @kotlin.concurrent.Volatile
    var onWorkQueued: (() -> Unit)? = null

    /**
     * The number of milliseconds until the first delayed task is due, 0 when one is due, null when there are none
     */
    fun millisUntilNextDelay(): Long? {
        val first = minOf(
            synchronized(toContinueAfterLock) { toContinueAfter.minOfOrNull { it.first } } ?: Long.MAX_VALUE,
            synchronized(toRunAfterLock) { toRunAfter.minOfOrNull { it.first } } ?: Long.MAX_VALUE
        )
        return if (first == Long.MAX_VALUE) null else (first - currentTimeMillis()).coerceAtLeast(0L)
    }

    override fun isDispatchNeeded(context: CoroutineContext): Boolean = true
    override fun toString(): String {
        return "MainCoroutineDispatcher"
    }

    override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>) {
        synchronized(toContinueAfterLock) {
            if (logger.isTraceEnabled()) logger.trace { "scheduleResume $timeMillis $continuation" }
            toContinueAfter.add(Pair(currentTimeMillis() + timeMillis, continuation))
        }
        onWorkQueued?.invoke()
    }


     override val immediate: MainCoroutineDispatcher
        get() = this

     override fun dispatch(context: CoroutineContext, block: Runnable) {
        synchronized(toRunLock) {
            if (logger.isTraceEnabled()) logger.trace { "dispatching $block" }
            toRun.add(block)
        }
        onWorkQueued?.invoke()
    }

    val shouldExecute : Boolean
        get() {
            val time = currentTimeMillis()
            return toRun.isNotEmpty() || toRunAfter.any { it.first <= time } || toContinueAfter.any { it.first <= time }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun execute() {
        lastRunTime = currentTimeMillis()
        // the tasks are copied before they run, tasks they dispatch run in the next call. Nothing is copied when
        // nothing is queued or due, which is the common case for a frame
        synchronized(toRunLock) {
            if (toRun.isNotEmpty()) {
                val copy = ArrayList(toRun)
                toRun.clear()
                for (i in copy.indices) {
                    val runnable = copy[i]
                    if (logger.isTraceEnabled()) logger.trace { "running $runnable" }
                    runnable.run()
                }
            }
        }

        val time = currentTimeMillis()
        synchronized(toRunAfterLock) {
            if (toRunAfter.any { it.first <= time }) {
                val toDo = toRunAfter.filter { it.first <= time }
                toRunAfter.removeAll { it.first <= time }
                for ((_, runnable) in toDo) {
                    if (logger.isTraceEnabled()) logger.trace { "running $runnable" }
                    runnable.run()
                }
            }
        }

        synchronized(toContinueAfterLock) {
            if (toContinueAfter.any { it.first <= time }) {
                val toDo = toContinueAfter.filter { it.first <= time }
                toContinueAfter.removeAll { it.first <= time }
                for ((_, continuation) in toDo) {
                    with(continuation) {
                        if (logger.isTraceEnabled()) logger.trace { "resuming $continuation" }
                        @Suppress("EXPERIMENTAL_API_USAGE")
                        resumeUndispatched(Unit)
                    }
                }
            }
        }
    }
}



@OptIn(DelicateCoroutinesApi::class)
fun Dispatcher.launch(start: CoroutineStart = CoroutineStart.DEFAULT,
                      block: suspend CoroutineScope.() -> Unit
): Job = GlobalScope.launch(this, start, block)

