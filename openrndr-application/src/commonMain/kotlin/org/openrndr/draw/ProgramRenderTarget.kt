package org.openrndr.draw

import org.openrndr.Program

/**
 * Represents a special type of [RenderTarget] that is directly tied to a [Program].
 * The rendering operations target the screen or window surface associated with the
 * [Program]. The dimensions of this render target are directly derived from the
 * width and height of the [Program].
 */
interface ProgramRenderTarget : RenderTarget {
    val program: Program
    override val width get() = program.width
    override val height get() = program.height

    /**
     * The type of the window's color buffer. HDR windows have a floating point color buffer, [ColorType.FLOAT16] or
     * [ColorType.FLOAT32], that holds linear colors in which 1.0 is SDR white. Other windows have a
     * [ColorType.UINT8_SRGB] color buffer.
     */
    val colorType: ColorType get() = ColorType.UINT8_SRGB
}