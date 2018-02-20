package org.openrndr.math.transforms

import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.rotate as _rotate
import org.openrndr.math.transforms.translate as _translate
import org.openrndr.math.transforms.scale as _scale

class TransformBuilder {
    var transform: Matrix44 = Matrix44.IDENTITY

    fun rotate(degrees:Double, axis:Vector3=Vector3.UNIT_Z) {
        transform *= _rotate(axis, degrees)
    }

    fun translate(offset:Vector3) {
        transform *= _translate(offset)
    }

    fun translate(offset: Vector2) {
        transform *= _translate(offset.xy0)
    }

    fun translate(x:Double, y:Double, z:Double = 0.0) {
        transform *= _translate(Vector3(x, y, z))
    }

    fun scale(scale:Double) {
        transform *= _scale(scale, scale, scale)
    }

    fun multiply(matrix:Matrix44) {
        transform *= matrix
    }
}

fun transform(builder:TransformBuilder.() -> Unit):Matrix44 {
    return TransformBuilder().apply { builder() }.transform
}
