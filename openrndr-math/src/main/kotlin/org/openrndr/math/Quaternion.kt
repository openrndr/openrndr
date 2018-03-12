package org.openrndr.math


data class Quaternion(val x: Double, val y: Double, val z: Double, val w: Double) {


    companion object {
        val IDENTITY: Quaternion = Quaternion(0.0, 0.0, 0.0, 1.0)
        val ZERO: Quaternion = Quaternion(0.0, 0.0, 0.0, 0.0)
    }

    val length: Double get() = Math.sqrt(x * x + y * y + z * z + w * w)

    operator fun times(q: Quaternion): Quaternion {
        return Quaternion(
                x * q.w + y * q.z - z * q.y + w * q.x,
                -x * q.z + y * q.w + z * q.x + w * q.y,
                x * q.y - y * q.x + z * q.w + w * q.z,
                -x * q.x - y * q.y - z * q.z + w * q.w)

    }


    val negated: Quaternion
        get() {
            return Quaternion(-x, -y, -z, -w)
        }

    val inversed: Quaternion
        get() {
            val n = norm
            if (n > 0.0) {
                val invNorm = 1.0 / n
                return Quaternion(-x * invNorm, -y * invNorm, -z * invNorm, w * invNorm)
            } else {
                throw RuntimeException("norm <= 0 => quaternion is not invertible")
            }
        }

    val normalized: Quaternion
        get() {

            val l = length
            return Quaternion(x / l, y / l, z / l, w / l)
        }

    val norm: Double get() = x * x + y * y + z * z + w * w

    val matrix: Matrix44
        get() {
            val norm = this.norm
            // we explicitly test norm against one here, saving a division
            // at the cost of a test and branch. Is it worth it?
            val s = if (norm == 1.0) 2.0 else if (norm > 0f) 2f / norm else 0.0

            // compute xs/ys/zs first to save 6 multiplications, since xs/ys/zs
            // will be used 2-4 times each.
            val xs = x * s
            val ys = y * s
            val zs = z * s
            val xx = x * xs
            val xy = x * ys
            val xz = x * zs
            val xw = w * xs
            val yy = y * ys
            val yz = y * zs
            val yw = w * ys
            val zz = z * zs
            val zw = w * zs

            // using s=2/norm (instead of 1/norm) saves 9 multiplications by 2 here
            return Matrix44( //
                    1 - (yy + zz), xy - zw, xz + yw, 0.0, //
                    xy + zw, 1 - (xx + zz), yz - xw, 0.0, //
                    xz - yw, yz + xw, 1 - (xx + yy), 0.0, //
                    0.0, 0.0, 0.0, 1.0)

        }

}

fun dot(q1:Quaternion, q2:Quaternion):Double {
    return q1.w*q2.w + q1.x*q2.x + q1.y * q2.y + q2.z*q2.z
}

fun slerp(q1: Quaternion, q2: Quaternion, x: Double): Quaternion {
    if (q1.x == q2.x && q1.y == q2.y && q1.z == q2.z && q1.w == q2.w) {
        return q1
    }


    var q2e = q2
    var result = (q1.x * q2.x + q1.y * q2.y + q1.z * q2.z
            + q1.w * q2.w)
    if (result < 0.0f) {
        // Negate the second quaternion and the result of the dot product
        q2e = q2.negated
        result = -result
    }

    // Set the first and second scale for the interpolation
    var scale0 = 1 - x
    var scale1 = x
    if (1 - result > 0.1f) {
        // Get the angle between the 2 quaternions, and then store the sin()
        // of that angle
        val theta = Math.acos(result)
        val invSinTheta = 1f / Math.sin(theta)

        // Calculate the scale for q1 and q2, according to the angle and
        // it's sine value
        scale0 = Math.sin((1 - x) * theta) * invSinTheta
        scale1 = Math.sin(x * theta) * invSinTheta
    }
    return Quaternion(
            scale0 * q1.x + scale1 * q2e.x,
            scale0 * q1.y + scale1 * q2e.y,
            scale0 * q1.z + scale1 * q2e.z,
            scale0 * q1.w + scale1 * q2e.w)
}

fun fromAngles(pitch: Double, roll: Double, yaw: Double): Quaternion {

    val cy = Math.cos(yaw * 0.5)
    val sy = Math.sin(yaw * 0.5)
    val cr = Math.cos(roll * 0.5)
    val sr = Math.sin(roll * 0.5)
    val cp = Math.cos(pitch * 0.5)
    val sp = Math.sin(pitch * 0.5)

    return Quaternion(

    cy * sr * cp - sy * cr * sp,
    cy * cr * sp + sy * sr * cp,
    sy * cr * cp - cy * sr * sp,
            cy * cr * cp + sy * sr * sp)

}
