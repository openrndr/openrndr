package org.openrndr.math

import java.io.Serializable
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Quaternion class for representing orientations in 3D space
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
data class Quaternion(val x: Double, val y: Double, val z: Double, val w: Double) : Serializable {

    companion object {
        val IDENTITY: Quaternion = Quaternion(0.0, 0.0, 0.0, 1.0)
        val ZERO: Quaternion = Quaternion(0.0, 0.0, 0.0, 0.0)

        /**
         * Construct [Quaternion] using from, to and up vectors
         */
        fun fromLookAt(from: Vector3, to: Vector3, up: Vector3 = Vector3.UNIT_Y): Quaternion {
            val direction = to - from
            val z = direction.normalized
            val y = up.normalized
            val x = (y cross z).normalized
            val y2 = (z cross x).normalized
            return fromAxes(x, y2, z).normalized
        }

        /**
         * Construct [Quaternion] from axes
         */
        fun fromAxes(x: Vector3, y: Vector3, z: Vector3): Quaternion {
            val m = Matrix33.fromColumnVectors(x, y, z)
            return fromMatrix(m)
        }

        /**
         * Construct [Quaternion] from [Matrix33]
         * @param m a [Matrix33] that describes an ortho-normal basis
         */
        fun fromMatrix(m: Matrix33): Quaternion {
            val t = m.trace + 1.0
            val x: Double
            val y: Double
            val z: Double
            val w: Double
            if (t > 0) {
                val s = 0.5 / sqrt(t)
                w = 0.25 / s
                x = (m.c1r2 - m.c2r1) * s
                y = (m.c2r0 - m.c0r2) * s
                z = (m.c0r1 - m.c1r0) * s
            } else if (m.c0r0 > m.c1r1 && m.c0r0 > m.c2r2) {
                val s = 0.5 / sqrt(1.0 + m.c0r0 - m.c1r1 - m.c2r2) // S=4*qx
                w = (m.c1r2 - m.c2r1) * s
                x = 0.25f / s
                y = (m.c0r1 + m.c1r0) * s
                z = (m.c2r0 + m.c0r2) * s
            } else if (m.c1r1 > m.c2r2) {
                val s = 0.5f / sqrt(1.0 + m.c1r1 - m.c0r0 - m.c2r2) // S=4*qy
                w = (m.c2r0 - m.c0r2) * s
                x = (m.c0r2 + m.c1r0) * s
                y = 0.25f / s
                z = (m.c1r2 + m.c2r1) * s
            } else {
                val s = 0.5f / sqrt(1.0 + m.c2r2 - m.c0r0 - m.c1r1) // S=4*qz
                w = (m.c0r1 - m.c1r0) * s
                x = (m.c2r0 + m.c0r2) * s
                y = (m.c1r2 + m.c2r1) * s
                z = 0.25f / s
            }
            return Quaternion(x, y, z, w)
        }

        /**
         * Construct [Quaternion] from a set of degrees
         */
        fun fromAngles(pitch: Double, roll: Double, yaw: Double) =
                fromAnglesRadian(Math.toRadians(pitch), Math.toRadians(roll), Math.toRadians(yaw))

        /**
         * Construct [Quaternion] from a set of arc lengths
         */
        fun fromAnglesRadian(pitch: Double, roll: Double, yaw: Double): Quaternion {
            val cy = cos(yaw * 0.5)
            val sy = sin(yaw * 0.5)
            val cr = cos(roll * 0.5)
            val sr = sin(roll * 0.5)
            val cp = cos(pitch * 0.5)
            val sp = sin(pitch * 0.5)

            return Quaternion(
                    cy * sr * cp - sy * cr * sp,
                    cy * cr * sp + sy * sr * cp,
                    sy * cr * cp - cy * sr * sp,
                    cy * cr * cp + sy * sr * sp)
        }
    }

    val length: Double get() = sqrt(x * x + y * y + z * z + w * w)

    operator fun times(q: Quaternion): Quaternion {
        return Quaternion(
                x * q.w + y * q.z - z * q.y + w * q.x,
                -x * q.z + y * q.w + z * q.x + w * q.y,
                x * q.y - y * q.x + z * q.w + w * q.z,
                -x * q.x - y * q.y - z * q.z + w * q.w)
    }

    operator fun times(vec: Vector3): Vector3 {
        val s = 2.0 / norm
        val xs = x * s
        val ys = y * s
        val zs = z * s
        val xxs = x * xs
        val yys = y * ys
        val zzs = z * zs
        val xys = x * ys
        val xzs = x * zs
        val yzs = y * zs
        val wxs = w * xs
        val wys = w * ys
        val wzs = w * zs

        return Vector3((1 - (yys + zzs)) * vec.x + (xys - wzs) * vec.y + (xzs + wys) * vec.z,
                (xys + wzs) * vec.x + (1 - (xxs + zzs)) * vec.y + (yzs - wxs) * vec.z,
                (xzs - wys) * vec.x + (yzs + wxs) * vec.y + (1 - (xxs + yys)) * vec.z)
    }

    val negated: Quaternion
        get() = Quaternion(-x, -y, -z, -w)

    val inversed: Quaternion
        get() {
            val n = norm
            if (n > 0.0) {
                val invNorm = 1.0 / n
                return Quaternion(-x * invNorm, -y * invNorm, -z * invNorm, w * invNorm)
            } else {
                error("norm <= 0 => quaternion is not invertible")
            }
        }

    val normalized: Quaternion
        get() {
            val l = length
            return Quaternion(x / l, y / l, z / l, w / l)
        }

    val norm: Double get() = x * x + y * y + z * z + w * w

    /**
     * An orthonormal basis for the orientation described by the quaternion
     */
    val matrix: Matrix33
        get() {
            val norm = this.norm
            // we explicitly test norm against one here, saving a division
            // at the cost of a test and branch. Is it worth it?
            val s = if (norm == 1.0) 2.0 else if (norm > 0.0) 2.0 / norm else 0.0

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
            return Matrix33(
                    1 - (yy + zz), xy - zw, xz + yw,
                    xy + zw, 1 - (xx + zz), yz - xw,
                    xz - yw, yz + xw, 1 - (xx + yy))

        }
}

@Suppress("unused")
fun dot(q1: Quaternion, q2: Quaternion): Double {
    return q1.w * q2.w + q1.x * q2.x + q1.y * q2.y + q2.z * q2.z
}

@Suppress("unused")
fun slerp(q1: Quaternion, q2: Quaternion, x: Double): Quaternion {
    if (q1.x == q2.x && q1.y == q2.y && q1.z == q2.z && q1.w == q2.w) {
        return q1
    }

    var q2e = q2
    var result = (q1.x * q2.x + q1.y * q2.y + q1.z * q2.z
            + q1.w * q2.w)
    if (result < 0.0) {
        // Negate the second quaternion and the result of the dot product
        q2e = q2.negated
        result = -result
    }

    // Set the first and second scale for the interpolation
    var scale0 = 1 - x
    var scale1 = x
    if (1 - result > 0.1) {
        // Get the angle between the 2 quaternions, and then store the sin()
        // of that angle
        val theta = acos(result)
        val invSinTheta = 1.0 / sin(theta)

        // Calculate the scale for q1 and q2, according to the angle and
        // it's sine value
        scale0 = sin((1 - x) * theta) * invSinTheta
        scale1 = sin(x * theta) * invSinTheta
    }

    return Quaternion(
            scale0 * q1.x + scale1 * q2e.x,
            scale0 * q1.y + scale1 * q2e.y,
            scale0 * q1.z + scale1 * q2e.z,
            scale0 * q1.w + scale1 * q2e.w)
}