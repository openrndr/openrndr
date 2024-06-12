package org.openrndr.math.transforms

import org.openrndr.math.Matrix44
import org.openrndr.math.Quaternion
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.jvm.JvmName
import kotlin.reflect.KMutableProperty0

/**
 * Build a [Matrix44] transform
 */
class TransformBuilder(baseTransform: Matrix44 = Matrix44.IDENTITY) {
    var transform: Matrix44 = baseTransform

    /**
     * rotate [degreesInAngles] around [Vector3.UNIT_Z]
     */
    fun rotate(degreesInAngles: Double) = rotate(Vector3.UNIT_Z, degreesInAngles)

    /**
     * rotate by [quaternion]
     */
    fun rotate(quaternion: Quaternion) {
        if (quaternion !== Quaternion.IDENTITY) {
            transform *= quaternion.matrix.matrix44
        }
    }

    fun rotate(axis: Vector3 = Vector3.UNIT_Z, degrees: Double) {
        if (degrees != 0.0) {
            transform *= Matrix44.rotate(axis, degrees)
        }
    }

    /**
     * translate by [offset]
     */
    fun translate(offset: Vector3) {
        if (offset !== Vector3.ZERO) {
            transform *= Matrix44.translate(offset)
        }
    }

    /**
     * translate by [offset]
     */
    fun translate(offset: Vector2) {
        if (offset !== Vector2.ZERO) {
            transform *= Matrix44.translate(offset.xy0)
        }
    }

    /**
     * translate by [x], [y], [z]
     */
    fun translate(x: Double, y: Double, z: Double = 0.0) {
        transform *= Matrix44.translate(Vector3(x, y, z))
    }

    /**
     * scale by [scale]
     */
    fun scale(scale: Double) {
        if (scale != 1.0) {
            transform *= Matrix44.scale(scale, scale, scale)
        }
    }

    fun scale(scaleX: Double, scaleY: Double, scaleZ: Double = 1.0) {
        transform *= Matrix44.scale(scaleX, scaleY, scaleZ)
    }

    /**
     * scale by [scale]
     */
    fun scale(scale: Vector3) {
        if (scale !== Vector3.ONE) {
            transform *= Matrix44.scale(scale.x, scale.y, scale.z)
        }
    }

    /**
     * multiply by [matrix]
     */
    fun multiply(matrix: Matrix44) {
        transform *= matrix
    }
}

/**
 * Build a transform presented by a Matrix44
 * @param baseTransform the transform to start with, default is an identity matrix
 * @param builder a function that is invoke inside the [TransformBuilder] context
 */
@OptIn(ExperimentalContracts::class)
fun transform(baseTransform: Matrix44 = Matrix44.IDENTITY, builder: TransformBuilder.() -> Unit): Matrix44 {
    contract {
        callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
    }
    return TransformBuilder(baseTransform).apply { builder() }.transform
}

/**
 * An alias for transform, useful for those cases in which the word transform is used too often
 */
fun buildTransform(baseTransform: Matrix44 = Matrix44.IDENTITY, builder: TransformBuilder.() -> Unit) =
    transform(baseTransform, builder)

/**
 * Matrix44 transform helper
 */
@OptIn(ExperimentalContracts::class)
@JvmName("matrix44Transform")
fun Matrix44.transform(builder: TransformBuilder.() -> Unit): Matrix44 {
    contract {
        callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
    }
    return buildTransform(baseTransform = this, builder = builder)
}

/**
 * Matrix44 property transform helper
 */
@OptIn(ExperimentalContracts::class)
fun KMutableProperty0<Matrix44>.transform(builder: TransformBuilder.() -> Unit) {
    contract {
        callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
    }
    set(get().transform(builder))
}

