package org.openrndr

import android.content.Context
import android.content.Context.SENSOR_SERVICE
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import org.openrndr.math.Matrix44

class SensorHandler(context: Context) {

    private var gyroscopeListener: GyroscopeListener? = null
    private var accelerometerListener: AccelerometerListener? = null
    private var compassListener: CompassListener? = null
    private var deviceRotationListener: DeviceRotationListener? = null

    private var gyroscopeEventListener: SensorEventListener? = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

        override fun onSensorChanged(event: SensorEvent) {
            AndroidGyroscope.instance.updateEvent.trigger(
                GyroscopeEvent(
                    pitch = event.values[0].toDouble(),
                    roll = event.values[1].toDouble(),
                    yaw = event.values[2].toDouble(),
                )
            )
        }
    }

    private var accelerometerEventListener: SensorEventListener? = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

        override fun onSensorChanged(event: SensorEvent) {
            AndroidAccelerometer.instance.updateEvent.trigger(
                AccelerometerEvent(
                    x = event.values[0].toDouble(),
                    y = event.values[1].toDouble(),
                    z = event.values[2].toDouble(),
                )
            )
        }
    }

    private var compassEventListener: SensorEventListener? = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

        override fun onSensorChanged(event: SensorEvent) {
            AndroidCompass.instance.updateEvent.trigger(
                CompassEvent(
                    x = event.values[0].toDouble(),
                    y = event.values[1].toDouble(),
                    z = event.values[2].toDouble(),
                )
            )
        }
    }

    private var deviceRotationEventListener: SensorEventListener? = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

        override fun onSensorChanged(event: SensorEvent) {

            val rotationMatrix = FloatArray(9)
            val matrix44 = FloatArray(16)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getRotationMatrixFromVector(matrix44, event.values)

            val orientationAngles = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)

            // Radians → Degrees
            val azimuth = Math.toDegrees(orientationAngles[0].toDouble())
            val pitch = Math.toDegrees(orientationAngles[1].toDouble())
            val roll = Math.toDegrees(orientationAngles[2].toDouble())

            val matrix = Matrix44.fromDoubleArray(matrix44.map { it.toDouble() }.toDoubleArray())

            AndroidDeviceRotation.instance.updateEvent.trigger(
                DeviceRotationEvent(
                    azimuth = azimuth,
                    pitch = pitch,
                    roll = roll,
                    matrix = matrix,
                )
            )
        }
    }
    private val sensorManager = context.getSystemService(SENSOR_SERVICE) as SensorManager
    private val gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val compassSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val deviceRotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    fun provideGyroscope(sensorRate: SensorRate): Gyroscope {
        val gyroscope = AndroidGyroscope.instance
        val listener = GyroscopeListener(sensorRate)
        gyroscopeListener = listener
        registerGyroscopeListener(listener)
        return gyroscope
    }

    fun provideAccelerometer(sensorRate: SensorRate): Accelerometer {
        val accelerometer = AndroidAccelerometer.instance
        val listener = AccelerometerListener(sensorRate)
        accelerometerListener = listener
        registerAccelerometerListener(listener)
        return accelerometer
    }

    fun provideCompass(sensorRate: SensorRate): Compass {
        val compass = AndroidCompass.instance
        val listener = CompassListener(sensorRate)
        compassListener = listener
        registerCompassListener(listener)
        return compass
    }

    fun provideDeviceRotation(sensorRate: SensorRate): DeviceRotation {
        val deviceRotation = AndroidDeviceRotation.instance
        val listener = DeviceRotationListener(sensorRate)
        deviceRotationListener = listener
        registerDeviceRotationListener(listener)
        return deviceRotation
    }
    private fun registerGyroscopeListener(listener: GyroscopeListener) {
        sensorManager.registerListener(
            gyroscopeEventListener,
            gyroscopeSensor,
            androidSensorRate(listener.sensorRate)
        )
    }

    private fun registerAccelerometerListener(listener: AccelerometerListener) {
        sensorManager.registerListener(
            accelerometerEventListener,
            accelerometerSensor,
            androidSensorRate(listener.sensorRate)
        )
    }

    private fun registerCompassListener(listener: CompassListener) {
        sensorManager.registerListener(
            compassEventListener,
            compassSensor,
            androidSensorRate(listener.sensorRate)
        )
    }

    private fun registerDeviceRotationListener(listener: DeviceRotationListener) {
        sensorManager.registerListener(
            deviceRotationEventListener,
            deviceRotationSensor,
            androidSensorRate(listener.sensorRate)
        )
    }
    private fun androidSensorRate(sensorRate: SensorRate): Int {
        return when (sensorRate) {
            SensorRate.NORMAL -> SensorManager.SENSOR_DELAY_UI
            SensorRate.UI -> SensorManager.SENSOR_DELAY_UI
            SensorRate.GAME -> SensorManager.SENSOR_DELAY_GAME
            SensorRate.FASTEST -> SensorManager.SENSOR_DELAY_FASTEST
        }
    }

    fun onResume() {
        gyroscopeListener?.let { registerGyroscopeListener(it) }
        accelerometerListener?.let { registerAccelerometerListener(it) }
        compassListener?.let { registerCompassListener(it) }
        deviceRotationListener?.let { registerDeviceRotationListener(it) }
    }

    fun onPause() {
        gyroscopeListener?.let { sensorManager.unregisterListener(gyroscopeEventListener) }
        accelerometerListener?.let { sensorManager.unregisterListener(accelerometerEventListener) }
        compassListener?.let { sensorManager.unregisterListener(compassEventListener) }
        deviceRotationListener?.let { sensorManager.unregisterListener(deviceRotationEventListener) }
    }
}