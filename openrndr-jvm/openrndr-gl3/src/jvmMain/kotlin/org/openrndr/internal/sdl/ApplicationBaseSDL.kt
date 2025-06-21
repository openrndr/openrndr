package org.openrndr.internal.sdl

import io.github.oshai.kotlinlogging.KotlinLogging
import org.lwjgl.sdl.SDLHints.SDL_HINT_OPENGL_ES_DRIVER
import org.lwjgl.sdl.SDLHints.SDL_SetHint
import org.lwjgl.sdl.SDLInit.SDL_INIT_VIDEO
import org.lwjgl.sdl.SDLInit.SDL_Init
import org.lwjgl.sdl.SDLInit.SDL_Quit
import org.openrndr.Application
import org.openrndr.ApplicationBase
import org.openrndr.ApplicationConfiguration
import org.openrndr.Configuration
import org.openrndr.Display
import org.openrndr.Program
import org.openrndr.internal.gl3.DriverGL3Configuration
import org.openrndr.internal.gl3.DriverTypeGL
import org.openrndr.internal.gl3.GlesBackend
import org.openrndr.internal.gl3.angle.extractAngleLibraries

private val logger = KotlinLogging.logger { }

class ApplicationBaseSDL : ApplicationBase() {
    override val displays: List<Display>
        get() = TODO("Not yet implemented")

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
        SDL_Init(SDL_INIT_VIDEO)
    }

    override fun close() {
        SDL_Quit()
    }
}