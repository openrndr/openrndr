package org.openrndr.animatable.easing

interface Easer {
    fun velocity(t: Double, b: Double, c: Double, d: Double): Double
    fun ease(t: Double, b: Double, c: Double, d: Double): Double
}

enum class Easing(val easer:Easer) {
    None(Linear()),
    SineIn(SineIn()),
    SineOut(SineOut()),
    SineInOut(SineInOut())
}

class Linear : Easer {
    override fun velocity(t: Double, b: Double, c: Double, d: Double): Double = c / d
    override fun ease(t: Double, b: Double, c: Double, d: Double): Double = b + c * (t / d)
}

@Suppress("unused")
class BackIn : Easer {
    override fun velocity(t: Double, b: Double, c: Double, d: Double): Double {
        return c * t * (8.10474 * t - 3.40316 * d) / (d * d * d)
    }

    override fun ease(t: Double, b: Double, c: Double, d: Double): Double {
        val s = 1.70158
        val td = t/d
        return c * (td) * td * ((s + 1) * td - s) + b
    }
}

@Suppress("unused")
class ExpoIn : Easer {
    override fun velocity(t: Double, b: Double, c: Double, d: Double): Double {
        return if (t == 0.0) {
            0.0
        } else {
            c * Math.log(32.0) * Math.pow(2.0, 10 * t / d - 9) / d
        }
    }

    override fun ease(t: Double, b: Double, c: Double, d: Double): Double =
            if (t == 0.0) b else c * Math.pow(2.0, 10 * (t / d - 1)) + b
}

class SineOut : Easer {
    override fun velocity(t: Double, b: Double, c: Double, d: Double): Double = Math.PI * c * Math.cos(Math.PI * t) / (2 * d) / (2 * d)
    override fun ease(t: Double, b: Double, c: Double, d: Double): Double = c * Math.sin(t / d * (Math.PI / 2)) + b
}

class SineIn : Easer {
    override fun velocity(t: Double, b: Double, c: Double, d: Double): Double = -Math.PI * c * Math.sin(Math.PI * t / (2 * d)) / (2 * d)
    override fun ease(t: Double, b: Double, c: Double, d: Double): Double = -c * Math.cos(t / d * (Math.PI / 2)) + c + b
}

class SineInOut : Easer {
    override fun velocity(t: Double, b: Double, c: Double, d: Double): Double = Math.PI * c * Math.sin(Math.PI * t) / d / (2 * d)
    override fun ease(t: Double, b: Double, c: Double, d: Double): Double = -c / 2 * (Math.cos(Math.PI * t / d) - 1) + b
}
