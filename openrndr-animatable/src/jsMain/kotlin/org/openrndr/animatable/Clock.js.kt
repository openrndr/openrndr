package org.openrndr.animatable

import kotlinx.browser.window
import kotlin.js.Date

actual class DefaultClock : Clock {
    val isBrowser = js("typeof window !== 'undefined'") as Boolean

    actual override val timeNanos: Long
        get() {
            // commonTests run on the JVM, the browser and in node.js.
            // node.js does not provide a `window` object, therefore this alternative.
            val t = if (isBrowser) window.performance.now() else Date.now()
            return (t * 1000).toLong()
        }

    actual override val time: Long get() = timeNanos / 1000
}