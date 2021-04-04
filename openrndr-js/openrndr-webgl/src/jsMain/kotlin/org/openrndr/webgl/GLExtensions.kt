package org.openrndr.webgl

import org.khronos.webgl.WebGLRenderingContext

external interface ANGLEinstancedArrays {
    fun vertexAttribDivisorANGLE(index: Int, divisor: Int)
    fun drawArraysInstancedANGLE(mode: Int, first: Int, count: Int, primcount: Int)
    fun drawElementsInstancedANGLE(mode: Int, count: Int, type: Int, offset: Int, primcount: Int)
}

