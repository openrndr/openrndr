package org.openrndr.application.sdl

import org.lwjgl.opengl.GL
import org.lwjgl.opengles.GLES
import org.lwjgl.sdl.SDLError.SDL_GetError
import org.lwjgl.sdl.SDLVideo.SDL_CreateWindow
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
import org.lwjgl.sdl.SDLVideo.SDL_GL_GREEN_SIZE
import org.lwjgl.sdl.SDLVideo.SDL_GL_RED_SIZE
import org.lwjgl.sdl.SDLVideo.SDL_GL_ResetAttributes
import org.lwjgl.sdl.SDLVideo.SDL_GL_SHARE_WITH_CURRENT_CONTEXT
import org.lwjgl.sdl.SDLVideo.SDL_GL_SetAttribute
import org.lwjgl.sdl.SDLVideo.SDL_WINDOW_HIDDEN
import org.lwjgl.sdl.SDLVideo.SDL_WINDOW_OPENGL
import org.lwjgl.system.MemoryUtil
import org.openrndr.Dispatcher
import org.openrndr.draw.DrawThread
import org.openrndr.draw.Drawer
import org.openrndr.internal.Driver
import org.openrndr.internal.gl3.DriverTypeGL
import org.openrndr.internal.gl3.NullRenderTargetGL3
import org.openrndr.internal.gl3.glBindVertexArray
import org.openrndr.internal.gl3.glGenVertexArrays
import org.openrndr.internal.gl3.glType
import org.openrndr.internal.gl3.glVersion
import kotlin.concurrent.thread

class DrawThreadSDL(val window: Long, val context: Long): DrawThread {
    companion object {
        fun create() : DrawThreadSDL {
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

            val window = SDL_CreateWindow("Draw Thread", 640, 480, SDL_WINDOW_OPENGL or SDL_WINDOW_HIDDEN)
            require(window != MemoryUtil.NULL) { "Failed to create window. ${SDL_GetError()}" }
            val context = SDL_GL_CreateContext(window)
            require(context != MemoryUtil.NULL) { "Failed to create OpenGL context. ${SDL_GetError()}" }

            return DrawThreadSDL(window, context)
        }
    }

    private lateinit var realDrawer: Drawer
    private lateinit var realDispatcher: Dispatcher


    override val dispatcher get() = realDispatcher

    override val drawer: Drawer
        get() = realDrawer


    init {
        thread(isDaemon = true, name = "DrawThreadGL3") {
            realDispatcher = Dispatcher()

            when (Driver.glType) {
                DriverTypeGL.GL -> GL.createCapabilities()
                DriverTypeGL.GLES -> GLES.createCapabilities()
            }

            realDrawer = Drawer(Driver.instance)
            val vaos = IntArray(1)
            glGenVertexArrays(vaos)
            glBindVertexArray(vaos[0])
            val renderTarget = NullRenderTargetGL3()
            renderTarget.bind()
            while (true) {
                dispatcher.execute()
                Thread.sleep(1)
            }
        }
    }
}