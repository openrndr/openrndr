package org.openrndr.math.transforms

import org.openrndr.math.Matrix44
import org.openrndr.math.Quaternion
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3

class TransformBuilder {
    var transform: Matrix44 = Matrix44.IDENTITY

    fun rotate(axis: Quaternion) {
        transform *= axis.matrix.matrix44
    }

    fun rotate(axis:Vector3=Vector3.UNIT_Z, degrees:Double) {
        transform *= Matrix44.rotate(axis, degrees)
    }

    fun translate(offset:Vector3) {
        transform *= Matrix44.translate(offset)
    }

    fun translate(offset: Vector2) {
        transform *= Matrix44.translate(offset.xy0)
    }

    fun translate(x:Double, y:Double, z:Double = 0.0) {
        transform *= Matrix44.translate(Vector3(x, y, z))
    }

    fun scale(scale:Double) {
        transform *= Matrix44.scale(scale, scale, scale)
    }

    fun scale(scaleX:Double, scaleY:Double, scaleZ:Double) {
        transform *= Matrix44.scale(scaleX, scaleY, scaleZ)
    }

    fun scale(scale:Vector3) {
        transform *= Matrix44.scale(scale.x, scale.y, scale.z)
    }

    fun multiply(matrix:Matrix44) {
        transform *= matrix
    }
}

fun transform(builder:TransformBuilder.() -> Unit):Matrix44 {
    return TransformBuilder().apply { builder() }.transform
}
