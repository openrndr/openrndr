package org.openrndr.webgl

import js.buffer.ArrayBuffer
import js.numbers.JsNumbers.toJsFloat
import web.gl.Float32List

internal actual fun float32Array(vararg floats: Float): Float32List {
    val ab = ArrayBuffer(floats.size * 4)
    val fb = Float32List(ab, 0, floats.size)
    for (i in floats.indices) {
        fb[i] = floats[i].toJsFloat()
    }
    return fb
}