package org.openrndr.animatable

import kotlin.js.Date

actual class DefaultClock : Clock {
    override val timeNanos: Long get() = time * 1000

    override val time: Long get() {
        return Date().getMilliseconds().toLong()
    }
}