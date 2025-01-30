@file:Suppress("SpellCheckingInspection")

package org.openrndr.ktessellation

/**
 * An adapter class for the `GLUtessellatorCallback` interface, providing default
 * empty implementations for all methods. This allows subclasses to override only the
 * methods they are interested in without the need to implement all methods of the interface.
 *
 * It is typically used with the OpenGL Utility Library (GLU) for tessellation, where
 * callbacks provide mechanisms to handle events during the tessellation process.
 */
open class GLUtessellatorCallbackAdapter : GLUtessellatorCallback {
    override fun begin(type: Int) {}
    override fun edgeFlag(boundaryEdge: Boolean) {}
    override fun vertex(vertexData: Any?) {}
    override fun end() {}

    override fun error(errnum: Int) {}
    override fun combine(coords: DoubleArray?, data: Array<Any?>?, weight: FloatArray?, outData: Array<Any?>?) {
    }

    override fun beginData(type: Int, polygonData: Any?) {}
    override fun edgeFlagData(
        boundaryEdge: Boolean,
        polygonData: Any?
    ) {
    }

    override fun vertexData(vertexData: Any?, polygonData: Any?) {}
    override fun endData(polygonData: Any?) {}
    override fun errorData(errnum: Int, polygonData: Any?) {}
    override fun combineData(
        coords: DoubleArray?,
        data: Array<Any?>?,
        weight: FloatArray?,
        outData: Array<Any?>?,
        polygonData: Any?
    ) {
    }
}
