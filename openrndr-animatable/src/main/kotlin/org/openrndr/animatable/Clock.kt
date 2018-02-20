package org.openrndr.animatable

interface Clock {
    val time: Long
}

class DefaultClock:Clock {
    override val time: Long
        get() = System.currentTimeMillis()
}