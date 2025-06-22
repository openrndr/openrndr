package org.openrndr.internal.sdl

import io.github.oshai.kotlinlogging.KotlinLogging
import org.lwjgl.glfw.GLFW.glfwDestroyWindow
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
import org.lwjgl.sdl.SDLVideo.SDL_GL_DEPTH_SIZE
import org.lwjgl.sdl.SDLVideo.SDL_GL_DOUBLEBUFFER
import org.lwjgl.sdl.SDLVideo.SDL_GL_FRAMEBUFFER_SRGB_CAPABLE
import org.lwjgl.sdl.SDLVideo.SDL_GL_GREEN_SIZE
import org.lwjgl.sdl.SDLVideo.SDL_GL_MULTISAMPLESAMPLES
import org.lwjgl.sdl.SDLVideo.SDL_GL_RED_SIZE
import org.lwjgl.sdl.SDLVideo.SDL_GL_ResetAttributes
import org.lwjgl.sdl.SDLVideo.SDL_GL_SHARE_WITH_CURRENT_CONTEXT
import org.lwjgl.sdl.SDLVideo.SDL_GL_STENCIL_SIZE
import org.lwjgl.sdl.SDLVideo.SDL_GL_SetAttribute
import org.lwjgl.sdl.SDLVideo.SDL_GetWindowDisplayScale
import org.lwjgl.sdl.SDLVideo.SDL_GetWindowTitle
import org.lwjgl.sdl.SDLVideo.SDL_SetWindowTitle
import org.lwjgl.sdl.SDLVideo.SDL_WINDOW_ALWAYS_ON_TOP
import org.lwjgl.sdl.SDLVideo.SDL_WINDOW_BORDERLESS
import org.lwjgl.sdl.SDLVideo.SDL_WINDOW_HIGH_PIXEL_DENSITY
import org.lwjgl.sdl.SDLVideo.SDL_WINDOW_OPENGL
import org.lwjgl.sdl.SDLVideo.SDL_WINDOW_RESIZABLE
import org.openrndr.ApplicationWindow
import org.openrndr.CursorType
import org.openrndr.MouseCursorHideMode
import org.openrndr.PresentationMode
import org.openrndr.Program
import org.openrndr.WindowConfiguration
import org.openrndr.WindowMultisample
import org.openrndr.internal.Driver
import org.openrndr.internal.gl3.DriverGL3Configuration
import org.openrndr.internal.gl3.DriverTypeGL
import org.openrndr.internal.gl3.glVersion
import org.openrndr.internal.glfw.ApplicationGLFW
import org.openrndr.math.Vector2


private val logger = KotlinLogging.logger { }

class ApplicationWindowSDL(
    val application: ApplicationSDL,
    val window: Long,
    val glContext: Long,
    windowTitle: String,
    override val windowResizable: Boolean,
    override val windowMultisample: WindowMultisample,
    override val windowClosable: Boolean,
    program: Program,
) : ApplicationWindow(program) {


    override var windowTitle: String
        get() = SDL_GetWindowTitle(window) ?: "OPENRNDR"
        set(value) {
            SDL_SetWindowTitle(window, value)
        }
    override var windowPosition: Vector2
        get() = TODO("Not yet implemented")
        set(value) {}
    override var windowSize: Vector2
        get() = TODO("Not yet implemented")
        set(value) {}
    override val windowFocused: Boolean
        get() = TODO("Not yet implemented")
    override var cursorPosition: Vector2
        get() = TODO("Not yet implemented")
        set(value) {}
    override var cursorVisible: Boolean
        get() = TODO("Not yet implemented")
        set(value) {}
    override var cursorHideMode: MouseCursorHideMode
        get() = TODO("Not yet implemented")
        set(value) {}
    override var cursorType: CursorType
        get() = TODO("Not yet implemented")
        set(value) {}
    override val cursorInWindow: Boolean
        get() = TODO("Not yet implemented")

    override var presentationMode: PresentationMode = PresentationMode.AUTOMATIC

    var drawRequested = false
    override fun requestDraw() {
        drawRequested = true
    }

    override var windowContentScale: Double
        get() = SDL_GetWindowDisplayScale(window).toDouble()
        set(value) {}

    init {
        this.windowTitle = windowTitle
    }




    override fun destroy() {
        logger.info { "destroying window $window" }
        for (extension in program.extensions) {
            extension.shutdown(program)
        }
        Driver.instance.destroyContext(window)
        glfwDestroyWindow(window)
        application.windows.remove(this)
    }
}

fun createApplicationWindowSDL(
    application: ApplicationSDL,
    configuration: WindowConfiguration,
    program: Program
): ApplicationWindowSDL {

    SDL_GL_ResetAttributes()
    SDL_GL_SetAttribute(SDL_GL_DOUBLEBUFFER, 1)
    SDL_GL_SetAttribute(SDL_GL_ACCELERATED_VISUAL, 1)
    SDL_GL_SetAttribute(SDL_GL_RED_SIZE, 8)
    SDL_GL_SetAttribute(SDL_GL_GREEN_SIZE, 8)
    SDL_GL_SetAttribute(SDL_GL_BLUE_SIZE, 8)
    SDL_GL_SetAttribute(SDL_GL_ALPHA_SIZE, 8)
    SDL_GL_SetAttribute(SDL_GL_DEPTH_SIZE, 24)
    SDL_GL_SetAttribute(SDL_GL_STENCIL_SIZE, 8)
    SDL_GL_SetAttribute(SDL_GL_SHARE_WITH_CURRENT_CONTEXT, 1)
    SDL_GL_SetAttribute(SDL_GL_FRAMEBUFFER_SRGB_CAPABLE, 1)

    SDL_GL_SetAttribute(SDL_GL_CONTEXT_MAJOR_VERSION, Driver.glVersion.majorVersion)
    SDL_GL_SetAttribute(SDL_GL_CONTEXT_MINOR_VERSION, Driver.glVersion.minorVersion)

    if (DriverGL3Configuration.driverType == DriverTypeGL.GLES) {
        SDL_GL_SetAttribute(SDL_GL_CONTEXT_PROFILE_MASK, SDL_GL_CONTEXT_PROFILE_ES)
    } else {
        SDL_GL_SetAttribute(SDL_GL_CONTEXT_FLAGS, SDL_GL_CONTEXT_FORWARD_COMPATIBLE_FLAG)
        SDL_GL_SetAttribute(SDL_GL_CONTEXT_PROFILE_MASK, SDL_GL_CONTEXT_PROFILE_CORE)
    }

    var windowFlags = SDL_WINDOW_OPENGL or SDL_WINDOW_HIGH_PIXEL_DENSITY

    if (configuration.alwaysOnTop) {
        windowFlags = windowFlags or SDL_WINDOW_ALWAYS_ON_TOP
    }

    if (configuration.resizable) {
        windowFlags = windowFlags or SDL_WINDOW_RESIZABLE
    }

    if (configuration.hideDecorations) {
        windowFlags = windowFlags or SDL_WINDOW_BORDERLESS
    }
    when (val ms = configuration.multisample) {
        WindowMultisample.Disabled -> Unit
        is WindowMultisample.SampleCount -> SDL_GL_SetAttribute(SDL_GL_MULTISAMPLESAMPLES, ms.count)
        WindowMultisample.SystemDefault -> Unit
    }

    val window = SDL_CreateWindow(configuration.title, configuration.width, configuration.height, windowFlags)
    val glContext = SDL_GL_CreateContext(window)
    return ApplicationWindowSDL(
        application,
        window,
        glContext,
        configuration.title,
        configuration.resizable,
        configuration.multisample,
        configuration.closable,
        program = program
    )

}