package org.openrndr

import org.openrndr.events.Event

expect interface SensorDriver {
    fun gyroscope(sensorRate: SensorRate): Gyroscope
    fun accelerometer(sensorRate: SensorRate): Accelerometer
    fun compass(sensorRate: SensorRate): Compass
    fun deviceRotation(sensorRate: SensorRate): DeviceRotation
    fun proximity(sensorRate: SensorRate): Proximity
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

interface Compass {
    val updateEvent: Event<CompassEvent>
}

interface DeviceRotation {
    val updateEvent: Event<DeviceRotationEvent>
}

interface Proximity {
    val updateEvent: Event<ProximityEvent>
}