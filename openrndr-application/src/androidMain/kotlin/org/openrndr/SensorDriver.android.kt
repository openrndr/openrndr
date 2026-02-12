package org.openrndr

import org.openrndr.events.Event

actual interface SensorDriver {
    actual fun gyroscope(sensorRate: SensorRate): Gyroscope
    actual fun accelerometer(sensorRate: SensorRate): Accelerometer
    actual fun compass(sensorRate: SensorRate): Compass
    actual fun deviceRotation(sensorRate: SensorRate): DeviceRotation
}

data class GyroscopeListener(val sensorRate: SensorRate)
data class AccelerometerListener(val sensorRate: SensorRate)
data class CompassListener(val sensorRate: SensorRate)
data class DeviceRotationListener(val sensorRate: SensorRate)

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

internal class AndroidCompass : Compass {

    companion object {
        val instance = AndroidCompass()
    }

    override val updateEvent: Event<CompassEvent> = Event("compass")
}

internal class AndroidDeviceRotation : DeviceRotation {

    companion object {
        val instance = AndroidDeviceRotation()
    }

    override val updateEvent: Event<DeviceRotationEvent> = Event("device-rotation")
}