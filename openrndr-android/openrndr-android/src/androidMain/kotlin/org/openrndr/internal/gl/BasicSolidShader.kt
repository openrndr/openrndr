package org.openrndr.internal.gl

val BASIC_SOLID_VS = """
    #version 300 es
    layout(location=0) in vec2 a_position;   // we’ll bind both "position" and "a_position" to loc 0
    uniform vec2 u_resolution;               // framebuffer size in pixels
    void main() {
        // pixel → clip-space
        vec2 zeroToOne = a_position / u_resolution;
        vec2 zeroToTwo = zeroToOne * 2.0;
        vec2 clip = zeroToTwo - 1.0;
        gl_Position = vec4(clip * vec2(1.0, -1.0), 0.0, 1.0);
    }
""".trimIndent()

val BASIC_SOLID_FS = """
    #version 300 es
    precision mediump float;
    uniform vec4 u_fill;                     // RGBA in [0..1]
    out vec4 o_color;
    void main() {
        o_color = u_fill;
    }
""".trimIndent()