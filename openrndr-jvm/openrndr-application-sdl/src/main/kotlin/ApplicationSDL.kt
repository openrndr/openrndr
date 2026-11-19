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
import org.lwjgl.opengles.GLES30
import org.lwjgl.sdl.SDLClipboard.SDL_GetClipboardText
import org.lwjgl.sdl.SDLClipboard.SDL_SetClipboardText
import org.lwjgl.sdl.SDLError.SDL_GetError
import org.lwjgl.sdl.SDLInit.SDL_Quit
import org.lwjgl.sdl.SDLEvents.*
import org.lwjgl.sdl.SDLKeyboard.SDL_GetModState
import org.lwjgl.sdl.SDLKeycode.*
import org.lwjgl.sdl.SDLMouse.*
import org.lwjgl.sdl.SDLTimer.SDL_GetTicksNS
import org.lwjgl.sdl.SDLTouch.SDL_GetTouchDeviceType
import org.lwjgl.sdl.SDLTouch.SDL_TOUCH_DEVICE_INDIRECT_RELATIVE
import org.lwjgl.sdl.SDLVideo.*
import org.lwjgl.sdl.SDL_Event
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.NULL
import org.openrndr.*
import org.openrndr.color.ColorProfile
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
        SDL_GetCurrentVideoDriver() != "wayland" && (Platform.type == PlatformType.WINDOWS || Platform.type == PlatformType.GENERIC)
    }
}

/** the sets of modifiers by their bits, alt, ctrl, shift and super, made once and shared by events */
private val modifierSets: Array<Set<KeyModifier>> = Array(16) { bits ->
    buildSet {
        if (bits and 1 != 0) add(KeyModifier.ALT)
        if (bits and 2 != 0) add(KeyModifier.CTRL)
        if (bits and 4 != 0) add(KeyModifier.SHIFT)
        if (bits and 8 != 0) add(KeyModifier.SUPER)
    }
}

private fun modifiersFromSdl(mod: Int): Set<KeyModifier> {
    if (mod == 0) {
        return emptySet()
    }
    var bits = 0
    if (mod and SDL_KMOD_ALT != 0) bits = bits or 1
    if (mod and SDL_KMOD_CTRL != 0) bits = bits or 2
    if (mod and SDL_KMOD_SHIFT != 0) bits = bits or 4
    if (mod and SDL_KMOD_GUI != 0) bits = bits or 8
    return modifierSets[bits]
}

class ApplicationSDL(override var program: Program, override var configuration: Configuration) : Application() {

    override fun requestDraw() {
        window.requestDraw()
    }

    /** The type of the event that wakes the loop when it waits for events, 0 when it could not be registered */
    private val wakeEventType = SDL_RegisterEvents(1)

    /** Is the loop waiting for events? */
    @Volatile
    private var waiting = false

    /** Was work queued for the loop, in a dispatcher or for the main thread, since it last looked? */
    @Volatile
    private var workQueued = false

    /**
     * Wakes the loop for work queued from any thread, an event is only pushed while it waits, so dispatching many
     * tasks does not fill the event queue
     */
    internal fun wake() {
        workQueued = true
        if (waiting && wakeEventType != 0) {
            stackPush().use {
                val event = SDL_Event.calloc(it)
                event.type(wakeEventType)
                SDL_PushEvent(event)
            }
        }
    }

    private var exitRequested = false

    /** Are exposed windows drawn by an event watch? See the loop */
    private val useEventWatch = System.getProperty("org.openrndr.application.eventwatch") != "false"

    override fun exit() {
        exitRequested = true
    }


    private val thread = Thread.currentThread()
    internal val windows: CopyOnWriteArrayList<ApplicationWindowSDL> = CopyOnWriteArrayList()
    internal val windowsById = mutableMapOf<Int, ApplicationWindowSDL>()

    private val dropFiles = mutableListOf<String>()
    private val dropTexts = mutableListOf<String>()
    private var dropType = 0


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

    override var windowHitTest: ((Vector2) -> Hit)?
        get() = window.windowHitTest
        set(value) {
            window.windowHitTest = value
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
            require(primaryWindow != 0L) { "failed to create primary window: ${SDL_GetError()}" }

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


        if (DriverGL3Configuration.driverType == DriverTypeGL.GLES) {
            val extensions = IntArray(1)
            glGetIntegerv(GL_NUM_EXTENSIONS, extensions)
            logger.debug {
                "OpenGL ES extensions: ${(0 until extensions[0]).joinToString(" ") { GLES30.glGetStringi(GL_EXTENSIONS, it) ?: "" }}"
            }

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
            override fun createDrawThread(session: Session?): DrawThread = DrawThreadSDL.create()

            override fun createResourceThread(session: Session?, f: () -> Unit): ResourceThread =
                ResourceThreadSDL.create(f)

            override val contextID: Long
                get() = SDL_GL_GetCurrentContext()
        }
        (Driver.driver as DriverGL3).onMainThreadWork = ::wake
        logger.debug { "Created primary window with id ${SDL_GetWindowID(primaryWindow)}" }
    }

    override suspend fun setup() {
        createPrimaryWindow()

        val preload = getPreload()

        preload?.onConfiguration(configuration)

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
            unfocusBehaviour = configuration.unfocusBehaviour,
            vsync = configuration.vsync,
            hdr = configuration.hdr
        )
        SDL_GL_MakeCurrent(primaryWindow, primaryGlContext)
        Program.active = program
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
        defaultRenderTarget.bind()
        window.setupSizes()
        preload?.onProgramSetup(program)
    }

    private inline fun Vector2.toDisplayUnits(scale: Double): Vector2 {
        return if (!fixWindowSize) this else this / scale
    }

    private var focusedWindowId = -1

    /**
     * The fingers that are down on a touchpad, touch devices of type [SDL_TOUCH_DEVICE_INDIRECT_RELATIVE]. Wheel events
     * while two or more fingers are down are touchpad scrolls, see [scrollSource].
     */
    private val touchpadFingers = mutableSetOf<Long>()

    /** Have touchpad fingers been reported? Wheel events are only known to come from a wheel when they have */
    private var touchpadFingersReported = false

    /** The time of the last touchpad scroll in nanoseconds, the momentum of a scroll continues after the fingers lift */
    private var lastTouchpadScroll = Long.MIN_VALUE

    /**
     * The source of a wheel event at [time] in nanoseconds: a touchpad while two or more fingers are down on it, or
     * while the wheel events of a touchpad scroll follow each other, which they do during its momentum
     */
    private fun scrollSource(time: Long): ScrollSource {
        val touchpad = touchpadFingers.size >= 2 || time - lastTouchpadScroll < 150_000_000L
        return when {
            touchpad -> ScrollSource.TOUCHPAD.also { lastTouchpadScroll = time }
            touchpadFingersReported -> ScrollSource.WHEEL
            else -> ScrollSource.UNKNOWN
        }
    }

    /** Tracks the fingers of touchpads, [down] is false when the finger is lifted or canceled */
    private fun trackTouchpadFinger(touchID: Long, fingerID: Long, down: Boolean) {
        if (SDL_GetTouchDeviceType(touchID) != SDL_TOUCH_DEVICE_INDIRECT_RELATIVE) {
            return
        }
        touchpadFingersReported = true
        if (down) {
            touchpadFingers.add(fingerID)
        } else {
            touchpadFingers.remove(fingerID)
        }
    }
    private fun handleSDLEvent(event: SDL_Event) {
        when (event.type()) {
            SDL_EVENT_QUIT -> {
                logger.debug { "Received quit event" }
                exitRequested = true
            }

            // without the event watch exposed windows are drawn by the loop, also in PresentationMode.MANUAL
            SDL_EVENT_WINDOW_EXPOSED -> {
                if (!useEventWatch) {
                    windowById(event.window().windowID())?.requestDraw()
                }
            }

            // the size in pixels changes when the window is resized and when its content scale changes
            SDL_EVENT_WINDOW_PIXEL_SIZE_CHANGED -> {
                val windowId = event.window().windowID()
                val eventWindow = windowById(windowId) ?: return
                eventWindow.updateProgramSize()
                eventWindow.program.window.sized.trigger(
                    WindowEvent(
                        WindowEventType.RESIZED,
                        eventWindow.windowPosition,
                        Vector2(eventWindow.program.width.toDouble(), eventWindow.program.height.toDouble()),
                        eventWindow.windowFocused
                    )
                )
            }

            SDL_EVENT_WINDOW_MINIMIZED -> {
                val windowId = event.window().windowID()
                val eventWindow = windowById(windowId) ?: return
                eventWindow.minimized = true
                eventWindow.program.window.minimized.trigger(
                    WindowEvent(WindowEventType.MINIMIZED, Vector2.ZERO, Vector2.ZERO, false)
                )
            }

            // SDL also sends this when a maximized window is restored, only a minimized window triggers restored
            SDL_EVENT_WINDOW_RESTORED -> {
                val windowId = event.window().windowID()
                val eventWindow = windowById(windowId) ?: return
                if (eventWindow.minimized) {
                    eventWindow.minimized = false
                    eventWindow.program.window.restored.trigger(
                        WindowEvent(
                            WindowEventType.RESTORED,
                            eventWindow.windowPosition,
                            Vector2(eventWindow.program.width.toDouble(), eventWindow.program.height.toDouble()),
                            true
                        )
                    )
                }
            }

            // a window gets the color profile of the display it is on. SDL also sends these events to the hidden
            // windows that hold OpenGL contexts, which are not in windowById and are skipped without a warning
            SDL_EVENT_WINDOW_ICCPROF_CHANGED, SDL_EVENT_WINDOW_DISPLAY_CHANGED -> {
                val windowId = event.window().windowID()
                val eventWindow = windowById(windowId) ?: return
                if (eventWindow.updateColorProfile()) {
                    logger.debug { "color profile of window $windowId changed to ${eventWindow.windowColorProfile}" }
                    eventWindow.program.window.colorProfileChanged.trigger(
                        WindowEvent(WindowEventType.COLOR_PROFILE_CHANGED, Vector2.ZERO, Vector2.ZERO, true)
                    )
                }
            }

            SDL_EVENT_WINDOW_HDR_STATE_CHANGED -> {
                val windowId = event.window().windowID()
                val eventWindow = windowById(windowId) ?: return
                if (eventWindow.updateHdrState()) {
                    eventWindow.program.window.hdrChanged.trigger(
                        WindowEvent(WindowEventType.HDR_CHANGED, Vector2.ZERO, Vector2.ZERO, true)
                    )
                }
            }

            SDL_EVENT_WINDOW_DISPLAY_SCALE_CHANGED -> {
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

                focusedWindowId = windowId
                eventWindow.let {
                    it.windowFocused = true
                    it.program.window.focused.trigger(
                        WindowEvent(
                            WindowEventType.FOCUSED,
                            getMousePosition(it),
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
                            getMousePosition(it),
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
                        getMousePosition(eventWindow),
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
                        getMousePosition(eventWindow),
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
                dropTexts.clear()
                dropType = 0
            }

            SDL_EVENT_DROP_FILE -> {
                val dropEvent = event.drop()
                val data = dropEvent.dataString()
                if (data != null) {
                    dropFiles.add(data)
                }
                dropType = dropEvent.type()
            }

            SDL_EVENT_DROP_TEXT -> {
                val dropEvent = event.drop()
                val data = dropEvent.dataString()
                if (data != null) {
                    dropTexts.add(data)
                }
                dropType = dropEvent.type()
            }

            SDL_EVENT_DROP_COMPLETE -> {
                val windowId = event.drop().windowID()
                val eventWindow = windowById(windowId)
                    ?: run { logger.warn { "got event (=SDL_EVENT_DROP_COMPLETE) for unknown window id (=${windowId}): " }; return };

                val scale = eventWindow.windowContentScale

                val dropEvent = event.drop()

                when (dropType) {
                    SDL_EVENT_DROP_FILE -> {
                        eventWindow.program.window.drop.trigger(
                            DropEvent(
                                Vector2(dropEvent.x().toDouble(), dropEvent.y().toDouble()).toDisplayUnits(scale),
                                dropFiles
                            )
                        )
                    }

                    SDL_EVENT_DROP_TEXT -> {
                        eventWindow.program.window.dropTexts.trigger(
                            DropTextEvent(
                                Vector2(dropEvent.x().toDouble(), dropEvent.y().toDouble()).toDisplayUnits(scale),
                                dropTexts
                            )
                        )
                    }

                    else -> run { logger.warn { "got event (=SDL_EVENT_DROP_COMPLETE) with unsupported dropType (=$dropType): " }; return }
                }
            }

            SDL_EVENT_MOUSE_WHEEL -> {
                val windowId = event.wheel().windowID()
                val eventWindow = windowById(windowId)
                    ?: run { logger.warn { "got event (=SDL_EVENT_MOUSE_WHEEL) for unknown window id (=${windowId}): " }; return };

                val mouseEvent = event.wheel()
                val cursorPosition = Vector2(
                    mouseEvent.mouse_x().toDouble(),
                    mouseEvent.mouse_y().toDouble()
                ).toDisplayUnits(eventWindow.windowContentScale)
                eventWindow.cursorPosition = cursorPosition
                val rotation = Vector2(mouseEvent.x().toDouble(), mouseEvent.y().toDouble())
                eventWindow.program.mouse.scrolled.trigger(
                    MouseEvent(
                        cursorPosition, rotation, Vector2.ZERO, MouseEventType.SCROLLED,
                        MouseButton.NONE, modifiersFromSdl(SDL_GetModState().toInt()),
                        scrollSource(mouseEvent.timestamp())
                    )
                )
            }

            SDL_EVENT_PINCH_BEGIN -> {
                val pinchEvent = event.pinch()
                val windowId = pinchEvent.windowID()
                val eventWindow = windowById(windowId) ?: windowById(focusedWindowId)
                ?: run { logger.trace { "got event (=SDL_EVENT_PINCH_BEGIN) for unknown window id (=${windowId}): " }; return };

                // pinch events have no position, a touchpad pinch happens at the cursor
                eventWindow.program.gestures.pinchStarted.trigger(PinchEvent(1.0, eventWindow.cursorPosition))
            }
            SDL_EVENT_PINCH_UPDATE -> {
                val pinchEvent = event.pinch()
                val windowId = pinchEvent.windowID()
                val eventWindow = windowById(windowId) ?: windowById(focusedWindowId)
                ?: run { logger.trace { "got event (=SDL_EVENT_PINCH_END) for unknown window id (=${windowId}): " }; return };

                eventWindow.program.gestures.pinchUpdated.trigger(
                    PinchEvent(pinchEvent.scale().toDouble(), eventWindow.cursorPosition)
                )
            }


            SDL_EVENT_PINCH_END -> {
                val pinchEvent = event.pinch()
                val windowId = pinchEvent.windowID()
                val eventWindow = windowById(windowId) ?: windowById(focusedWindowId)
                ?: run { logger.trace { "got event (=SDL_EVENT_PINCH_END) for unknown window id (=${windowId}): " }; return };

                eventWindow.program.gestures.pinchEnded.trigger(PinchEvent(1.0, eventWindow.cursorPosition))
            }

            SDL_EVENT_FINGER_DOWN -> {
                trackTouchpadFinger(event.tfinger().touchID(), event.tfinger().fingerID(), down = true)
                val windowId = event.tfinger().windowID()
                val eventWindow = windowById(windowId) ?: windowById(focusedWindowId)
                    ?: run { logger.trace { "got event (=SDL_EVENT_FINGER_DOWN) for unknown window id (=${windowId}): " }; return };

                val fingerEvent = event.tfinger()
                eventWindow.program.pointers.pointerDown.trigger(
                    PointerEvent(
                        fingerEvent.fingerID(),
                        Vector2(fingerEvent.x().toDouble(), fingerEvent.y().toDouble()) * eventWindow.program.window.size,
                        Vector2(fingerEvent.dx().toDouble(), fingerEvent.dy().toDouble()) * eventWindow.program.window.size,
                        fingerEvent.pressure().toDouble()
                    )
                )
            }

            SDL_EVENT_FINGER_UP -> {
                trackTouchpadFinger(event.tfinger().touchID(), event.tfinger().fingerID(), down = false)
                val windowId = event.tfinger().windowID()
                val eventWindow = windowById(windowId) ?: windowById(focusedWindowId)
                ?: run { logger.trace { "got event (=SDL_EVENT_FINGER_DOWN) for unknown window id (=${windowId}): " }; return };

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
                val eventWindow = windowById(windowId) ?: windowById(focusedWindowId)
                ?: run { logger.trace { "got event (=SDL_EVENT_FINGER_DOWN) for unknown window id (=${windowId}): " }; return };

                val fingerEvent = event.tfinger()
                eventWindow.program.pointers.moved.trigger(
                    PointerEvent(
                        fingerEvent.fingerID(),
                        Vector2(fingerEvent.x().toDouble(), fingerEvent.y().toDouble()) * eventWindow.program.window.size,
                        Vector2(fingerEvent.dx().toDouble(), fingerEvent.dy().toDouble()) * eventWindow.program.window.size,
                        fingerEvent.pressure().toDouble()
                    )
                )
            }

            SDL_EVENT_FINGER_CANCELED -> {
                trackTouchpadFinger(event.tfinger().touchID(), event.tfinger().fingerID(), down = false)
                val windowId = event.tfinger().windowID()
                val eventWindow = windowById(windowId) ?: windowById(focusedWindowId)
                ?: run { logger.trace { "got event (=SDL_EVENT_FINGER_DOWN) for unknown window id (=${windowId}): " }; return };

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
                ?: run { logger.trace { "got event (=SDL_EVENT_FINGER_DOWN) for unknown window id (=${windowId}): " }; return };

                // a text event can hold more than one character, from an input method or as a surrogate pair
                val text = event.text().textString() ?: return
                for (character in text) {
                    eventWindow.program.keyboard.character.trigger(CharacterEvent(character, emptySet()))
                }
            }

            SDL_EVENT_KEY_UP -> {
                val windowId = event.key().windowID()
                val eventWindow = windowById(windowId)
                ?: run { logger.trace { "got event (=SDL_EVENT_FINGER_DOWN) for unknown window id (=${windowId}): " }; return };

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
                val eventWindow = windowById(windowId) ?: windowById(focusedWindowId)
                ?: run { logger.trace { "got event (=SDL_EVENT_FINGER_DOWN) for unknown window id (=${windowId}): " }; return };

                val keyEvent = event.key()
                val isRepeat = keyEvent.repeat()
                val eventType = if (isRepeat) KeyEventType.KEY_REPEAT else KeyEventType.KEY_DOWN
                val key = keyEvent.key()
                val event = KeyEvent(
                    eventType,
                    key,
                    KeyboardDriver.instance.getKeyName(key),
                    modifiersFromSdl(keyEvent.mod().toInt())
                )
                if(isRepeat) {
                    eventWindow.program.keyboard.keyRepeat.trigger(event)
                } else {
                    eventWindow.program.keyboard.keyDown.trigger(event)
                }
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
     * Query SDL for the current mouse position, in the units of [window]
     */
    private fun getMousePosition(window: ApplicationWindowSDL): Vector2 {
        stackPush().use { stack ->
            val mouseX = stack.mallocFloat(1)
            val mouseY = stack.mallocFloat(1)
            SDL_GetMouseState(mouseX, mouseY)
            return Vector2(mouseX[0].toDouble(), mouseY[0].toDouble()).toDisplayUnits(window.windowContentScale)
        }
    }

    override fun loop() {
        defaultRenderTarget.bind()

        window.setupSizes()
        @Suppress("DEPRECATION")
        program.drawer.reset()
        program.drawer.ortho()

        if (configuration.showBeforeSetup) {
            window.showBeforeSetup()
        }

        runBlocking {
            program.setup()
        }

        if (useEventWatch) {
            // https://wiki.libsdl.org/SDL3/AppFreezeDuringDrag
            // the watch draws exposed windows while the event loop is blocked, during a live resize on macOS for
            // example. The poll loop ignores exposed events, a window that was drawn by the watch is not drawn again
            SDL_AddEventWatch({ _: Long, p1: Long ->
                val event = SDL_Event.create(p1)
                if (event.type() == SDL_EVENT_WINDOW_EXPOSED) {
                    val eventWindow = windowById(event.window().windowID())
                    if (eventWindow != null && eventWindow.update(exposed = true)) {
                        // the window is presented right away, the loop is blocked
                        eventWindow.present()
                        eventWindow.drawnByEventWatch = true
                    }
                }
                true

            }, NULL)
        }

        val event = SDL_Event.create()
        program.dispatcher.onWorkQueued = ::wake

        fun update(window: ApplicationWindowSDL): Boolean {
            if (window.drawnByEventWatch) {
                window.drawnByEventWatch = false
                return true
            }
            return window.update()
        }

        while (!exitRequested && !window.closeRequested) {
            while (SDL_PollEvent(event)) {
                handleSDLEvent(event)
            }
            // the functions resource threads leave for the main thread, such as destroying their contexts
            (Driver.instance as DriverGL3).processMainThreadExecutables()
            var drawn = update(window)

            // the child windows are looped over by index, iterating would allocate an iterator every frame
            if (windows.isNotEmpty()) {
                var i = 0
                while (i < windows.size) {
                    val child = windows[i]
                    if (child.closeRequested) {
                        windows.removeAt(i)
                        windowsById.remove(SDL_GetWindowID(child.window))
                        child.destroy()
                    } else {
                        i++
                    }
                }
                for (j in 0 until windows.size) {
                    drawn = update(windows[j]) or drawn
                }
            }
            if (drawn) {
                present()
            }
            // no window was drawn: they are minimized, or unfocused and throttled. Wait for an event, for work queued
            // from another thread, or until a window or a delayed coroutine is due, instead of spinning
            if (!drawn && !exitRequested) {
                val timeout = idleTimeout()
                waiting = true
                if (!workQueued && timeout > 0 && SDL_WaitEventTimeout(event, timeout.toInt())) {
                    handleSDLEvent(event)
                }
                waiting = false
            }
            workQueued = false
        }
        window.destroy()
        for (window in windows) {
            window.destroy()
        }
        shutdown()
    }

    /**
     * Presents the windows that were drawn. All windows are drawn before they are presented, one window paces the
     * loop: it is presented last and is the only one that waits for a vertical blank, the primary window when it was
     * drawn, the first drawn child window with vsync otherwise. With every window waiting, the frame rate would be
     * divided by the number of windows on drivers that block in the swap.
     */
    private fun present() {
        var pacer: ApplicationWindowSDL? = null
        for (i in -1 until windows.size) {
            val w = if (i == -1) window else windows[i]
            if (pacer == null && w.vsync && w.presentPending) {
                pacer = w
            }
        }
        for (i in -1 until windows.size) {
            val w = if (i == -1) window else windows[i]
            w.pacing = w === pacer
            if (w !== pacer) {
                w.present()
            }
        }
        pacer?.present()
    }

    /**
     * The number of milliseconds the loop can wait for events: until the next window is drawn or the next delay of a
     * coroutine is due, at most a second in case work is queued without waking it
     */
    private fun idleTimeout(): Long {
        var timeout = 1000L
        for (i in -1 until windows.size) {
            val w = if (i == -1) window else windows[i]
            w.millisUntilNextDraw()?.let { timeout = minOf(timeout, it) }
            w.program.dispatcher.millisUntilNextDelay()?.let { timeout = minOf(timeout, it) }
        }
        return timeout
    }

    /**
     * Destroys the primary context and window and shuts SDL down. EGL is only terminated by [SDL_Quit], some EGL
     * drivers crash at exit when it was not.
     */
    private fun shutdown() {
        SDL_GL_MakeCurrent(0L, 0L)
        SDL_GL_DestroyContext(primaryGlContext)
        SDL_DestroyWindow(primaryWindow)
        primaryGlContext = 0L
        primaryWindow = 0L
        destroySystemCursors()
        SDL_Quit()
    }


    override var clipboardContents: String?
        get() = SDL_GetClipboardText()
        set(value) {
            if (value != null) {
                SDL_SetClipboardText(value)
            }
        }
    override var windowTitle: String by Proxy { window::windowTitle }
    override var textInputArea: TextInputArea? by Proxy { window::textInputArea }
    override var textInputActive: Boolean by Proxy { window::textInputActive }

    override var windowPosition: Vector2 by Proxy { window::windowPosition }
    override var windowSize: Vector2 by Proxy { window::windowSize }
    override var windowResizable by Proxy { window::windowResizable }
    override var windowMultisample by Proxy { window::windowMultisample }
    override var cursorPosition by Proxy { window::cursorPosition }
    override var cursorVisible by Proxy { window::cursorVisible }
    override var cursorHideMode by Proxy { window::cursorHideMode }
    override var cursorType by Proxy { window::cursorType }
    override val seconds: Double
        get() = SDL_GetTicksNS() / 1E9
    override var presentationMode by Proxy { window::presentationMode }
    override var windowContentScale by Proxy { window::windowContentScale }
    override val windowColorProfile: ColorProfile
        get() = window.windowColorProfile
    override val windowHdrHeadroom: Double
        get() = window.windowHdrHeadroom
    override val windowSdrWhiteLevel: Double
        get() = window.windowSdrWhiteLevel

    override fun createChildWindow(
        configuration: WindowConfiguration,
        program: Program
    ): ApplicationWindow {
        require(Thread.currentThread() === thread) { "createChildWindow should be called from thread '${thread.name}'" }

        // this prevents the created window from occasionally not appearing on macOS
        if (windows.isNotEmpty()) {
            SDL_SyncWindow(windows.last().window)
        }

        val oldContext = SDL_GL_GetCurrentContext()
        SDL_GL_MakeCurrent(primaryWindow, primaryGlContext)
        val childWindow = createApplicationWindowSDL(this, configuration, program, window.program.drawer)
        childWindow.defaultRenderTarget.bind()

        childWindow.setupSizes()
        @Suppress("DEPRECATION")
        program.drawer.reset()
        program.drawer.ortho()

        runBlocking {
            program.setup()
        }
        windows.add(childWindow)
        windowsById[SDL_GetWindowID(childWindow.window)] = childWindow
        program.dispatcher.onWorkQueued = ::wake

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