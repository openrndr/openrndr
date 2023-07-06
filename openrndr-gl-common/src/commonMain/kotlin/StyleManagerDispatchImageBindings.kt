package org.openrndr.internal.glcommon

import org.openrndr.draw.ShaderImageBindings
import org.openrndr.draw.StyleImageBindings

interface StyleManagerDispatchImageBindings {
    var imageIndex: Int

    fun <T> dispatchImageBindings(
        style: StyleImageBindings,
        shader: T
    ) where T : ShaderImageBindings {
        imageIndex = 0
        for (it in style.imageValues.entries) {
            shader.image("p_${it.key}", imageIndex, it.value)
        }
    }
}