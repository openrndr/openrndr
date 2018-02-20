package org.openrndr.math.transforms

import org.openrndr.math.Matrix44
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4

fun ortho(left: Double, right: Double, bottom: Double, top: Double, near: Double, far: Double): Matrix44 {

    val tx = -(right + left) / (right - left)
    val ty = -(top + bottom) / (top - bottom)
    val tz = -(far + near) / (far - near)

    return Matrix44(
            2.0 / (right - left), 0.0, 0.0, tx,
            0.0, 2.0 / (top - bottom), 0.0, ty,
            0.0, 0.0, -2 / (far - near), tz,
            0.0, 0.0, 0.0, 1.0)
}

fun perspectiveDegrees(fovy: Double, aspect: Double, near: Double, far: Double, xoff: Double, yoff: Double): Matrix44 {
    val fH = Math.tan(fovy / 360 * Math.PI) * near
    val fW = fH * aspect
    return frustum(-fW + xoff, fW + xoff, -fH + yoff, fH + yoff, near, far)
}

fun perspectiveHorizontalDegrees(fovy: Double, aspect: Double, near: Double, far: Double, xoff: Double, yoff: Double): Matrix44 {

    val fW = Math.tan(fovy / 360 * Math.PI) * near
    val fH = fW / aspect

    return frustum(-fW + xoff, fW + xoff, -fH + yoff, fH + yoff, near, far)
}

// from: http://www.opengl.org/sdk/docs/man2/xhtml/gluLookAt.xml
fun lookAt(eye: Vector3, center: Vector3, up: Vector3): Matrix44 {
    val parts = lookAtParts(eye, center, up)
    return parts[0] * parts[1]
}


fun lookAtParts(eye: Vector3, center: Vector3, up: Vector3): Array<Matrix44> {

    var nup = up.normalized

    if (Math.abs(up dot (eye.normalized)) > 0.9999) {
        nup = Vector3(up.x, up.z, up.y)
    }

    val za = center.minus(eye).normalized
    val xa = nup.cross(za).normalized
    val ya = za.cross(xa).normalized

    val re = eye * -1.0

    val t = Matrix44(
            1.0, 0.0, 0.0, re.x,
            0.0, 1.0, 0.0, re.y,
            0.0, 0.0, 1.0, re.z,
            0.0, 0.0, 0.0, 1.0)

    val m = Matrix44(
            xa.x, ya.x, -za.x, 0.0,
            xa.y, ya.y, -za.y, 0.0,
            xa.z, ya.z, -za.z, 0.0,
            0.0, 0.0, 0.0, 1.0).transposed

    return arrayOf(m, t)

}


fun frustum(left: Double, right: Double, bottom: Double, top: Double, near: Double, far: Double): Matrix44 {

    val a = (right + left) / (right - left)
    val b = (top + bottom) / (top - bottom)
    val c = -(far + near) / (far - near)
    val d = -(2.0 * far * near) / (far - near)

    return Matrix44(
            2 * near / (right - left), 0.0, a, 0.0,
            0.0, 2 * near / (top - bottom), b, 0.0,
            0.0, 0.0, c, d,
            0.0, 0.0, -1.0, 0.0)
}

fun translate(translation: Vector3): Matrix44 {
    return Matrix44.fromColumnVectors(
            Vector4.UNIT_X,
            Vector4.UNIT_Y,
            Vector4.UNIT_Z,
            Vector4(translation.x, translation.y, translation.z, 1.0))
}

// from: http://en.wikipedia.org/wiki/Rotation_matrix
fun rotate(axis: Vector3, d: Double): Matrix44 {
    val r = Math.toRadians(d)
    val cr = Math.cos(r)
    val sr = Math.sin(r)

    val ux = axis.x
    val uy = axis.y
    val uz = axis.z

    val c0 = Vector4(cr + axis.x * axis.x * (1 - cr),
            axis.y * axis.x * (1 - cr) + axis.z * sr,
            axis.z * axis.x * (1 - cr) - axis.y * sr, 0.0)

    val c1 = Vector4(ux * uy * (1 - cr) - uz * sr,
            cr + uy * uy * (1 - cr),
            uz * uy * (1 - cr) + ux * sr, 0.0)

    val c2 = Vector4(ux * uz * (1 - cr) + uy * sr,
            uy * uz * (1 - cr) - ux * sr,
            cr + uz * uz * (1 - cr), 0.0)

    return Matrix44.fromColumnVectors(c0, c1, c2, Vector4.UNIT_W)

}


fun rotateX(d: Double): Matrix44 {
    val r = Math.toRadians(d)

    val cr = Math.cos(r)
    val sr = Math.sin(r)
    return Matrix44(
            1.0, 0.0, 0.0, 0.0,
            0.0, cr, -sr, 0.0,
            0.0, sr, cr, 0.0,
            0.0, 0.0, 0.0, 1.0
    )
}

fun rotateY(d: Double): Matrix44 {
    val r = Math.toRadians(d)

    val cr = Math.cos(r)
    val sr = Math.sin(r)
    return Matrix44(
            cr, 0.0, sr, 0.0,
            0.0, 1.0, 0.0, 0.0,
            -sr, 0.0, cr, 0.0,
            0.0, 0.0, 0.0, 1.0)
}


fun rotateZ(d: Double): Matrix44 {
    val r = Math.toRadians(d)
    val cr = Math.cos(r)
    val sr = Math.sin(r)
    return Matrix44(
            cr, -sr, 0.0, 0.0,
            sr, cr, 0.0, 0.0,
            0.0, 0.0, 1.0, 0.0,
            0.0, 0.0, 0.0, 1.0)
}

fun scale(x: Double = 1.0, y: Double, z: Double): Matrix44 {
    return Matrix44(
            x, 0.0, 0.0, 0.0,
            0.0, y, 0.0, 0.0,
            0.0, 0.0, z, 0.0,
            0.0, 0.0, 0.0, 1.0)
}

fun unproject(point: Vector3, projection: Matrix44, view: Matrix44, width: Int, height: Int): Vector3 {
    val ipm = (projection * view).inversed
    val v = Vector3(2 * point.x / width - 1, 2 * point.y / height - 1, 2 * point.z - 1)
    return (ipm * v)
}


fun project(point: Vector3, projection: Matrix44, view: Matrix44, width: Int, height: Int): Vector3 {

    val homo = Vector4(point.x, point.y, point.z, 1.0)
    val projectedHomo = (projection * view) * homo

    // Homogeneous division
    val rhw = 1 / projectedHomo.w

    return Vector3((projectedHomo.x * rhw + 1) * width / 2,
            (1 - projectedHomo.y * rhw) * height / 2,
            rhw)
}

fun normalMatrix(view:Matrix44) : Matrix44 {

    val subView = Matrix44(
            view.c0r0, view.c1r0, view.c2r0, 0.0,
            view.c0r1, view.c1r1, view.c2r1, 0.0,
            view.c0r2, view.c1r2, view.c2r2, 0.0,
            0.0, 0.0, 0.0, 1.0)

    return subView.inversed.transposed.copy(c3r3=0.0)

}

