package org.openrndr.animatable

import org.openrndr.animatable.easing.Easing
import org.openrndr.events.Event
import org.openrndr.math.LinearType
import kotlin.reflect.KMutableProperty0

class AnimationEvent

class AnimationUpdateEvent(val dt: Double)

abstract class PropertyAnimationKey<T>(
        open val property: KMutableProperty0<T>,
        open val targetValue: T,
        val durationInNs: Long,
        val startInNs: Long,
        val easing: Easing
) {
    /**
     * animation cancelled event
     */
    val cancelled = Event<AnimationEvent>()

    /**
     * animation completed event
     */
    val completed = Event<AnimationEvent>()

    /**
     * animation updated event
     */
    val updated = Event<AnimationUpdateEvent>()

    internal open var startValue: T? = null

    /**
     * end time in nanoseconds of the animation
     */
    val endInNs get() = (startInNs + durationInNs)

    var animationState = AnimationState.Queued
        private set

    internal fun play() {
        if (animationState != AnimationState.Playing) {
            this.startValue = property.get()
            animationState = AnimationState.Playing
        }
    }

    internal fun stop() {
        animationState = AnimationState.Stopped
    }

    internal abstract fun applyToProperty(t: Double)
}

internal class LinearTypeAnimationKey<T : LinearType<T>>(
        override val property: KMutableProperty0<T>,
        override val targetValue: T,
        durationInNs: Long,
        startInNs: Long,
        easing: Easing
) : PropertyAnimationKey<T>(property, targetValue, durationInNs, startInNs, easing) {
    override var startValue: T? = null

    override fun applyToProperty(t: Double) {
        val et = easing.easer.ease(t, 0.0, 1.0, 1.0)
        property.set(startValue!! * (1.0 - et) + targetValue * et)
    }
}

internal class DoubleAnimationKey(
        override val property: KMutableProperty0<Double>,
        override val targetValue: Double,
        duration: Long,
        start: Long,
        easing: Easing
) : PropertyAnimationKey<Double>(property, targetValue, duration, start, easing) {
    override var startValue: Double? = null

    override fun applyToProperty(t: Double) {
        val et = easing.easer.ease(t, 0.0, 1.0, 1.0)
        property.set(startValue!! * (1.0 - et) + targetValue * et)
    }
}

internal class UnitAnimationKey(
        override val property: KMutableProperty0<Unit>,
        override val targetValue: Unit,
        duration: Long,
        start: Long,
) : PropertyAnimationKey<Unit>(property, targetValue, duration, start, Easing.None) {
    override var startValue: Unit? = null

    override fun applyToProperty(t: Double) {

    }
}