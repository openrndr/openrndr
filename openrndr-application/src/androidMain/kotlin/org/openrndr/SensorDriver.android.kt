package org.openrndr

import org.openrndr.events.Event

actual interface SensorDriver {
    actual fun gyroscope(sensorRate: SensorRate): Gyroscope
    actual fun accelerometer(sensorRate: SensorRate): Accelerometer
}

data class GyroscopeListener(val sensorRate: SensorRate)
data class AccelerometerListener(val sensorRate: SensorRate)

internal class AndroidGyroscope : Gyroscope {

    companion object {
        val instance = AndroidGyroscope()
    }

    override val updateEvent: Event<GyroscopeEvent> = Event("gyroscope")
}

internal class AndroidAccelerometer : Accelerometer {

    companion object {
        val instance = AndroidAccelerometer()
    }

    override val updateEvent: Event<AccelerometerEvent> = Event("accelerometer")
}