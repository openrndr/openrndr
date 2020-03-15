package org.openrndr.animatable

import org.openrndr.animatable.easing.Easer
import org.openrndr.animatable.easing.Easing
import java.lang.reflect.Field
import java.util.*
import kotlin.reflect.KMutableProperty0

/*
Copyright (c) 2012, Edwin Jakobs
Copyright (c) 2013, Edwin Jakobs
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/


/**
 * The Animatable base class.
 *
 * Example usage:
 * <pre>
 * class Foo extends Animatable {
 * double x;
 * double y;
 * }
</pre> *
 * Then construct a `Foo` object and cue an animation
 * <pre>
 * Foo foo = new Foo();
 * foo.animate("x", 5, 1000).animate("y", 10, 1000);
</pre> *
 * In your animation loop you update the object's animation and use its animated fields:
 * <pre>
 * foo.updateAnimation();
 * drawFoo(foo.x, foo.y);
</pre> *
 *
 * @author Edwin Jakobs
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
open class Animatable {

    @JvmName("animateProp")
    fun KMutableProperty0<Double>.animate(target: Double, duration: Long, easing: Easing) {
        animate(this, target, duration, easing)
    }


    internal var createAtTime: Long = clock.timeNanos
    internal var lastTime = createAtTime

    private var animationKeys: MutableList<AnimationKey>? = mutableListOf()

    private var stage: String? = null

    private var timeStack: Stack<Long>? = null

    // private var emitListeners: ArrayList<EmitListener>? = null// = new ArrayList<EmitListener>();

    private var animatable: Any? = null


    private val fieldCache = HashMap<String, Field>()


    constructor(target: Any) {
        animatable = target
        //   animationKeys!!.ensureCapacity(10)
    }

    constructor() {
        // animationKeys!!.ensureCapacity(10)
        animatable = this
    }

    constructor(createAtTime: Long) {
        this.createAtTime = createAtTime
        //animationKeys!!.ensureCapacity(10)
        animatable = this
    }

    fun pushTime() {
        if (timeStack == null) {
            timeStack = Stack()
        }
        timeStack!!.push(createAtTime)
    }

    fun popTime() {
        createAtTime = timeStack!!.pop()
    }

    /**
     * Wait for a given time before cueing the next animation.
     * @param durationMillis the delay in milliseconds
     * @return `this` for easy animation chaining
     */
    fun delay(durationMillis: Long, durationNanos:Long = 0) {
        createAtTime += durationMillis * 1_000 + durationNanos
    }

    /**
     * Wait until a the given timeMillis
     * @param timeMillis the timeMillis in milliseconds
     * @return `this` for easy animation chaining
     */
    fun waitUntil(timeMillis: Long, timeNanos:Long = 0) {
        createAtTime = timeMillis * 1_000 + timeNanos
    }


    /**
     * Animates a single variable.
     * @param variable the name of the variable to animate
     * @param target the target value
     * @param durationMillis the durationMillis of the animation in milliseconds
     * @return `this` for easy animation chaining
     */
    fun animate(variable: String, target: Double, durationMillis: Long) {
        return animate(variable, target, durationMillis, Easing.None)
    }

    /**
     * Animates a single variable.
     * @param variable the name of the variable to animate
     * @param target the target value
     * @param durationMillis the durationMillis of the animation in milliseconds
     * @param easing the easing to use during the animation
     * @return `this` for easy animation chaining
     * Also arrays of floats or doubles can be animated
     * <pre>animatable.animate("x[0]", 100, 1000).complete().animate("x[0]", 0, 1000);</pre>
     */
    fun animate(variable: String, target: Double, durationMillis: Long, easing: Easing) {
        return animate(variable, target, durationMillis, easing.easer)
    }

    /**
     * Animates a single variable.
     * @param variable the name of the variable to animate
     * @param target the target value
     * @param durationMillis the durationMillis of the animation in milliseconds
     * @param easer the easing to use during the animation
     * @return `this` for easy animation chaining
     * Also arrays of floats or doubles can be animated
     * <pre>animatable.animate("x[0]", 100, 1000).complete().animate("x[0]", 0, 1000);</pre>
     */
    fun animate(variable: String, target: Double, durationMillis: Long, easer: Easer) {
        val key = AnimationKey(variable, target, durationMillis * 1000, createAtTime)
        key.easing = easer
        if (animationKeys == null) {
            animationKeys = mutableListOf()
        }
        animationKeys!!.add(key)
    }


    /**
     * Queries if animations are cued.
     * @return `true` iff animations are cued.
     */
    fun hasAnimations(): Boolean {
        return animationKeys!!.size != 0
    }

    /**
     * Queries if animations matching any of the given variables are cued.
     * @param variables
     * @return `true` iff animations matching any of the given variables are cued.
     */
    fun hasAnimations(vararg variables: String): Boolean {
        val variableSet = HashSet<String>()
        for (variable in variables) {
            variableSet.add(variable)
        }
        return animationKeys?.any { variableSet.contains(it.variable) } ?: false
    }

    /**
     * Cues an additive animation for a single variable.
     * @param variable the target animation
     * @param target the target value
     * @param duration the duration of the animation in milliseconds
     * @return `this` for easy animation chaining
     */
    fun add(variable: String, target: Double, duration: Long) {
        add(variable, target, duration, Easing.None)
    }

    /**
     * Cues an additive animation for a single variable using a given easing mode.
     * @param variable the target animation
     * @param target the target value
     * @param durationMillis the durationMillis of the animation in milliseconds
     * @param easing the easing to use for this animation
     * @return `this` for easy animation chaining
     *
     * Example usage:
     * <pre>
     * `animatable.add("x", 100, 1000);
     * animatable.complete();
     * animatable.add("x", 0, 1000);`
    </pre> *
     */

    fun add(variable: String, target: Double, durationMillis: Long, easing: Easing) {
        add(variable, target, durationMillis, easing.easer)
    }

    fun add(variable: String, target: Double, durationMillis: Long, easer: Easer) {
        val key = AnimationKey(variable, target, durationMillis * 1000, createAtTime)
        key.animationMode = AnimationKey.AnimationMode.Additive
        key.easing = easer
        animationKeys!!.add(key)
    }


    fun complete(variable: String) {
        val key = lastQueued(variable)
        if (key != null) {
            createAtTime = key.start + key.duration

        }
    }

    /**
     * Wait for the last cued animation to complete before starting the next animation and install
     * a completion call back that is called when the last queued animation ends.
     *
     * Example usage:
     * <pre>
     * `AnimationCompletedCallBack callback = new AnimationCompletedCallBack() {
     * public void animationCompleted(Animatable target) {
     * System.out.println("animation completed");
     * }
     * }
     *
     * animatable.animate("x", 100, 1000);
     * animatable.complete(callback);
     * animatable.animate("x", 0, 1000);`
    </pre> *
     *
     * @param callback the callback to call when the animation is completed
     * @return `this` for easy animation chaining
     */
    fun complete(callback: (Animatable) -> Unit) {
        return complete(this, callback)
    }


    /**
     * Sets the easing mode for the last cued animation.
     *
     *
     * Example usage:
     * <pre>
     * `animatable.animate("x", 100, 1000).ease(Easing.BounceOut);
     * animatable.complete();
     * animatable.animate("x", 0, 1000).ease(Easing.BounceIn);`
    </pre> *
     *
     * @param easing the easing mode to set
     * @return `this` for easy animation chaining
     */
    fun ease(easing: Easing) {
        return ease(easing.easer)
    }

    fun ease(easer: Easer) {
        val last = lastQueued()
        if (last != null) {
            last.easing = easer
        }
    }

    /**
     * Returns the current velocity of the animating variable
     * @param variable the variable to give the velocity for
     * @return velocity in units per second
     */

    fun velocity(variable: String): Double {


        for (key in animationKeys!!) {
            if (key.start <= lastTime && key.variable.equals(variable)) {

                val delta = key.target - key.from
                val dt = (lastTime - key.start) / 1E6


                return key.easing.velocity(dt, key.from, delta, key.durationSeconds)

            }
        }
        return 0.0
    }


    /**
     * Wait for the last cued animation in the given animatable to complete before starting the next animation.
     * @param animatable the animatable to wait for
     * @return `this` for easy animation chaining
     */
    fun complete(animatable: Animatable) {
        //noinspection unchecked,RedundantCast
        return complete(animatable, null)
    }


    /**
     * Wait for the animation to complete in the given animatable to complete before starting the next animation and install
     * a completion call back that is called when the last queued animation ends.
     * @param animatable the animatable to wait for
     * @param callback the callback to call when the animation is completed
     * @return `this` for easy animation chaining
     */

    fun complete(animatable: Animatable = this, callback: ((Animatable) -> Unit)? = null) {

        val waitForKey = animatable.lastQueued()

        if (waitForKey != null) {
            createAtTime = waitForKey.start + waitForKey.duration
            if (callback != null)
                animationKeys!![animationKeys!!.size - 1].addCompletionCallback(callback)
        } else {
            callback?.invoke(this)
        }
    }

    /**
     * Cancels all animations.
     * @return `this` for easy animation chaining
     */
    fun cancel() {
        animationKeys?.clear()
        createAtTime = clock.timeNanos
    }

    /**
     * Cancels all queued animations. Running animations will run until end.
     * @return `this` for easy animation chaining
     */

    fun cancelQueued() {
        val keep = mutableListOf<AnimationKey>()
        animationKeys?.let {keys ->
            keys.filterTo(keep) { it.animationState == AnimationKey.AnimationState.Playing }
            keys.clear()
            keys.addAll(keep)
        }
    }


    /**
     * Ends running animations and cancels all cued animations. The behaviour of end() differs from cancel() in how animated variables are treated, end() sets the animated variables to their target value. Using end() will produce a pop in the animation.
     * @return `this` for easy animation chaining
     */
    fun end() {
        for (key in animationKeys!!) {
            if (key.animationState == AnimationKey.AnimationState.Playing) {
                setFieldValue(key.variable, key.target)
            }
        }

        animationKeys?.clear()
        createAtTime = lastTime
    }

    /**
     * Cancels selected animations
     * <pre>
     * animatable.animate("x", 100, 1000);
     * animatable.animate("y", 100, 1000);
     * animatable.cancel(new String["x"]);
    </pre> *
     * @param fields the names of the fields for which animations should be cancelled
     * @return `this` for easy animation chaining
     */
    fun cancel(fields: Array<String>) {

        val filter = HashSet<String>()
        Collections.addAll(filter, *fields)

        val toRemove = ArrayList<AnimationKey>()
        for (key in animationKeys!!) {
            if (filter.contains(key.variable)) {
                toRemove.add(key)
            }
        }

        animationKeys?.removeAll(toRemove)


        //animationKeys.clear();
        createAtTime = lastTime
    }

    private fun getField(variable: String): Field? {

        var field: Field? = fieldCache[variable]

        if (field == null) {
            if (animatable == null) {
                animatable = this
            }
            var `class`: Class<*>? = animatable?.javaClass

            while (`class` != null) {
                try {
                    field = `class`.getDeclaredField(variable)
                    field!!.isAccessible = true
                    fieldCache.put(variable, field)
                    return field
                } catch (e: NoSuchFieldException) {
                    `class` = `class`.superclass
                }
            }
        }
        return field
    }


    private fun getFieldValue(variable: String): Double {

        //getFieldValue(this::lastTime)

        try {

            val indexPos = if (variable[variable.length - 1] == ']') variable.indexOf("[") else -1
            if (indexPos != -1) {

                val fieldName = variable.substring(0, indexPos)
                val keyName = variable.substring(indexPos + 1, variable.length - 1)
                val field = getField(fieldName)

                val _class = field!!.type

                if (_class == DoubleArray::class.java) {
                    val offset = Integer.parseInt(keyName)
                    val a = field.get(animatable) as DoubleArray
                    return a[offset]
                } else if (_class == FloatArray::class.java) {
                    val offset = Integer.parseInt(keyName)
                    val a = field.get(animatable) as FloatArray
                    return a[offset].toDouble()
                } else if (field.get(this) is List<*>) {
                    val offset = Integer.parseInt(keyName)
                    val a = field.get(this) as List<*>
                    val o = a[offset]
                    if (o is Double) {
                        return o
                    } else if (o is Float) {
                        return o.toDouble()
                    }
                }
            } else {
                val field = getField(variable)
                if (field != null) {
                    return field.getDouble(animatable)
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }

        return 0.0
    }

    private fun getFieldValue(variable: KMutableProperty0<Any>): Double {
        val g = variable.get()
        when (g) {
            is Double -> return g.toDouble()
        }
        return 0.0
    }


    private fun lastQueued(): AnimationKey? {
        return if (animationKeys!!.size > 0) {
            animationKeys!![animationKeys!!.size - 1]
        } else
            null
    }

    private fun lastQueued(variable: String): AnimationKey? {
        for (i in animationKeys!!.indices.reversed()) {
            val k = animationKeys!![i]
            if (variable == k.variable) {
                return k
            }
        }
        return null
    }

    private fun setFieldValue(variable: String, value: Double) {

        try {

            val indexPos = if (variable[variable.length - 1] == ']') variable.indexOf("[") else -1
            if (indexPos != -1) {

                val fieldName = variable.substring(0, indexPos)
                val keyName = variable.substring(indexPos + 1, variable.length - 1)

                val field = getField(fieldName)
                val _class = field!!.type
                field.isAccessible = true
                if (_class == DoubleArray::class.java) {
                    val offset = Integer.parseInt(keyName)
                    val a = field.get(animatable) as DoubleArray
                    a[offset] = value
                } else if (_class == FloatArray::class.java) {
                    val offset = Integer.parseInt(keyName)
                    val a = field.get(animatable) as FloatArray
                    a[offset] = value.toFloat()
                } else if (field.get(animatable) is MutableList<*>) {
                    val offset = Integer.parseInt(keyName)
                    val list = field.get(animatable) as MutableList<*>
                    val o = list[offset]
                    if (o is Float) {
                        @Suppress("UNCHECKED_CAST")
                        val floatList = field.get(animatable) as MutableList<Float>
                        floatList[offset] = value.toFloat()
                    } else if (o is Double) {
                        @Suppress("UNCHECKED_CAST")
                        val doubleList = field.get(animatable) as MutableList<Double>
                        doubleList[offset] = value
                    }
                }
            } else {
                val field = getField(variable)

                if (field != null) {
                    if (field.type == Double::class.javaPrimitiveType) {
                        field.setDouble(animatable, value)
                    } else if (field.type == Float::class.javaPrimitiveType) {
                        field.setFloat(animatable, value.toFloat())
                    } else {
                        System.err.println("warning: could not set animatable value")
                    }
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }

    }


    private fun blend(easing: Easer, dt: Double, from: Double, delta: Double): Double {
        return easing.ease(dt, from, delta, 1.0)
    }


    /**
     * Returns the duration of the animation in milliseconds
     * @return the duration of the running plus queued animations
     */
    fun duration(): Long {
        var latest: Long = 0
        for (key in this.animationKeys!!) {
            val end = key.start + key.duration
            if (end > latest) {
                latest = end
            }
        }

        return Math.max(0L, Math.max(latest, createAtTime) - lastTime)
    }

    /**
     * Updates the animation state with a user supplied time
     * @param time the time to use for updating the animation state
     */
    @JvmOverloads
    fun updateAnimation(time: Long = clock.timeNanos) {

        val toRemove = mutableListOf<AnimationKey>()
        val calls = mutableListOf<Pair<(Animatable) -> Unit, Animatable>>()



        if (animationKeys == null) {
            animationKeys = mutableListOf()
        }
        for (key in animationKeys!!) {

            if (key.start == -1L) {
                key.start = time
            }

            if (key.start <= time) { // && key.start + key.duration >= time) {

                if (key.animationState == AnimationKey.AnimationState.Queued) {
                    key.play(getFieldValue(key.variable))

                }

                if (key.animationState == AnimationKey.AnimationState.Playing) {
                    var dt = (time - key.start).toDouble()

                    if (key.duration > 0) {
                        dt /= key.duration
                    } else {
                        dt = 1.0
                    }

                    if (dt < 0)
                        dt = 0.0
                    if (dt >= 1) {
                        dt = 1.0
                        key.stop()
                    }

                    if (key.animationMode == AnimationKey.AnimationMode.Blend) {
                        val value = blend(key.easing, dt, key.from, key.target - key.from)
                        setFieldValue(key.variable, value)
                    } else if (key.animationMode == AnimationKey.AnimationMode.Additive) {
                        val value = blend(key.easing, dt, key.from, key.target)
                        setFieldValue(key.variable, value)
                    }

                }

                if (key.animationState == AnimationKey.AnimationState.Stopped) {
                    for (callback in key.completionCallbacks) {
                        calls.add(Pair(callback, this))
                    }
                    toRemove.add(key)
                }
            }

        }

        for (call in calls) {
            call.first(call.second)
        }

        for (key in toRemove) {
            animationKeys!!.remove(key)
        }

        lastTime = time

        if (lastTime > createAtTime) {
            createAtTime = lastTime
        }
    }


    internal fun createAtTime(): Long {
        return createAtTime
    }

    /**
     * Returns the number of playing plus queued animations
     * @return number of playing plus queued animations
     */
    fun animationCount(): Int {
        return animationKeys!!.size
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

    fun animate(variable: KMutableProperty0<*>, target: Double, durationMillis: Long,
                easing: Easing = Easing.None) {
        animate(variable.name, target, durationMillis, easing)
    }
}
