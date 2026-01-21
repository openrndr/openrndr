package org.openrndr.draw

import kotlin.jvm.JvmInline

@JvmInline
value class TextureBindings(val binding: MutableMap<Int, Texture> = mutableMapOf()) {
    operator fun set(unit: Int, texture: Texture?) {
        if (texture == null) {
            binding.remove(unit)
        } else {
        binding[unit] = texture}
    }
}