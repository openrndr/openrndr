package org.openrndr.webgl

import js.buffer.ArrayBuffer
import js.core.JsPrimitives.toJsFloat
import js.typedarrays.Float32Array

internal actual fun float32Array(vararg floats: Float): Float32Array<ArrayBuffer> {
    val ab = ArrayBuffer(floats.size * 4)
    val fb = Float32Array(ab, 0, floats.size)
    for (i in floats.indices) {
        fb[i] = floats[i].toJsFloat()
    }
    return fb
}