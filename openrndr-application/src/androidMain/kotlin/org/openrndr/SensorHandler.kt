package org.openrndr

import android.content.Context
import android.content.Context.SENSOR_SERVICE
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class SensorHandler(context: Context) {

    private var gyroscopeListener: GyroscopeListener? = null
    private var accelerometerListener: AccelerometerListener? = null
    private var compassListener: CompassListener? = null

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
    private val sensorManager = context.getSystemService(SENSOR_SERVICE) as SensorManager
    private val gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val compassSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

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
    }

    fun onPause() {
        gyroscopeListener?.let { sensorManager.unregisterListener(gyroscopeEventListener) }
        accelerometerListener?.let { sensorManager.unregisterListener(accelerometerEventListener) }
        compassListener?.let { sensorManager.unregisterListener(compassEventListener) }
    }
}