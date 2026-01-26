package org.openrndr.application.sdl

import io.github.oshai.kotlinlogging.KotlinLogging
import org.lwjgl.sdl.SDLMouse.SDL_CursorVisible
import org.lwjgl.sdl.SDLMouse.SDL_HideCursor
import org.lwjgl.sdl.SDLMouse.SDL_SYSTEM_CURSOR_CROSSHAIR
import org.lwjgl.sdl.SDLMouse.SDL_SYSTEM_CURSOR_DEFAULT
import org.lwjgl.sdl.SDLMouse.SDL_SYSTEM_CURSOR_EW_RESIZE
import org.lwjgl.sdl.SDLMouse.SDL_SYSTEM_CURSOR_NS_RESIZE
import org.lwjgl.sdl.SDLMouse.SDL_SYSTEM_CURSOR_POINTER
import org.lwjgl.sdl.SDLMouse.SDL_SYSTEM_CURSOR_TEXT
import org.lwjgl.sdl.SDLMouse.SDL_SetCursor
import org.lwjgl.sdl.SDLMouse.SDL_SetWindowRelativeMouseMode
import org.lwjgl.sdl.SDLMouse.SDL_ShowCursor
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
import org.lwjgl.sdl.SDLVideo.SDL_GL_DEPTH_SIZE
import org.lwjgl.sdl.SDLVideo.SDL_GL_DOUBLEBUFFER
import org.lwjgl.sdl.SDLVideo.SDL_GL_FRAMEBUFFER_SRGB_CAPABLE
import org.lwjgl.sdl.SDLVideo.SDL_GL_GREEN_SIZE
import org.lwjgl.sdl.SDLVideo.SDL_GL_MULTISAMPLESAMPLES
import org.lwjgl.sdl.SDLVideo.SDL_GL_MakeCurrent
import org.lwjgl.sdl.SDLVideo.SDL_GL_RED_SIZE
import org.lwjgl.sdl.SDLVideo.SDL_GL_ResetAttributes
import org.lwjgl.sdl.SDLVideo.SDL_GL_SHARE_WITH_CURRENT_CONTEXT
import org.lwjgl.sdl.SDLVideo.SDL_GL_STENCIL_SIZE
import org.lwjgl.sdl.SDLVideo.SDL_GL_SetAttribute
import org.lwjgl.sdl.SDLVideo.SDL_GL_SwapWindow
import org.lwjgl.sdl.SDLVideo.SDL_GetWindowDisplayScale
import org.lwjgl.sdl.SDLVideo.SDL_GetWindowPosition
import org.lwjgl.sdl.SDLVideo.SDL_GetWindowSizeInPixels
import org.lwjgl.sdl.SDLVideo.SDL_GetWindowTitle
import org.lwjgl.sdl.SDLVideo.SDL_SetWindowPosition
import org.lwjgl.sdl.SDLVideo.SDL_SetWindowSize
import org.lwjgl.sdl.SDLVideo.SDL_SetWindowTitle
import org.lwjgl.sdl.SDLVideo.SDL_ShowWindow
import org.lwjgl.sdl.SDLVideo.SDL_WINDOW_ALWAYS_ON_TOP
import org.lwjgl.sdl.SDLVideo.SDL_WINDOW_BORDERLESS
import org.lwjgl.sdl.SDLVideo.SDL_WINDOW_FULLSCREEN
import org.lwjgl.sdl.SDLVideo.SDL_WINDOW_HIDDEN
import org.lwjgl.sdl.SDLVideo.SDL_WINDOW_HIGH_PIXEL_DENSITY
import org.lwjgl.sdl.SDLVideo.SDL_WINDOW_MOUSE_CAPTURE
import org.lwjgl.sdl.SDLVideo.SDL_WINDOW_OPENGL
import org.lwjgl.sdl.SDLVideo.SDL_WINDOW_RESIZABLE
import org.lwjgl.sdl.SDLVideo.SDL_WINDOW_TRANSPARENT
import org.lwjgl.sdl.SDLVideo.SDL_WINDOW_UTILITY
import org.lwjgl.sdl.SDL_Event
import org.lwjgl.system.MemoryStack.stackPush
import org.openrndr.ApplicationWindow
import org.openrndr.CursorType
import org.openrndr.Fullscreen
import org.openrndr.MouseCursorHideMode
import org.openrndr.PresentationMode
import org.openrndr.Program
import org.openrndr.UnfocusBehaviour
import org.openrndr.WindowConfiguration
import org.openrndr.WindowMultisample
import org.openrndr.WindowProgram
import org.openrndr.draw.Drawer
import org.openrndr.internal.Driver
import org.openrndr.internal.gl3.DriverGL3Configuration
import org.openrndr.internal.gl3.DriverTypeGL
import org.openrndr.internal.gl3.ProgramRenderTargetGL3
import org.openrndr.internal.gl3.glVersion
import org.openrndr.internal.gl3.glViewport
import org.openrndr.math.Vector2
import kotlin.use


private val logger = KotlinLogging.logger { }

class ApplicationWindowSDL(
    val application: ApplicationSDL,
    val window: Long,
    val glContext: Long,
    windowTitle: String,
    override var windowResizable: Boolean,
    override var windowMultisample: WindowMultisample,
    override var windowClosable: Boolean,
    override var unfocusBehaviour: UnfocusBehaviour,
    program: Program,
    drawer: Drawer,
) : ApplicationWindow(program) {

    var lastUpdate = -1L

    init {
        program.application = application
        program.drawer = drawer
        (program as? WindowProgram)?.applicationWindow = this
    }

    var closeRequested = false

    fun close() {
        SDL_DestroyWindow(window)
    }


    override var windowTitle: String
        get() = SDL_GetWindowTitle(window) ?: "OPENRNDR"
        set(value) {
            SDL_SetWindowTitle(window, value)
        }
    override var windowPosition: Vector2
        get() {
            stackPush().use { stack ->
                val x = stack.mallocInt(1)
                val y = stack.mallocInt(1)
                SDL_GetWindowPosition(window, x, y)
                return Vector2(x[0].toDouble(), y[0].toDouble())
            }
        }
        set(value) {
            SDL_SetWindowPosition(window, value.x.toInt(), value.y.toInt())
        }
    override var windowSize: Vector2
        get() {
            stackPush().use { stack ->
                val width = stack.mallocInt(1)
                val height = stack.mallocInt(1)
                SDL_GetWindowSizeInPixels(window, width, height)
                val scale = SDL_GetWindowDisplayScale(window)
                return Vector2(width[0].toDouble()/scale, height[0].toDouble()/scale)
            }
        }
        set(value) {
            SDL_SetWindowSize(window, value.x.toInt(), value.y.toInt())
        }
    override var windowFocused: Boolean = true

    override var cursorPosition: Vector2 = Vector2.ZERO
    override var cursorVisible: Boolean
        get() {
            return SDL_CursorVisible()
        }
        set(value) {
            if (value) {
                SDL_ShowCursor()
            } else {
                SDL_HideCursor()
            }

        }
    override var cursorHideMode: MouseCursorHideMode
        get() = TODO("Not yet implemented")
        set(value) {}
    override var cursorType: CursorType
        get() = TODO("Not yet implemented")
        set(value) {
            val sdlCursor = when (cursorType) {
                CursorType.HAND_CURSOR -> SDL_SYSTEM_CURSOR_DEFAULT
                CursorType.ARROW_CURSOR -> SDL_SYSTEM_CURSOR_POINTER
                CursorType.HRESIZE_CURSOR -> SDL_SYSTEM_CURSOR_EW_RESIZE
                CursorType.VRESIZE_CURSOR -> SDL_SYSTEM_CURSOR_NS_RESIZE
                CursorType.CROSSHAIR_CURSOR -> SDL_SYSTEM_CURSOR_CROSSHAIR
                CursorType.IBEAM_CURSOR -> SDL_SYSTEM_CURSOR_TEXT
            }
            SDL_SetCursor(sdlCursor.toLong())
        }
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

    val defaultRenderTarget by lazy { ProgramRenderTargetGL3(program) }
    override fun destroy() {
        logger.info { "destroying window $window" }
        for (extension in program.extensions) {
            extension.shutdown(program)
        }
        Driver.instance.destroyContext(window)
        SDL_DestroyWindow(window)
        application.windows.remove(this)
    }

    internal fun setupSizes() {
        stackPush().use { stack ->
            program.window.contentScale = SDL_GetWindowDisplayScale(window).toDouble()

            val fbw = stack.mallocInt(1)
            val fbh = stack.mallocInt(1)

            SDL_GetWindowSizeInPixels(window, fbw, fbh)
            glViewport(0, 0, fbw[0], fbh[0])


            //SDL_GetWindowSize(window, fbw, fbh)
            program.width = (fbw[0] / program.window.contentScale).toInt()
            program.height = (fbh[0] / program.window.contentScale).toInt()
        }
    }


    fun deliverEvents() {
        program.window.drop.deliver()
        program.window.sized.deliver()
        program.window.unfocused.deliver()
        program.window.focused.deliver()
        program.window.minimized.deliver()
        program.window.restored.deliver()
        program.keyboard.keyDown.deliver()
        program.keyboard.keyUp.deliver()
        program.keyboard.keyRepeat.deliver()
        program.keyboard.character.deliver()
        program.mouse.moved.deliver()
        program.mouse.scrolled.deliver()
        program.mouse.buttonDown.deliver()
        program.mouse.buttonUp.deliver()
        program.mouse.dragged.deliver()
        program.mouse.entered.deliver()
        program.mouse.exited.deliver()
    }

    fun update() {


        SDL_GL_MakeCurrent(window, glContext)
        defaultRenderTarget.bind()

        setupSizes()
        deliverEvents()
        program.drawer.reset()
        program.drawer.ortho()
        program.dispatcher.execute()

        val ct = System.currentTimeMillis()
        val draw = windowFocused || unfocusBehaviour == UnfocusBehaviour.NORMAL || (ct - lastUpdate >= 100)

        if (draw) {
            program.drawImpl()
            SDL_GL_SwapWindow(window)
            lastUpdate = ct
        }
    }
}

fun createApplicationWindowSDL(
    application: ApplicationSDL,
    configuration: WindowConfiguration,
    program: Program,
    drawer: Drawer
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

    var windowFlags = SDL_WINDOW_OPENGL or SDL_WINDOW_HIGH_PIXEL_DENSITY or SDL_WINDOW_HIDDEN

    if (configuration.alwaysOnTop) {
        windowFlags = windowFlags or SDL_WINDOW_ALWAYS_ON_TOP
    }

    if (configuration.resizable) {
        windowFlags = windowFlags or SDL_WINDOW_RESIZABLE
    }

    if (configuration.hideDecorations) {
        windowFlags = windowFlags or SDL_WINDOW_BORDERLESS
    }

    if (configuration.transparent) {
        windowFlags = windowFlags or SDL_WINDOW_TRANSPARENT
    }

    if (configuration.utilityWindow) {
        windowFlags = windowFlags or SDL_WINDOW_UTILITY
    }

    if (configuration.relativeMouseCoordinates) {
        logger.info { "Enabling mouse capture and relative mode for window" }
        windowFlags = windowFlags or SDL_WINDOW_MOUSE_CAPTURE //or SDL_WINDOW_MOUSE_RELATIVE_MODE
    }
    when (configuration.fullscreen) {
        Fullscreen.DISABLED -> Unit
        Fullscreen.CURRENT_DISPLAY_MODE -> windowFlags = windowFlags or SDL_WINDOW_FULLSCREEN
        Fullscreen.SET_DISPLAY_MODE -> TODO("not yet implemented")
    }

    when (val ms = configuration.multisample) {
        WindowMultisample.Disabled -> Unit
        is WindowMultisample.SampleCount -> SDL_GL_SetAttribute(SDL_GL_MULTISAMPLESAMPLES, ms.count)
        WindowMultisample.SystemDefault -> Unit
    }

    var window = SDL_CreateWindow(configuration.title, configuration.width, configuration.height, windowFlags)
    require(window != 0L) { "Failed to create window with configuration $configuration" }
    val glContext: Long
    stackPush().use {
        val scale = SDL_GetWindowDisplayScale(window).toDouble()

        val w = it.callocInt(1)
        val h = it.callocInt(1)
        SDL_GetWindowSizeInPixels(window, w, h)

        if (w.get(0)/scale != configuration.width.toDouble()) {
            SDL_DestroyWindow(window)
            window = SDL_CreateWindow(configuration.title, (configuration.width *scale).toInt(), (configuration.height * scale).toInt(), windowFlags)
        }

        glContext = SDL_GL_CreateContext(window)

        SDL_ShowWindow(window)
        if (configuration.hideMouseCursor) {
            if (!SDL_HideCursor()) {
                logger.warn { "Failed to hide mouse cursor." }
            }
        }
        if (configuration.relativeMouseCoordinates) {
            if (!SDL_SetWindowRelativeMouseMode(window, true)) {
                logger.warn { "Failed to enable mouse capture and relative mode." }
            }
        }
    }

    return ApplicationWindowSDL(
        application,
        window,
        glContext,
        configuration.title,
        configuration.resizable,
        configuration.multisample,
        configuration.closable,
        unfocusBehaviour = configuration.unfocusBehaviour,
        program = program,
        drawer
    )
}