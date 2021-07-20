package org.openrndr.animatable

import org.openrndr.animatable.easing.Easer
import org.openrndr.animatable.easing.Easing
import org.openrndr.events.Event
import org.openrndr.math.LinearType
import kotlin.jvm.JvmName
import kotlin.jvm.JvmOverloads
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KMutableProperty0

/*
Copyright (c) 2012, Edwin Jakobs
Copyright (c) 2013, Edwin Jakobs
Copyright (c) 2020, Edwin Jakobs
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/


private val globalAnimator by lazy { Animatable() }

enum class AnimationState {
    Queued,
    Playing,
    Stopped
}

@Suppress("unused", "MemberVisibilityCanBePrivate")
open class Animatable {

    /**
     * create an animation group
     * @param builder the animation group builder function
     */
    @JvmName("animateUnitProperty")
    fun KMutableProperty0<Unit>.animationGroup(builder: (Animatable.() -> Unit)): PropertyAnimationKey<Unit> {
        pushTime()
        val before = propertyAnimationKeys.map { it }
        this@Animatable.builder()
        val added = propertyAnimationKeys subtract before

        val groupDurationInNs = added.maxByOrNull { it.endInNs }?.let {
            it.endInNs - createAtTimeInNs
        } ?: 0L
        val uak = UnitAnimationKey(this, Unit, groupDurationInNs, createAtTimeInNs)
        uak.cancelled.listen {
            for (key in added) {
                key.cancelled.trigger(AnimationEvent())
            }
            this@Animatable.propertyAnimationKeys.removeAll(added)
        }
        propertyAnimationKeys.add(uak)
        popTime()
        return uak
    }

    /**
     * animate [Double] property
     * @param targetValue the target animation value
     * @param durationInMs animation duration in milliseconds
     * @param easing the easing to use during the animation, default is [Easing.None]
     * @param predelayInMs time to wait in milliseconds before the animation starts
     */
    @JvmName("animateProp")
    fun KMutableProperty0<Double>.animate(targetValue: Double, durationInMs: Long, easing: Easing = Easing.None, predelayInMs: Long = 0): PropertyAnimationKey<Double> {
        return animate(this, targetValue, durationInMs, easing, predelayInMs)
    }

    /**
     * animate [LinearType] property
     * @param targetValue the target animation value
     * @param durationInMs animation duration in milliseconds
     * @param easing the easing to use during the animation, default is [Easing.None]
     * @param predelayInMs time to wait in milliseconds before the animation starts
     */
    @JvmName("animatePropLinearType")
    fun <T : LinearType<T>> KMutableProperty0<T>.animate(targetValue: T, durationInMs: Long, easing: Easing = Easing.None, predelayInMs: Long = 0): PropertyAnimationKey<T> {
        return animate(this, targetValue, durationInMs, easing, predelayInMs)
    }

    internal fun MutableList<PropertyAnimationKey<*>>.cancel(property: KMutableProperty<*>) {
        val toCancel = filter { it.property == property }
        for (key in toCancel) {
            key.cancelled.trigger(AnimationEvent())
        }
        removeAll(toCancel)
    }

    /**
     * cancel all animation groups on [Unit] property
     */
    @JvmName("cancelUnitProperty")
    fun KMutableProperty0<Unit>.cancel() = propertyAnimationKeys.cancel(this)

    /**
     * cancel all animations on [Double] property
     */
    @JvmName("cancelDoubleProperty")
    fun KMutableProperty0<Double>.cancel() = propertyAnimationKeys.cancel(this)

    /**
     * cancel all animations on [LinearType] property
     */
    @JvmName("cancelLinearTypeProperty")
    fun <T : LinearType<T>> KMutableProperty0<T>.cancel() = propertyAnimationKeys.cancel(this)


    @JvmName("completeUnitProperty")
    fun KMutableProperty0<Unit>.complete() {
        propertyAnimationKeys.findLast { it.property == this }?.let {
            createAtTimeInNs = it.startInNs + it.durationInNs
        }
    }

    @JvmName("completeDoubleProperty")
    fun KMutableProperty0<Double>.complete() {
        propertyAnimationKeys.findLast { it.property == this }?.let {
            createAtTimeInNs = it.startInNs + it.durationInNs
        }
    }

    @JvmName("completeLinearTypeProperty")
    fun <T : LinearType<T>> KMutableProperty0<T>.complete() {
        propertyAnimationKeys.findLast { it.property == this }?.let {
            createAtTimeInNs = it.startInNs + it.durationInNs
        }
    }

    /**
     * the last queued or active animation group for [Unit] property, or null if no animations are available
     */
    @Suppress("UNCHECKED_CAST")
    val KMutableProperty0<Unit>.last: PropertyAnimationKey<Unit>?
        @JvmName("lastUnitProperty")
        get() = propertyAnimationKeys.findLast { it.property == this } as PropertyAnimationKey<Unit>?

    /**
     * the last queued or active animation for [LinearType] property, or null if no animations are available
     */
    @Suppress("UNCHECKED_CAST")
    val <T : LinearType<T>> KMutableProperty0<T>.last: PropertyAnimationKey<T>?
        @JvmName("lastLinearTypeProperty")
        get() = propertyAnimationKeys.findLast { it.property == this } as PropertyAnimationKey<T>?

    /**
     * the last queued or active animation for [Double] property, or null if no animations are available
     */
    @Suppress("UNCHECKED_CAST")
    val KMutableProperty0<Double>.last: PropertyAnimationKey<Double>?
        @JvmName("lastDoubleProperty")
        get() = propertyAnimationKeys.findLast { it.property == this } as PropertyAnimationKey<Double>?

    /**
     * check if [Unit] property has queued or active animation groups
     */
    val KMutableProperty0<Unit>.hasAnimations
        @JvmName("hasAnimationsUnitProperty")
        get() = propertyAnimationKeys.find { it.property == this } != null

    /**
     * check if [Double] property has queued or active animation groups
     */
    val KMutableProperty0<Double>.hasAnimations
        @JvmName("hasAnimationsDoubleProperty")
        get() = propertyAnimationKeys.find { it.property == this } != null

    /**
     * check if [LinearType] property has queued or active animation groups
     */
    val <T : LinearType<T>>  KMutableProperty0<T>.hasAnimations
        @JvmName("hasAnimationsLinearTypeProperty")
        get() = propertyAnimationKeys.find { it.property == this } != null

    internal fun <T> List<PropertyAnimationKey<*>>.durationInMs(property: KMutableProperty0<T>) =
            filter { it.property == property }.maxByOrNull { it.endInNs }?.let {
                (it.endInNs - lastTimeInNs) / 1000L
            } ?: 0L

    /**
     * remaining duration of queued and activate animation groups for [Unit] property
     */
    val KMutableProperty0<Unit>.durationInMs: Long
        @JvmName("durationInMsUnit")
        get() = propertyAnimationKeys.durationInMs(this)

    /**
     * remaining duration of queued and active animations for [Double] property
     */
    val KMutableProperty0<Double>.durationInMs: Long
        @JvmName("durationInMsDouble")
        get() = propertyAnimationKeys.durationInMs(this)

    /**
     * remaining duration of queued and active animations for [LinearType] property
     */
    val <T : LinearType<T>> KMutableProperty0<T>.durationInMs: Long
        @JvmName("durationInMsLinearTypeProperty")
        get() = propertyAnimationKeys.durationInMs(this)

    /**
     * the time at which created animations will start
     */
    var createAtTimeInNs: Long = clock.timeNanos
        private set

    internal var lastTimeInNs = createAtTimeInNs

    private var propertyAnimationKeys: MutableList<PropertyAnimationKey<*>> = mutableListOf()

    private var stage: String? = null

    private var timeStack: ArrayDeque<Long>? = null

    private var animatable: Any? = null


    constructor() {
        // animationKeys!!.ensureCapacity(10)
        animatable = this
    }

    constructor(createAtTime: Long) {
        this.createAtTimeInNs = createAtTime
        //animationKeys!!.ensureCapacity(10)
        animatable = this
    }

    fun pushTime() {
        if (timeStack == null) {
            timeStack = ArrayDeque()
        }
        timeStack!!.addLast(createAtTimeInNs)
    }

    fun popTime() {
        createAtTimeInNs = timeStack!!.removeLastOrNull() ?: error("stack underflow")
    }

    /**
     * Wait for a given time before cueing the next animation.
     * @param delayInMs the delay in milliseconds
     * @return `this` for easy animation chaining
     */
    fun delay(delayInMs: Long, delayInNs: Long = 0) {
        createAtTimeInNs += delayInMs * 1_000 + delayInNs
    }

    /**
     * Wait until a the given timeMillis
     * @param timeInMs the timeMillis in milliseconds
     * @return `this` for easy animation chaining
     */
    fun waitUntil(timeInMs: Long, timeInNs: Long = 0) {
        createAtTimeInNs = timeInMs * 1_000 + timeInNs
    }

    /**
     * Queries if animations are cued.
     * @return `true` iff animations are cued.
     */
    fun hasAnimations(): Boolean {
        return propertyAnimationKeys.size != 0
    }

    /**
     * Cancels all animations.
     * @return `this` for easy animation chaining
     */
    fun cancel() {
        propertyAnimationKeys.clear()
        createAtTimeInNs = lastTimeInNs
    }

    private fun getFieldValue(variable: KMutableProperty0<Any>): Double {
        val g = variable.get()
        when (g) {
            is Double -> return g.toDouble()
        }
        return 0.0
    }

    private fun blend(easing: Easer, dt: Double, from: Double, delta: Double): Double {
        return easing.ease(dt, from, delta, 1.0)
    }

    /**
     * Updates the animation state with a user supplied time
     * @param timeInNs the time to use for updating the animation state
     */
    @JvmOverloads
    fun updateAnimation(timeInNs: Long = clock.timeNanos) {
        lastTimeInNs = timeInNs
        createAtTimeInNs = lastTimeInNs
        updatePropertyAnimations(timeInNs)
    }

    private fun updatePropertyAnimations(timeInNs: Long = clock.timeNanos) {
        val toRemove = mutableListOf<PropertyAnimationKey<*>>()
        val triggers = mutableListOf<Event<AnimationEvent>>()

        for (key in propertyAnimationKeys) {
            if (key.startInNs <= timeInNs) { // && key.start + key.duration >= time) {
                if (key.animationState == AnimationState.Queued) {
                    key.play()
                }

                if (key.animationState == AnimationState.Playing) {
                    var dt = (timeInNs - key.startInNs).toDouble()

                    if (key.durationInNs > 0) {
                        dt /= key.durationInNs
                    } else {
                        dt = 1.0
                    }

                    if (dt < 0)
                        dt = 0.0
                    if (dt >= 1) {
                        dt = 1.0
                        key.stop()
                    }
                    key.applyToProperty(dt)
                }
                if (key.animationState == AnimationState.Stopped) {
                    triggers.add(key.completed)
                    toRemove.add(key)
                }
            }
        }
        for (event in triggers) {
            event.trigger(AnimationEvent())
        }

        for (key in toRemove) {
            propertyAnimationKeys.remove(key)
        }

        lastTimeInNs = timeInNs

        if (lastTimeInNs > createAtTimeInNs) {
            createAtTimeInNs = lastTimeInNs
        }
    }

    /**
     * Returns the number of playing plus queued animations
     * @return number of playing plus queued animations
     */
    fun animationCount(): Int {
        return propertyAnimationKeys.size
    }

    companion object {
        internal var clock: Clock = DefaultClock()

        /**
         * Globally sets the clock object to use. The default is a `DefaultClock` instance
         * @param clock a `a Clock instance`
         */
        fun clock(clock: Clock) {
            Animatable.clock = clock
        }

        /**
         * Returns the global clock object
         * @return this
         */
        fun clock(): Clock {
            return clock
        }

        fun array(variable: String, index: Int): String {
            return "$variable[$index]"
        }
    }

    fun <T : LinearType<T>> animate(variable: KMutableProperty0<T>, target: T, durationMillis: Long,
                                    easing: Easing = Easing.None, predelayInMs: Long = 0): PropertyAnimationKey<T> {
        val key = LinearTypeAnimationKey(variable, target, durationMillis * 1000, createAtTimeInNs + predelayInMs * 1000, easing)
        propertyAnimationKeys.add(key)
        return key
    }

    fun animate(variable: KMutableProperty0<Double>, target: Double, durationMillis: Long,
                easing: Easing = Easing.None, predelayInMs: Long = 0): PropertyAnimationKey<Double> {
        val key = DoubleAnimationKey(variable, target, durationMillis * 1000, createAtTimeInNs + predelayInMs * 1000, easing)
        propertyAnimationKeys.add(key)
        return key
    }
}
