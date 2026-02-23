package org.openrndr.internal.glcommon

import io.github.oshai.kotlinlogging.KotlinLogging
import org.openrndr.draw.ShaderImageBindings
import org.openrndr.draw.StyleImageBindings

private val logger = KotlinLogging.logger {  }

interface StyleManagerDispatchImageBindings {
    var imageIndex: Int

    fun <T> dispatchImageBindings(
        style: StyleImageBindings,
        shader: T
    ) where T : ShaderImageBindings {
        if ( style.imageValues.isNotEmpty()) {
            logger.debug { "dispatching image bindings, ${style.imageValues}" }
        }
        for (it in style.imageValues.entries) {
            if (style.imageArrayLength[it.key] == -1) {
                shader.image("p_${it.key}", style.imageBindings[it.key] ?: -1, it.value.first())
            } else {
                shader.image("p_${it.key}", style.imageBindings[it.key] ?: -1, it.value)
            }
        }
    }
}