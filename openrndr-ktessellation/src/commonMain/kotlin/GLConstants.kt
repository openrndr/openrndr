@file:Suppress("unused")

package org.openrndr.ktessellation

/**
 * Provides constants for OpenGL primitive types which represent the formats
 * in which vertices can be connected to define various shapes.
 *
 * These constants are commonly used in rendering pipelines to specify
 * the topology of vertex data to the graphics system.
 */
object GLConstants {
    const val GL_POINTS = 0x0000
    const val GL_LINES = 0x0001
    const val GL_LINE_LOOP = 0x0002
    const val GL_LINE_STRIP = 0x0003
    const val GL_TRIANGLES = 0x0004
    const val GL_TRIANGLE_STRIP = 0x0005
    const val GL_TRIANGLE_FAN = 0x0006
}