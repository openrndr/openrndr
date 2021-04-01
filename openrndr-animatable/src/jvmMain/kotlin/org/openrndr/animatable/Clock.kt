package org.openrndr.animatable


actual class DefaultClock : Clock {
    override val timeNanos: Long
        get() = System.nanoTime()

    override val time get() = timeNanos / 1000
}