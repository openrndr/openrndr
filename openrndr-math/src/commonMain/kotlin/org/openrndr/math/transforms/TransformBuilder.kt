package org.openrndr.math.transforms

import org.openrndr.math.Matrix44
import org.openrndr.math.Quaternion
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import kotlin.jvm.JvmName
import kotlin.reflect.KMutableProperty0

class TransformBuilder(baseTransform: Matrix44 = Matrix44.IDENTITY) {
    var transform: Matrix44 = baseTransform

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

/**
 * Build a transform presented by a Matrix44
 * @param baseTransform the transform to start with, default is an identity matrix
 * @param builder a function that is invoke inside the [TransformBuilder] context
 */
fun transform(baseTransform: Matrix44 = Matrix44.IDENTITY, builder:TransformBuilder.() -> Unit):Matrix44 {
    return TransformBuilder(baseTransform).apply { builder() }.transform
}

/**
 * An alias for transform, useful for those cases in which the word transform is used too often
 */
fun buildTransform(baseTransform: Matrix44 = Matrix44.IDENTITY, builder: TransformBuilder.() -> Unit) = transform(baseTransform, builder)

/**
 * Matrix44 transform helper
 */
@JvmName("matrix44Transform")
fun Matrix44.transform(builder: TransformBuilder.() -> Unit) : Matrix44 {
    return buildTransform(baseTransform = this, builder = builder)
}

/**
 * Matrix44 property transform helper
 */
fun KMutableProperty0<Matrix44>.transform(builder: TransformBuilder.() -> Unit) {
    set(get().transform(builder))
}

