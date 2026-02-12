package org.openrndr

import org.openrndr.events.Event

internal class DummyGyroscope : Gyroscope {
    companion object {
        val instance = DummyGyroscope()
    }
    override val updateEvent: Event<GyroscopeEvent> = Event("gyroscope")
}

internal class DummyAccelerometer : Accelerometer {
    companion object {
        val instance = DummyAccelerometer()
    }
    override val updateEvent: Event<AccelerometerEvent> = Event("accelerometer")
}

internal class DummyCompass : Compass {
    companion object {
        val instance = DummyCompass()
    }
    override val updateEvent: Event<CompassEvent> = Event("compass")
}