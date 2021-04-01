package org.openrndr.animatable

interface Clock {
    val time: Long
    val timeNanos: Long get() = time * 1000
}

expect class DefaultClock() : Clock