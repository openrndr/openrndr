package org.openrndr.internal.gl3

import org.openrndr.Display

class DisplayGLFWGL3 internal constructor(
    val pointer: Long,
    override val name: String?,
    override val x: Int,
    override val y: Int,
    override val width: Int?,
    override val height: Int?,
    override val contentScale: Double
) : Display() {
    override fun toString(): String {
        return "DisplayGLFWGL3(pointer=$pointer, name=$name, x=$x, y=$y, width=$width, height=$height, contentScale=$contentScale)"
    }
}