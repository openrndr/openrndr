package org.openrndr

import org.openrndr.events.Event

actual interface SensorDriver {
    actual fun gyroscope(sensorRate: SensorRate): Gyroscope
    actual fun accelerometer(sensorRate: SensorRate): Accelerometer
    actual fun compass(sensorRate: SensorRate): Compass
    actual fun deviceRotation(sensorRate: SensorRate): DeviceRotation
    actual fun proximity(sensorRate: SensorRate): Proximity
    actual fun light(sensorRate: SensorRate): Light
}

data class GyroscopeListener(val sensorRate: SensorRate)
data class AccelerometerListener(val sensorRate: SensorRate)
data class CompassListener(val sensorRate: SensorRate)
data class DeviceRotationListener(val sensorRate: SensorRate)
data class ProximityListener(val sensorRate: SensorRate)
data class LightListener(val sensorRate: SensorRate)

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

internal class AndroidProximity : Proximity {

    companion object {
        val instance = AndroidProximity()
    }

    override val updateEvent: Event<ProximityEvent> = Event("proximity")
}

internal class AndroidLight : Light {

    companion object {
        val instance = AndroidLight()
    }

    override val updateEvent: Event<LightEvent> = Event("light")
}