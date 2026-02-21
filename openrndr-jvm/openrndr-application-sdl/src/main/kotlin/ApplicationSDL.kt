package org.openrndr.application.sdl

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.GL_RENDERER
import org.lwjgl.opengl.GL11.GL_VENDOR
import org.lwjgl.opengl.GL11.GL_VERSION
import org.lwjgl.opengl.GL30.GL_MAJOR_VERSION
import org.lwjgl.opengl.GL30.GL_MINOR_VERSION
import org.lwjgl.opengles.GLES
import org.lwjgl.sdl.SDLError.SDL_GetError
import org.lwjgl.sdl.SDLEvents.*
import org.lwjgl.sdl.SDLKeyboard.SDL_GetModState
import org.lwjgl.sdl.SDLKeycode.*
import org.lwjgl.sdl.SDLMouse.*
import org.lwjgl.sdl.SDLTimer.SDL_GetTicks
import org.lwjgl.sdl.SDLVideo.*
import org.lwjgl.sdl.SDL_Event
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.NULL
import org.openrndr.*
import org.openrndr.animatable.Animatable
import org.openrndr.animatable.Clock
import org.openrndr.application.sdl.ApplicationSDLConfiguration.fixWindowSize
import org.openrndr.draw.DrawThread
import org.openrndr.draw.Session
import org.openrndr.internal.Driver
import org.openrndr.internal.KeyboardDriver
import org.openrndr.internal.ResourceThread
import org.openrndr.internal.gl3.*
import org.openrndr.math.Vector2
import org.openrndr.platform.Platform
import org.openrndr.platform.PlatformType
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.reflect.KMutableProperty0

private val logger = KotlinLogging.logger {}

class Proxy<R, T>(val b: R.() -> KMutableProperty0<T>) {
    operator fun getValue(r: R, p: Any): T = r.b().get()
    operator fun setValue(r: R, p: Any, v: T) {
        r.b().set(v)
    }
}

object ApplicationSDLConfiguration {
    val fixWindowSize by lazy {
        Platform.type == PlatformType.WINDOWS || Platform.type == PlatformType.GENERIC
    }
}

private fun modifiersFromSdl(mod: Int): Set<KeyModifier> {
    if (mod == 0) {
        return emptySet()
    } else {
        val modifiers = mutableSetOf<KeyModifier>()
        if (mod and SDL_KMOD_ALT != 0) modifiers.add(KeyModifier.ALT)
        if (mod and SDL_KMOD_CTRL != 0) modifiers.add(KeyModifier.CTRL)
        if (mod and SDL_KMOD_SHIFT != 0) modifiers.add(KeyModifier.SHIFT)
        if (mod and SDL_KMOD_GUI != 0) modifiers.add(KeyModifier.SUPER)
        return modifiers
    }
}

class ApplicationSDL(override var program: Program, override var configuration: Configuration) : Application() {

    override fun requestDraw() {
        drawRequested = true
    }

    private var drawRequested = false

    private var exitRequested = false

    override fun exit() {
        exitRequested = true
    }


    private val thread = Thread.currentThread()
    internal val windows: CopyOnWriteArrayList<ApplicationWindowSDL> = CopyOnWriteArrayList()
    private val windowsById = mutableMapOf<Int, ApplicationWindowSDL>()

    private val dropFiles = mutableListOf<String>()


    init {
        program.application = this
    }

    private var primaryWindow: Long = 0L
    private var primaryGlContext: Long = 0L

    lateinit var window: ApplicationWindowSDL

    private val vaos = IntArray(1)

    override fun requestFocus() {
        TODO("Not yet implemented")
    }

    override fun windowClose() {
        window.close()
    }

    override fun windowFullscreen(mode: Fullscreen) {
        window.fullscreen(mode)
    }

    override fun windowMaximize() {
        window.maximize()
    }

    override fun windowMinimize() {
        window.minimize()
    }

    val defaultRenderTarget by lazy { ProgramRenderTargetGL3(program) }

    fun windowById(id: Int): ApplicationWindowSDL? {
        if (SDL_GetWindowID(window.window) == id) {
            return window
        }
        return windowsById[id]
    }

    private fun createPrimaryWindow() {
        SDL_GL_ResetAttributes()
        SDL_GL_SetAttribute(SDL_GL_DOUBLEBUFFER, 1)
        SDL_GL_SetAttribute(SDL_GL_ACCELERATED_VISUAL, 1)

        SDL_GL_SetAttribute(SDL_GL_DEPTH_SIZE, 24)
        SDL_GL_SetAttribute(SDL_GL_STENCIL_SIZE, 8)
        SDL_GL_SetAttribute(SDL_GL_FRAMEBUFFER_SRGB_CAPABLE, 1)

        SDL_GL_SetAttribute(SDL_GL_RED_SIZE, 8)
        SDL_GL_SetAttribute(SDL_GL_GREEN_SIZE, 8)
        SDL_GL_SetAttribute(SDL_GL_BLUE_SIZE, 8)
        SDL_GL_SetAttribute(SDL_GL_ALPHA_SIZE, 8)

        for (version in DriverGL3Configuration.candidateVersions()) {
            when (version.type) {
                DriverTypeGL.GL -> {
                    SDL_GL_SetAttribute(SDL_GL_CONTEXT_FLAGS, SDL_GL_CONTEXT_FORWARD_COMPATIBLE_FLAG)
                    SDL_GL_SetAttribute(SDL_GL_CONTEXT_MAJOR_VERSION, version.majorVersion)
                    SDL_GL_SetAttribute(SDL_GL_CONTEXT_MINOR_VERSION, version.minorVersion)
                    SDL_GL_SetAttribute(SDL_GL_CONTEXT_PROFILE_MASK, SDL_GL_CONTEXT_PROFILE_CORE)
                }

                DriverTypeGL.GLES -> {
                    SDL_GL_SetAttribute(SDL_GL_CONTEXT_MAJOR_VERSION, version.majorVersion)
                    SDL_GL_SetAttribute(SDL_GL_CONTEXT_MINOR_VERSION, version.minorVersion)
                    SDL_GL_SetAttribute(SDL_GL_CONTEXT_PROFILE_MASK, SDL_GL_CONTEXT_PROFILE_ES)
                }
            }
            val windowFlags = SDL_WINDOW_HIDDEN or SDL_WINDOW_OPENGL
            primaryWindow = SDL_CreateWindow("OPENRNDR - hidden window", 640, 480, windowFlags)
            require(primaryWindow != 0L) { "failed to create primary window" }

            primaryGlContext = SDL_GL_CreateContext(primaryWindow)
            if (primaryGlContext != 0L) {
                logger.debug { "Created GL context ${version.type} ${version.majorVersion}.${version.minorVersion}" }
                break
            } else {
                SDL_DestroyWindow(primaryWindow)
            }
        }

        require(primaryGlContext != 0L) {
            "Failed to create primary GL context. '${SDL_GetError()}', tried versions ${
                DriverGL3Configuration.candidateVersions()
                    .joinToString(", ") { "${it.type} ${it.majorVersion}.${it.minorVersion}" }
            }"
        }
        SDL_GL_MakeCurrent(primaryWindow, primaryGlContext)

        when (DriverGL3Configuration.driverType) {
            DriverTypeGL.GL -> GL.createCapabilities()
            DriverTypeGL.GLES -> GLES.createCapabilities()
        }

        val driverVersion =
            DriverVersionGL.find(
                DriverGL3Configuration.driverType,
                glGetInteger(GL_MAJOR_VERSION),
                glGetInteger(GL_MINOR_VERSION)
            )
                ?: error("unsupported driver version")

        logger.info { "OpenGL vendor: ${glGetString(GL_VENDOR)}" }
        logger.info { "OpenGL renderer: ${glGetString(GL_RENDERER)}" }
        logger.info { "OpenGL version: ${glGetString(GL_VERSION)}" }

        Driver.driver = object : DriverGL3(driverVersion) {
            override fun createDrawThread(session: Session?): DrawThread {
                TODO("Not yet implemented")
            }

            override fun createResourceThread(session: Session?, f: () -> Unit): ResourceThread {
                return ResourceThreadSDL()
            }

            override val contextID: Long
                get() = SDL_GL_GetCurrentContext()
        }
        logger.info { "Created primary window with id ${SDL_GetWindowID(primaryWindow)}" }
    }

    override suspend fun setup() {
        createPrimaryWindow()

        val wc = WindowConfiguration(
            width = configuration.width,
            height = configuration.height,
            position = configuration.position,
            title = configuration.title,
            multisample = configuration.multisample,
            alwaysOnTop = configuration.windowAlwaysOnTop,
            hideDecorations = configuration.hideWindowDecorations,
            resizable = configuration.windowResizable,
            fullscreen = configuration.fullscreen,
            transparent = configuration.windowTransparent,
            hideMouseCursor = configuration.hideCursor,
            display = configuration.display,
            relativeMouseCoordinates = configuration.cursorHideMode == MouseCursorHideMode.DISABLE,
            unfocusBehaviour = configuration.unfocusBehaviour
        )
        SDL_GL_MakeCurrent(primaryWindow, primaryGlContext)
        program.driver = Driver.instance

        Animatable.clock(object : Clock {
            override val time: Long
                get() = (program.seconds * 1E3).toLong()
            override val timeNanos: Long
                get() = (program.seconds * 1E6).toLong()
        })
        window = createApplicationWindowSDL(this, wc, program, null)
        SDL_GL_MakeCurrent(window.window, window.glContext)
        windowsById[SDL_GetWindowID(window.window)] = window
        setupPreload(program, configuration)
    }

    private inline fun Vector2.toDisplayUnits(scale: Double): Vector2 {
        return if (!fixWindowSize) this else this / scale
    }

    private fun handleSDLEvent(event: SDL_Event) {
        when (event.type()) {
            SDL_EVENT_QUIT -> {
                logger.info { "received quit event" }
                exitRequested = true
            }

            SDL_EVENT_WINDOW_RESIZED -> {
                val windowId = event.window().windowID()
                val eventWindow = windowById(windowId)
                    ?: run { logger.warn { "got event (=SDL_EVENT_WINDOW_RESIZED) for unknown window id (=${windowId}): " }; return };
            }

            SDL_EVENT_WINDOW_EXPOSED -> {
                val windowId = event.window().windowID()
                val eventWindow = windowById(windowId)
                    ?: run { logger.warn { "got event (=SDL_EVENT_WINDOW_RESIZED) for unknown window id (=${windowId}): " }; return };
                eventWindow.update()
            }

            SDL_EVENT_WINDOW_PIXEL_SIZE_CHANGED -> {
//                println("pixel size changed")
            }

            SDL_EVENT_WINDOW_ICCPROF_CHANGED -> {
//                println("icc profile changed")
            }

            SDL_EVENT_WINDOW_DISPLAY_CHANGED -> {
//                println("display changed")
            }

            SDL_EVENT_WINDOW_DISPLAY_SCALE_CHANGED -> {
//                println("display scale changed")
            }

            SDL_EVENT_WINDOW_MOVED -> {
                val windowId = event.window().windowID()
                val eventWindow = windowById(windowId)
                    ?: run { logger.warn { "got event (=SDL_EVENT_WINDOW_MOVED) for unknown window id (=${windowId}): " }; return };

                val windowEvent = event.window()
                val x = windowEvent.data1().toDouble()
                val y = windowEvent.data2().toDouble()
                eventWindow.program.window.moved.trigger(
                    WindowEvent(
                        WindowEventType.MOVED,
                        Vector2(x, y),
                        Vector2.ZERO,
                        true
                    )
                )
            }

            SDL_EVENT_WINDOW_FOCUS_GAINED -> {
                val windowId = event.window().windowID()
                val eventWindow = windowById(windowId)
                    ?: run { logger.warn { "got event (=SDL_EVENT_WINDOW_FOCUS_GAINED) for unknown window id (=${windowId}): " }; return };


                SDL_SetWindowMouseGrab(event.window().windowID().toLong(), true)
                eventWindow.let {
                    it.windowFocused = true
                    program.window.focused.trigger(
                        WindowEvent(
                            WindowEventType.FOCUSED,
                            getMousePosition(),
                            Vector2.ZERO,
                            true
                        )
                    )
                }
            }

            SDL_EVENT_WINDOW_FOCUS_LOST -> {
                val windowId = event.window().windowID()
                val eventWindow = windowById(windowId)
                    ?: run { logger.warn { "got event (=SDL_EVENT_WINDOW_FOCUS_LOST) for unknown window id (=${windowId}): " }; return };

                eventWindow.let {
                    it.windowFocused = false
                    it.program.window.unfocused.trigger(
                        WindowEvent(
                            WindowEventType.UNFOCUSED,
                            getMousePosition(),
                            Vector2.ZERO,
                            true
                        )
                    )
                }
            }

            SDL_EVENT_WINDOW_MOUSE_ENTER -> {
                val windowId = event.window().windowID()
                val eventWindow = windowById(windowId)
                    ?: run { logger.warn { "got event (=SDL_EVENT_WINDOW_MOUSE_ENTER) for unknown window id (=${windowId}): " }; return };

                eventWindow.program.mouse.entered.trigger(
                    MouseEvent(
                        getMousePosition(),
                        Vector2.ZERO,
                        Vector2.ZERO,
                        MouseEventType.ENTERED,
                        MouseButton.NONE,
                        modifiersFromSdl(SDL_GetModState().toInt())
                    )
                )
            }

            SDL_EVENT_WINDOW_MOUSE_LEAVE -> {
                val windowId = event.window().windowID()
                val eventWindow = windowById(windowId)
                    ?: run { logger.warn { "got event (=SDL_EVENT_WINDOW_MOUSE_LEAVE) for unknown window id (=${windowId}): " }; return };

                eventWindow.program.mouse.exited.trigger(
                    MouseEvent(
                        getMousePosition(),
                        Vector2.ZERO,
                        Vector2.ZERO,
                        MouseEventType.EXITED,
                        MouseButton.NONE,
                        modifiersFromSdl(SDL_GetModState().toInt())
                    )
                )
            }

            SDL_EVENT_MOUSE_MOTION -> {
                val windowId = event.motion().windowID()
                val eventWindow = windowById(windowId)
                    ?: run { logger.warn { "got event (=SDL_EVENT_MOUSE_MOTION) for unknown window id (=${windowId}): " }; return };

                val mouseEvent = event.motion()
                val scale = eventWindow.windowContentScale
                val cursorPosition = Vector2(mouseEvent.x().toDouble(), mouseEvent.y().toDouble()).toDisplayUnits(scale)
                val displacement =
                    Vector2(mouseEvent.xrel().toDouble(), mouseEvent.yrel().toDouble()).toDisplayUnits(scale)
                eventWindow.cursorPosition = cursorPosition
                eventWindow.program.mouse.moved.trigger(
                    MouseEvent(
                        cursorPosition, Vector2.ZERO, displacement, MouseEventType.MOVED,
                        MouseButton.NONE, modifiersFromSdl(SDL_GetModState().toInt())
                    )
                )
                val button = getMouseButtonFromMask(mouseEvent.state())
                if (button != MouseButton.NONE) {
                    eventWindow.program.mouse.dragged.trigger(
                        MouseEvent(
                            cursorPosition, Vector2.ZERO, displacement, MouseEventType.DRAGGED,
                            button, modifiersFromSdl(SDL_GetModState().toInt())
                        )
                    )
                }
            }

            SDL_EVENT_MOUSE_BUTTON_UP -> {
                val windowId = event.button().windowID()
                val eventWindow = windowById(windowId)
                    ?: run { logger.warn { "got event (=SDL_EVENT_MOUSE_BUTTON_UP) for unknown window id (=${windowId}): " }; return };

                val mouseEvent = event.button()
                val scale = eventWindow.windowContentScale
                val cursorPosition = Vector2(mouseEvent.x().toDouble(), mouseEvent.y().toDouble()).toDisplayUnits(scale)
                eventWindow.cursorPosition = cursorPosition
                eventWindow.program.mouse.buttonUp.trigger(
                    MouseEvent(
                        cursorPosition,
                        Vector2.ZERO,
                        Vector2.ZERO,
                        MouseEventType.BUTTON_UP,
                        getMouseButton(mouseEvent.button().toInt()),
                        modifiersFromSdl(SDL_GetModState().toInt())
                    )
                )
            }

            SDL_EVENT_MOUSE_BUTTON_DOWN -> {
                val windowId = event.button().windowID()
                val eventWindow = windowById(windowId)
                    ?: run { logger.warn { "got event (=SDL_EVENT_MOUSE_BUTTON_DOWN) for unknown window id (=${windowId}): " }; return };

                val mouseEvent = event.button()
                val scale = eventWindow.windowContentScale
                val cursorPosition = Vector2(mouseEvent.x().toDouble(), mouseEvent.y().toDouble()).toDisplayUnits(scale)
                eventWindow.cursorPosition = cursorPosition
                eventWindow.program.mouse.buttonDown.trigger(
                    MouseEvent(
                        cursorPosition,
                        Vector2.ZERO,
                        Vector2.ZERO,
                        MouseEventType.BUTTON_DOWN,
                        getMouseButton(mouseEvent.button().toInt()),
                        modifiersFromSdl(SDL_GetModState().toInt())
                    )
                )
            }

            SDL_EVENT_CLIPBOARD_UPDATE -> {
                val clipboardEvent = event.clipboard()

//                println("number of mime types: ${clipboardEvent.num_mime_types()}")
            }

            SDL_EVENT_DROP_POSITION -> {
//                val dropEvent = event.drop()
//                println("${dropEvent.x()} ${dropEvent.y()}")
//                println(dropEvent.sourceString())
//                println(dropEvent.dataString())
//                println(dropEvent.data())
            }

            SDL_EVENT_DROP_BEGIN -> {
                dropFiles.clear()
            }

            SDL_EVENT_DROP_FILE -> {
                val dropEvent = event.drop()
                val data = dropEvent.dataString()
                if (data != null) {
                    dropFiles.add(data)
                }
            }

            SDL_EVENT_DROP_COMPLETE -> {
                val windowId = event.drop().windowID()
                val eventWindow = windowById(windowId)
                    ?: run { logger.warn { "got event (=SDL_EVENT_DROP_COMPLETE) for unknown window id (=${windowId}): " }; return };

                val dropEvent = event.drop()
                eventWindow.program.window.drop.trigger(
                    DropEvent(
                        Vector2(dropEvent.x().toDouble(), dropEvent.y().toDouble()),
                        dropFiles
                    )
                )
            }

            SDL_EVENT_MOUSE_WHEEL -> {
                val windowId = event.wheel().windowID()
                val eventWindow = windowById(windowId)
                    ?: run { logger.warn { "got event (=SDL_EVENT_MOUSE_WHEEL) for unknown window id (=${windowId}): " }; return };

                val mouseEvent = event.wheel()
                cursorPosition = Vector2(
                    mouseEvent.mouse_x().toDouble(),
                    mouseEvent.mouse_y().toDouble()
                ).toDisplayUnits(eventWindow.windowContentScale)
                val rotation = Vector2(mouseEvent.x().toDouble(), mouseEvent.y().toDouble())
                eventWindow.program.mouse.scrolled.trigger(
                    MouseEvent(
                        cursorPosition, rotation, Vector2.ZERO, MouseEventType.SCROLLED,
                        MouseButton.NONE, modifiersFromSdl(SDL_GetModState().toInt())
                    )
                )
            }

            SDL_EVENT_FINGER_DOWN -> {
                val windowId = event.tfinger().windowID()
                val eventWindow = windowById(windowId)
                    ?: run { logger.debug { "got event (=SDL_EVENT_FINGER_DOWN) for unknown window id (=${windowId}): " }; return };

                val fingerEvent = event.tfinger()
                eventWindow.program.pointers.pointerDown.trigger(
                    PointerEvent(
                        fingerEvent.fingerID(),
                        Vector2(fingerEvent.x().toDouble(), fingerEvent.y().toDouble()) * program.window.size,
                        Vector2(fingerEvent.dx().toDouble(), fingerEvent.dy().toDouble()) * program.window.size,
                        fingerEvent.pressure().toDouble()
                    )
                )
            }

            SDL_EVENT_FINGER_UP -> {
                val windowId = event.tfinger().windowID()
                val eventWindow = windowById(windowId)
                    ?: run { logger.debug { "got event (=SDL_EVENT_FINGER_UP) for unknown window id (=${windowId}): " }; return };

                val fingerEvent = event.tfinger()
                eventWindow.program.pointers.pointerUp.trigger(
                    PointerEvent(
                        fingerEvent.fingerID(),
                        Vector2.ZERO,
                        Vector2.ZERO,
                        0.0
                    )
                )
            }

            SDL_EVENT_FINGER_MOTION -> {
                val windowId = event.tfinger().windowID()
                val eventWindow = windowById(windowId)
                    ?: run { logger.debug { "got event (=SDL_EVENT_FINGER_MOTION) for unknown window id (=${windowId}): " }; return };

                val fingerEvent = event.tfinger()
                eventWindow.program.pointers.moved.trigger(
                    PointerEvent(
                        fingerEvent.fingerID(),
                        Vector2(fingerEvent.x().toDouble(), fingerEvent.y().toDouble()) * program.window.size,
                        Vector2(fingerEvent.dx().toDouble(), fingerEvent.dy().toDouble()) * program.window.size,
                        fingerEvent.pressure().toDouble()
                    )
                )
            }

            SDL_EVENT_FINGER_CANCELED -> {
                val windowId = event.tfinger().windowID()
                val eventWindow = windowById(windowId)
                    ?: run { logger.debug { "got event for (=SDL_EVENT_FINGER_CANCELED) unknown window id (=${windowId}): " }; return };

                val fingerEvent = event.tfinger()
                eventWindow.program.pointers.pointerUp.trigger(
                    PointerEvent(
                        fingerEvent.fingerID(),
                        Vector2.ZERO,
                        Vector2.ZERO,
                        0.0
                    )
                )
            }


            SDL_EVENT_TEXT_INPUT -> {
                val windowId = event.text().windowID()
                val eventWindow = windowById(windowId)
                    ?: run { logger.warn { "got event for unknown window id (=${windowId}): " }; return };

                val textEvent = event.text()
                eventWindow.program.keyboard.character.trigger(
                    CharacterEvent(textEvent.textString()?.first() ?: ' ', emptySet())
                )
            }

            SDL_EVENT_KEY_UP -> {
                val windowId = event.key().windowID()
                val eventWindow = windowById(windowId)
                    ?: run { logger.warn { "got event for unknown window id (=${windowId}): " }; return };

                val keyEvent = event.key()
                val key = keyEvent.key()
                eventWindow.program.keyboard.keyUp.trigger(
                    KeyEvent(
                        KeyEventType.KEY_UP,
                        key,
                        KeyboardDriver.instance.getKeyName(key),
                        modifiersFromSdl(keyEvent.mod().toInt())
                    )
                )
            }

            SDL_EVENT_KEY_DOWN -> {
                val windowId = event.key().windowID()
                val eventWindow = windowById(windowId)
                    ?: run { logger.warn { "got event for unknown window id (=${windowId}): " }; return };

                val keyEvent = event.key()
                val eventType = if (keyEvent.repeat()) KeyEventType.KEY_REPEAT else KeyEventType.KEY_DOWN
                val key = keyEvent.key()
                eventWindow.program.keyboard.keyDown.trigger(
                    KeyEvent(
                        eventType,
                        key,
                        KeyboardDriver.instance.getKeyName(key),
                        modifiersFromSdl(keyEvent.mod().toInt())
                    )
                )
            }

            SDL_EVENT_WINDOW_CLOSE_REQUESTED -> {
                val windowId = event.window().windowID()
                val eventWindow = windowById(windowId)
                    ?: run { logger.warn { "got event for unknown window id (=${windowId}): " }; return };

                eventWindow.closeRequested = true
            }

            else -> {
//                println("got event: ${event.type()}")
            }
        }
    }


    /**
     * Maps an SDL mouse button bitmask to a corresponding `MouseButton` constant.
     *
     * @param buttons A button bitmask. Example: 1110 = Left, Middle and Right buttons
     * pressed simultaneously.
     *
     * Note: `buttons` can contain a combination of buttons, but here
     * we only return the first matching one.
     */
    private fun getMouseButtonFromMask(buttons: Int) = when {
        SDL_BUTTON_LMASK and buttons != 0 -> MouseButton.LEFT
        SDL_BUTTON_MMASK and buttons != 0 -> MouseButton.CENTER
        SDL_BUTTON_RMASK and buttons != 0 -> MouseButton.RIGHT
        else -> MouseButton.NONE
    }

    /**
     * Maps an SDL mouse button constant to a `MouseButton` constant
     * Note: SDL provides SDL_BUTTON_X1 and SDL_BUTTON_X2, but MouseButton doesn't.
     */
    private fun getMouseButton(button: Int) = when (button) {
        SDL_BUTTON_LEFT -> MouseButton.LEFT
        SDL_BUTTON_MIDDLE -> MouseButton.CENTER
        SDL_BUTTON_RIGHT -> MouseButton.RIGHT
        else -> MouseButton.NONE
    }

    /**
     * Query SDL for the current mouse position
     */
    private fun getMousePosition(): Vector2 {
        stackPush().use { stack ->
            val mouseX = stack.mallocFloat(1)
            val mouseY = stack.mallocFloat(1)
            SDL_GetMouseState(mouseX, mouseY)
            return Vector2(mouseX[0].toDouble(), mouseY[0].toDouble())
        }
    }

    override fun loop() {
        defaultRenderTarget.bind()


        window.setupSizes()
        runBlocking {
            program.setup()
        }

        if (System.getProperty("org.openrndr.application.eventwatch") != "false") {
            // https://wiki.libsdl.org/SDL3/AppFreezeDuringDrag
            SDL_AddEventWatch({ p0: Long, p1: Long ->
                val event = SDL_Event.create(p1)
                if (event.type() == SDL_EVENT_WINDOW_EXPOSED) {
                    handleSDLEvent(event)
                }
                true

            }, NULL)
        }

        val event = SDL_Event.create()

        while (!exitRequested && !window.closeRequested) {
            while (SDL_PollEvent(event)) {
                handleSDLEvent(event)
            }
            window.update()

            val closeWindows = windows.filter { it.closeRequested }
            for (window in closeWindows) {
                windows.remove(window)
                windowsById.remove(SDL_GetWindowID(window.window))
                window.close()
            }
            for (window in windows) {
                window.update()
            }
        }
    }


    override var clipboardContents: String?
        get() = TODO("Not yet implemented")
        set(value) {}
    override var windowTitle: String by Proxy { window::windowTitle }

    override var windowPosition: Vector2 by Proxy { window::windowPosition }
    override var windowSize: Vector2 by Proxy { window::windowSize }
    override var windowResizable by Proxy { window::windowResizable }
    override var windowMultisample by Proxy { window::windowMultisample }
    override var cursorPosition by Proxy { window::cursorPosition }
    override var cursorVisible by Proxy { window::cursorVisible }
    override var cursorHideMode by Proxy { window::cursorHideMode }
    override var cursorType by Proxy { window::cursorType }
    override val seconds: Double
        get() = SDL_GetTicks() / 1000.0
    override var presentationMode by Proxy { window::presentationMode }
    override var windowContentScale by Proxy { window::windowContentScale }

    override fun createChildWindow(
        configuration: WindowConfiguration,
        program: Program
    ): ApplicationWindow {
        require(Thread.currentThread() === thread) { "createChildWindow should be called from thread '${thread.name}'" }

        val oldContext = SDL_GL_GetCurrentContext()
        SDL_GL_MakeCurrent(primaryWindow, primaryGlContext)
        val childWindow = createApplicationWindowSDL(this, configuration, program, window.program.drawer)
        childWindow.defaultRenderTarget.bind()

        runBlocking {
            program.setup()
        }
        windows.add(childWindow)
        windowsById[SDL_GetWindowID(childWindow.window)] = childWindow

        if (oldContext != primaryGlContext) {
            SDL_GL_MakeCurrent(primaryWindow, oldContext)
        } else if (oldContext == window.glContext) {
            SDL_GL_MakeCurrent(window.window, window.glContext)
        } else {
            val oldWindow = windows.find { it.glContext == oldContext }
            require(oldWindow != null) { "could not find old context $oldContext" }
            SDL_GL_MakeCurrent(oldWindow.window, oldContext)
        }

        return childWindow
    }
}