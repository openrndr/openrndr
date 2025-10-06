package org.openrndr.webgl
import js.buffer.ArrayBuffer
import js.typedarrays.Float32Array

internal actual fun float32Array(vararg floats: Float): Float32Array<ArrayBuffer> {
    return Float32Array(floats.toTypedArray())
}