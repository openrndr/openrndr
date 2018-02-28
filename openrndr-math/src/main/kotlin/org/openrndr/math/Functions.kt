package org.openrndr.math

fun mod(a:Double, b:Double) : Double {
    return ((a%b)+b)%b
}

fun mod(a:Int, b:Int) : Int {
    return ((a%b)+b)%b
}

fun mod(a:Float, b:Float) : Float {
    return ((a%b)+b)%b
}

fun mod(a:Long, b:Long) : Long {
    return ((a%b)+b)%b
}

fun clamp(value: Double, min: Double, max: Double): Double {
    return Math.max(min, Math.min(max, value))
}
