package org.openrndr

import org.openrndr.events.Event

expect interface SensorDriver {
    fun gyroscope(sensorRate: SensorRate): Gyroscope
    fun accelerometer(sensorRate: SensorRate): Accelerometer
}

enum class SensorRate {
    NORMAL,
    UI,
    GAME,
    FASTEST,
}

interface Gyroscope {
    val updateEvent: Event<GyroscopeEvent>
}

interface Accelerometer {
    val updateEvent: Event<AccelerometerEvent>
}