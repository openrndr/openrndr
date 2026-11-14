package org.openrndr.internal.glcommon

import org.openrndr.draw.ShaderImageBindings
import org.openrndr.draw.StyleImageBindings

interface StyleManagerDispatchImageBindings {
    var imageIndex: Int

    /** the names of the uniforms of image bindings */
    val uniformNames: UniformNames

    fun <T> dispatchImageBindings(
        style: StyleImageBindings,
        shader: T
    ) where T : ShaderImageBindings {
        val imageValues = style.imageValues
        if (imageValues.isEmpty()) return
        for (it in imageValues.entries) {
            val name = uniformNames.parameter(it.key)
            if (style.imageArrayLength[it.key] == -1) {
                shader.image(name, style.imageBindings[it.key] ?: -1, it.value.first())
            } else {
                shader.image(name, style.imageBindings[it.key] ?: -1, it.value)
            }
        }
    }
}