package org.openrndr.animatable

import kotlinx.browser.window

actual class DefaultClock : Clock {
    override val timeNanos: Long get() = (window.performance.now() * 1000).toLong()
    override val time: Long get() = timeNanos / 1000
}