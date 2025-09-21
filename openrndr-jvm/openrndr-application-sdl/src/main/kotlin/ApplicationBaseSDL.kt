package org.openrndr.application.sdl

import io.github.oshai.kotlinlogging.KotlinLogging
import org.lwjgl.sdl.SDLHints.SDL_HINT_OPENGL_ES_DRIVER
import org.lwjgl.sdl.SDLHints.SDL_HINT_TRACKPAD_IS_TOUCH_ONLY
import org.lwjgl.sdl.SDLHints.SDL_SetHint
import org.lwjgl.sdl.SDLInit.SDL_INIT_VIDEO
import org.lwjgl.sdl.SDLInit.SDL_Init
import org.lwjgl.sdl.SDLInit.SDL_Quit
import org.lwjgl.sdl.SDLStdinc.SDL_free
import org.lwjgl.sdl.SDLVideo.SDL_GetDesktopDisplayMode
import org.lwjgl.sdl.SDLVideo.SDL_GetDisplayBounds
import org.lwjgl.sdl.SDLVideo.SDL_GetDisplayName
import org.lwjgl.sdl.SDLVideo.SDL_GetDisplays
import org.lwjgl.sdl.SDL_Rect
import org.openrndr.Application
import org.openrndr.ApplicationBase
import org.openrndr.ApplicationConfiguration
import org.openrndr.Configuration
import org.openrndr.Display
import org.openrndr.Program
import org.openrndr.draw.font.internal.FontDriver
import org.openrndr.fontdriver.stb.FontDriverStbTt
import org.openrndr.internal.ImageDriver
import org.openrndr.internal.gl3.DriverGL3Configuration
import org.openrndr.internal.gl3.DriverTypeGL
import org.openrndr.internal.gl3.GlesBackend
import org.openrndr.internal.gl3.ImageDriverStbImage
import org.openrndr.internal.gl3.angle.extractAngleLibraries

private val logger = KotlinLogging.logger { }

class ApplicationBaseSDL : ApplicationBase() {
    private val realDisplays = mutableListOf<Display>()
    override val displays: List<Display> = realDisplays

    override fun build(
        program: Program,
        configuration: Configuration
    ): Application {
        return ApplicationSDL(program, configuration)
    }

    init {
        logger.debug { "initializing ApplicationBaseSDL" }
        if (!ApplicationConfiguration.checkThread0) {
            org.lwjgl.system.Configuration.GLFW_CHECK_THREAD0.set(false)
        }
        if (DriverGL3Configuration.driverType == DriverTypeGL.GLES && DriverGL3Configuration.glesBackend == GlesBackend.ANGLE) {
            extractAngleLibraries()
            SDL_SetHint(SDL_HINT_OPENGL_ES_DRIVER, "1")
        }

        SDL_SetHint(SDL_HINT_TRACKPAD_IS_TOUCH_ONLY, "1")
        SDL_Init(SDL_INIT_VIDEO)

        val displays = SDL_GetDisplays()

        if (displays != null) {
            for (i in 0 until displays.capacity()) {
                val mode = SDL_GetDesktopDisplayMode(displays[i])!!
                val rect = SDL_Rect.create()
                SDL_GetDisplayBounds(displays[i], rect)
                val name = SDL_GetDisplayName(displays[i])

                val dp = DisplaySDL(
                    displays[i].toLong(),
                    name,
                    rect.x(),
                    rect.y(),
                    mode.w(),
                    mode.h(),
                    mode.pixel_density().toDouble()
                )

                realDisplays.add(dp)
            }
            SDL_free(displays)
        }
        ImageDriver.driver = ImageDriverStbImage()
        FontDriver.driver = FontDriverStbTt()
    }

     fun close() {
        SDL_Quit()
    }
}