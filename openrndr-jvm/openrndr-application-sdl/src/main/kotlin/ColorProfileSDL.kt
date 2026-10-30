package org.openrndr.application.sdl

import io.github.oshai.kotlinlogging.KotlinLogging
import org.lwjgl.sdl.SDLError.SDL_GetError
import org.lwjgl.sdl.SDLStdinc.SDL_free
import org.lwjgl.sdl.SDLVideo.SDL_GetWindowICCProfile
import org.openrndr.color.ColorProfile

private val logger = KotlinLogging.logger { }

/**
 * Reads the color profile of the display [window] is on, [ColorProfile.SRGB] when SDL has no ICC profile for it or
 * the profile is not an RGB matrix/TRC profile
 */
internal fun readWindowColorProfile(window: Long): ColorProfile {
    val buffer = SDL_GetWindowICCProfile(window)
    if (buffer == null) {
        logger.debug { "No ICC profile for window $window: ${SDL_GetError()}" }
        return ColorProfile.SRGB
    }
    // an absolute read keeps the position of the buffer, SDL_free frees the memory at its position
    val bytes = ByteArray(buffer.remaining())
    buffer.get(buffer.position(), bytes)
    SDL_free(buffer)
    return ColorProfile.fromICC(bytes) ?: run {
        logger.warn { "The ICC profile of window $window is not an RGB matrix/TRC profile, reporting sRGB" }
        ColorProfile.SRGB
    }
}
