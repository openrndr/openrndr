package org.openrndr

import kotlin.jvm.JvmRecord

@JvmRecord
data class GyroscopeEvent(val pitch: Double, val roll: Double, val yaw: Double)

@JvmRecord
data class AccelerometerEvent(val x: Double, val y: Double, val z: Double)

@JvmRecord
data class CompassEvent(val x: Double, val y: Double, val z: Double)
