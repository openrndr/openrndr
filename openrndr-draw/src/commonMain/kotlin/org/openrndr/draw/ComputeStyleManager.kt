package org.openrndr.draw

abstract class ComputeStyleManager {
    abstract fun shader(
        style: ComputeStyle,
        name: String
    ): ComputeShader
}