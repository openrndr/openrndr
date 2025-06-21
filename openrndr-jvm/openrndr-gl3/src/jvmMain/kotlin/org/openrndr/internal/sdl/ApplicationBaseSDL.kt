package org.openrndr.internal.sdl

import io.github.oshai.kotlinlogging.KotlinLogging
import org.lwjgl.sdl.*
import org.lwjgl.sdl.SDLInit.SDL_INIT_VIDEO
import org.lwjgl.sdl.SDLInit.SDL_Init
import org.lwjgl.sdl.SDLInit.SDL_Quit
import org.lwjgl.sdl.SDLVideo.SDL_GL_ACCELERATED_VISUAL
import org.lwjgl.sdl.SDLVideo.SDL_GL_ALPHA_SIZE
import org.lwjgl.sdl.SDLVideo.SDL_GL_BLUE_SIZE
import org.lwjgl.sdl.SDLVideo.SDL_GL_CONTEXT_FLAGS
import org.lwjgl.sdl.SDLVideo.SDL_GL_CONTEXT_FORWARD_COMPATIBLE_FLAG
import org.lwjgl.sdl.SDLVideo.SDL_GL_CONTEXT_MAJOR_VERSION
import org.lwjgl.sdl.SDLVideo.SDL_GL_CONTEXT_MINOR_VERSION
import org.lwjgl.sdl.SDLVideo.SDL_GL_CONTEXT_PROFILE_CORE
import org.lwjgl.sdl.SDLVideo.SDL_GL_CONTEXT_PROFILE_MASK
import org.lwjgl.sdl.SDLVideo.SDL_GL_DOUBLEBUFFER
import org.lwjgl.sdl.SDLVideo.SDL_GL_GREEN_SIZE
import org.lwjgl.sdl.SDLVideo.SDL_GL_RED_SIZE
import org.lwjgl.sdl.SDLVideo.SDL_GL_SetAttribute
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
import org.openrndr.internal.glfw.ApplicationGLFW

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
        logger.debug { "initializing ApplicationBaseGLFW" }
        if (!ApplicationConfiguration.checkThread0) {
            org.lwjgl.system.Configuration.GLFW_CHECK_THREAD0.set(false)
        }

        if (DriverGL3Configuration.driverType == DriverTypeGL.GLES && DriverGL3Configuration.glesBackend == GlesBackend.ANGLE) {
            extractAngleLibraries()
        }
        SDL_GL_SetAttribute( SDL_GL_DOUBLEBUFFER, 1 );
        SDL_GL_SetAttribute( SDL_GL_ACCELERATED_VISUAL, 1 );
        SDL_GL_SetAttribute( SDL_GL_RED_SIZE, 8 );
        SDL_GL_SetAttribute( SDL_GL_GREEN_SIZE, 8 );
        SDL_GL_SetAttribute( SDL_GL_BLUE_SIZE, 8 );
        SDL_GL_SetAttribute( SDL_GL_ALPHA_SIZE, 8 );
        SDL_GL_SetAttribute(SDL_GL_CONTEXT_PROFILE_MASK, SDL_GL_CONTEXT_PROFILE_CORE)
        SDL_GL_SetAttribute(SDL_GL_CONTEXT_FLAGS, SDL_GL_CONTEXT_FORWARD_COMPATIBLE_FLAG)
        SDL_GL_SetAttribute(SDL_GL_CONTEXT_MAJOR_VERSION, 4)
        SDL_GL_SetAttribute(SDL_GL_CONTEXT_MINOR_VERSION, 0)

        SDL_GL_SetAttribute(SDL_GL_CONTEXT_PROFILE_MASK, SDL_GL_CONTEXT_PROFILE_CORE)

        SDL_Init(SDL_INIT_VIDEO)
    }

    override fun close() {
        SDL_Quit()
    }
}