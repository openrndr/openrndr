package org.openrndr.webgl
import js.typedarrays.Float32Array
import web.gl.Float32List

internal actual fun float32Array(vararg floats: Float): Float32List {
    return Float32Array(floats.toTypedArray())
}