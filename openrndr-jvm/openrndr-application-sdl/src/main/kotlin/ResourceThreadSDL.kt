package org.openrndr.application.sdl

import io.github.oshai.kotlinlogging.KotlinLogging
import org.lwjgl.opengl.GL
import org.lwjgl.opengles.GLES
import org.lwjgl.sdl.SDLError.SDL_GetError
import org.lwjgl.sdl.SDLVideo.SDL_CreateWindow
import org.lwjgl.sdl.SDLVideo.SDL_DestroyWindow
import org.lwjgl.sdl.SDLVideo.SDL_GL_ACCELERATED_VISUAL
import org.lwjgl.sdl.SDLVideo.SDL_GL_ALPHA_SIZE
import org.lwjgl.sdl.SDLVideo.SDL_GL_BLUE_SIZE
import org.lwjgl.sdl.SDLVideo.SDL_GL_CONTEXT_FLAGS
import org.lwjgl.sdl.SDLVideo.SDL_GL_CONTEXT_FORWARD_COMPATIBLE_FLAG
import org.lwjgl.sdl.SDLVideo.SDL_GL_CONTEXT_MAJOR_VERSION
import org.lwjgl.sdl.SDLVideo.SDL_GL_CONTEXT_MINOR_VERSION
import org.lwjgl.sdl.SDLVideo.SDL_GL_CONTEXT_PROFILE_CORE
import org.lwjgl.sdl.SDLVideo.SDL_GL_CONTEXT_PROFILE_ES
import org.lwjgl.sdl.SDLVideo.SDL_GL_CONTEXT_PROFILE_MASK
import org.lwjgl.sdl.SDLVideo.SDL_GL_CreateContext
import org.lwjgl.sdl.SDLVideo.SDL_GL_DOUBLEBUFFER
import org.lwjgl.sdl.SDLVideo.SDL_GL_DestroyContext
import org.lwjgl.sdl.SDLVideo.SDL_GL_GREEN_SIZE
import org.lwjgl.sdl.SDLVideo.SDL_GL_MakeCurrent
import org.lwjgl.sdl.SDLVideo.SDL_GL_RED_SIZE
import org.lwjgl.sdl.SDLVideo.SDL_GL_ResetAttributes
import org.lwjgl.sdl.SDLVideo.SDL_GL_SHARE_WITH_CURRENT_CONTEXT
import org.lwjgl.sdl.SDLVideo.SDL_GL_SetAttribute
import org.lwjgl.sdl.SDLVideo.SDL_WINDOW_HIDDEN
import org.lwjgl.sdl.SDLVideo.SDL_WINDOW_OPENGL
import org.lwjgl.system.MemoryUtil
import org.openrndr.color.ColorRGBa
import org.openrndr.internal.Driver
import org.openrndr.internal.ResourceThread
import org.openrndr.internal.gl3.DriverGL3
import org.openrndr.internal.gl3.DriverTypeGL
import org.openrndr.internal.gl3.NullRenderTargetGL3
import org.openrndr.internal.gl3.glType
import org.openrndr.internal.gl3.glVersion
import kotlin.concurrent.thread

private val logger = KotlinLogging.logger {  }

/**
 * A specialized implementation of the `ResourceThread` interface tailored for use with OpenGL
 * or OpenGL ES.
 *
 * `ResourceThreadGL3` is used to initialize and manage a dedicated rendering context in a separate
 * thread. This ensures that OpenGL-related resources can be managed independently without interfering
 * with the primary graphics thread.
 *
 * This class manages the creation of a hidden SDL window and sets up an OpenGL or OpenGL ES context
 * depending on the driver's configuration. Resources are processed within the thread, and a default
 * render target (`NullRenderTargetGL3`) is used for binding. This ensures a consistent and isolated
 * environment for resource operations.
 *
 * The `create` function sets thread-specific properties, initializes OpenGL/GLES capabilities, and
 * invokes the provided lambda (`f`) within the resource processing thread. Cleanup of the created
 * context and SDL window is handled automatically after the lambda finishes execution.
 */
class ResourceThreadSDL : ResourceThread {
    companion object {
        fun create(f: () -> Unit): ResourceThreadSDL {
            SDL_GL_ResetAttributes()
            SDL_GL_SetAttribute(SDL_GL_DOUBLEBUFFER, 1);
            SDL_GL_SetAttribute(SDL_GL_ACCELERATED_VISUAL, 1);
            SDL_GL_SetAttribute(SDL_GL_RED_SIZE, 8);
            SDL_GL_SetAttribute(SDL_GL_GREEN_SIZE, 8);
            SDL_GL_SetAttribute(SDL_GL_BLUE_SIZE, 8);
            SDL_GL_SetAttribute(SDL_GL_ALPHA_SIZE, 8);
            SDL_GL_SetAttribute(SDL_GL_CONTEXT_PROFILE_MASK, SDL_GL_CONTEXT_PROFILE_CORE)
            SDL_GL_SetAttribute(SDL_GL_CONTEXT_MAJOR_VERSION, Driver.glVersion.majorVersion)
            SDL_GL_SetAttribute(SDL_GL_CONTEXT_MINOR_VERSION, Driver.glVersion.minorVersion)
            SDL_GL_SetAttribute(SDL_GL_SHARE_WITH_CURRENT_CONTEXT, 1)

            if (Driver.glType == DriverTypeGL.GLES) {
                SDL_GL_SetAttribute(SDL_GL_CONTEXT_PROFILE_MASK, SDL_GL_CONTEXT_PROFILE_ES)
            } else {
                SDL_GL_SetAttribute(SDL_GL_CONTEXT_FLAGS, SDL_GL_CONTEXT_FORWARD_COMPATIBLE_FLAG)
                SDL_GL_SetAttribute(SDL_GL_CONTEXT_PROFILE_MASK, SDL_GL_CONTEXT_PROFILE_CORE)
            }

            val window = SDL_CreateWindow("Resource Thread", 640, 480, SDL_WINDOW_OPENGL or SDL_WINDOW_HIDDEN)
            require(window != MemoryUtil.NULL) { "Failed to create window. ${SDL_GetError()}" }
            val context = SDL_GL_CreateContext(window)
            require(context != MemoryUtil.NULL) { "Failed to create OpenGL context. ${SDL_GetError()}" }

            thread(isDaemon = true, name = "ResourceThread") {
                logger.debug { "Context thread starting" }
                SDL_GL_MakeCurrent(window, context)

                when (Driver.glType) {
                    DriverTypeGL.GL -> GL.createCapabilities()
                    DriverTypeGL.GLES -> GLES.createCapabilities()
                }
                val n = NullRenderTargetGL3()
                n.bind()
                Driver.instance.clear(ColorRGBa.BLACK)
                try {
                    f()
                } catch(e: Throwable) {
                    logger.error(e) { "Caught exception in resource thread." }
                } finally {
                    logger.debug { "Context thread exiting" }
                    (Driver.instance as DriverGL3).executeOnMainThread {
                        logger.debug { "Destroying resource thread context $context" }
                        Driver.instance.destroyContext(context)
                        SDL_GL_DestroyContext(context)
                        SDL_DestroyWindow(window)
                    }
                }
            }
            return ResourceThreadSDL()
        }
    }
}