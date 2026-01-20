package org.openrndr.animatable

import js.date.Date
import web.performance.performance

actual class DefaultClock : Clock {
    @OptIn(ExperimentalWasmJsInterop::class)
    val isBrowser = true

    @OptIn(ExperimentalWasmJsInterop::class)
    actual override val timeNanos: Long
        get() {
            // commonTests run on the JVM, the browser and in node.js.
            // node.js does not provide a `window` object, therefore this alternative.
            val t = if (isBrowser) performance.now().toDouble() else Date.now()
            return (t * 1000).toLong()
        }

    actual override val time: Long get() = timeNanos / 1000
}
