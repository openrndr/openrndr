package org.openrndr

actual interface SensorDriver {
    actual fun gyroscope(sensorRate: SensorRate): Gyroscope
    actual fun accelerometer(sensorRate: SensorRate): Accelerometer
}