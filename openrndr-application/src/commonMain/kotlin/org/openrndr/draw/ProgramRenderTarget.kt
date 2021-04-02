package org.openrndr.draw

import org.openrndr.Program

/**
 * A render target that wraps around the back-buffer
 */
interface ProgramRenderTarget : RenderTarget {
    val program: Program
    override val width get() = program.width
    override val height get() = program.height
}