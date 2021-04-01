package org.openrndr.math

import org.amshove.kluent.should
import kotlin.math.absoluteValue

fun Double.closeTo(expected: Double, delta: Double) = this.should("\n[$this]\nand\n[$expected]\nshould be equal within an accuracy of $delta") {
    ((this - expected).absoluteValue - delta) <= 0.0
}

fun Vector2.closeTo(expected: Vector2, delta: Double) = this.should("\n[$this]\nand\n[$expected]\nshould be equal within an accuracy of $delta") {
    ((this.x - expected.x).absoluteValue - delta) <= 0.0 &&
            ((this.y - expected.y).absoluteValue - delta) <= 0.0
}

fun Vector3.closeTo(expected: Vector3, delta: Double) = this.should("\n[$this]\nand\n[$expected]\nshould be equal within an accuracy of $delta") {
    ((this.x - expected.x).absoluteValue - delta) <= 0.0 &&
            ((this.y - expected.y).absoluteValue - delta) <= 0.0 &&
            ((this.z - expected.z).absoluteValue - delta) <= 0.0
}

fun Vector4.closeTo(expected: Vector4, delta: Double) = this.should("\n[$this]\nand\n[$expected]\nshould be equal within an accuracy of $delta") {
    ((this.x - expected.x).absoluteValue - delta) <= 0.0 &&
            ((this.y - expected.y).absoluteValue - delta) <= 0.0 &&
            ((this.z - expected.z).absoluteValue - delta) <= 0.0 &&
            ((this.w - expected.w).absoluteValue - delta) <= 0.0
}

fun Matrix44.closeTo(expected: Matrix44, delta: Double) = this.should("\n[$this]\nand\n[$expected]\nshould be equal within an accuracy of $delta") {

    ((this.c0r0 - expected.c0r0).absoluteValue - delta) <= 0.0 &&
            ((this.c0r1 - expected.c0r1).absoluteValue - delta) <= 0.0 &&
            ((this.c0r2 - expected.c0r2).absoluteValue - delta) <= 0.0 &&
            ((this.c0r3 - expected.c0r3).absoluteValue - delta) <= 0.0 &&
            ((this.c1r0 - expected.c1r0).absoluteValue - delta) <= 0.0 &&
            ((this.c1r1 - expected.c1r1).absoluteValue - delta) <= 0.0 &&
            ((this.c1r2 - expected.c1r2).absoluteValue - delta) <= 0.0 &&
            ((this.c1r3 - expected.c1r3).absoluteValue - delta) <= 0.0 &&
            ((this.c2r0 - expected.c2r0).absoluteValue - delta) <= 0.0 &&
            ((this.c2r1 - expected.c2r1).absoluteValue - delta) <= 0.0 &&
            ((this.c2r2 - expected.c2r2).absoluteValue - delta) <= 0.0 &&
            ((this.c2r3 - expected.c2r3).absoluteValue - delta) <= 0.0 &&
            ((this.c3r0 - expected.c3r0).absoluteValue - delta) <= 0.0 &&
            ((this.c3r1 - expected.c3r1).absoluteValue - delta) <= 0.0 &&
            ((this.c3r2 - expected.c3r2).absoluteValue - delta) <= 0.0 &&
            ((this.c3r3 - expected.c3r3).absoluteValue - delta) <= 0.0
}
