package org.openrndr.animatable

interface Clock {
    val time: Long
    val timeNanos: Long get() = time * 1000
}

class DefaultClock : Clock {
    override val timeNanos: Long
        get() = System.nanoTime()

    override val time get() = timeNanos / 1000

}