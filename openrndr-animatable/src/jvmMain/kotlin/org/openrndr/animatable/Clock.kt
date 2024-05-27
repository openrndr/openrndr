package org.openrndr.animatable


actual class DefaultClock : Clock {
    override actual val timeNanos: Long
        get() = System.nanoTime()

    override actual val time get() = timeNanos / 1000
}