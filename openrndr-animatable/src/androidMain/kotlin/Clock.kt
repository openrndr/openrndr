package org.openrndr.animatable


actual class DefaultClock : Clock {
    actual override val timeNanos: Long
        get() = System.nanoTime()

    actual override val time get() = timeNanos / 1000
}