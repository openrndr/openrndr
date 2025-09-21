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
import org.lwjgl.sdl.SDLEvents.SDL_EVENT_CLIPBOARD_UPDATE
import org.lwjgl.sdl.SDLEvents.SDL_EVENT_FINGER_CANCELED
import org.lwjgl.sdl.SDLEvents.SDL_EVENT_FINGER_DOWN
import org.lwjgl.sdl.SDLEvents.SDL_EVENT_FINGER_MOTION
import org.lwjgl.sdl.SDLEvents.SDL_EVENT_FINGER_UP
import org.lwjgl.sdl.SDLEvents.SDL_EVENT_KEY_DOWN
import org.lwjgl.sdl.SDLEvents.SDL_EVENT_KEY_UP
import org.lwjgl.sdl.SDLEvents.SDL_EVENT_MOUSE_BUTTON_DOWN
import org.lwjgl.sdl.SDLEvents.SDL_EVENT_MOUSE_BUTTON_UP
import org.lwjgl.sdl.SDLEvents.SDL_EVENT_MOUSE_MOTION
import org.lwjgl.sdl.SDLEvents.SDL_EVENT_MOUSE_WHEEL
import org.lwjgl.sdl.SDLEvents.SDL_EVENT_QUIT
import org.lwjgl.sdl.SDLEvents.SDL_EVENT_TEXT_INPUT
import org.lwjgl.sdl.SDLEvents.SDL_EVENT_WINDOW_CLOSE_REQUESTED
import org.lwjgl.sdl.SDLEvents.SDL_EVENT_WINDOW_DISPLAY_CHANGED
import org.lwjgl.sdl.SDLEvents.SDL_EVENT_WINDOW_DISPLAY_SCALE_CHANGED
import org.lwjgl.sdl.SDLEvents.SDL_EVENT_WINDOW_EXPOSED
import org.lwjgl.sdl.SDLEvents.SDL_EVENT_WINDOW_FOCUS_GAINED
import org.lwjgl.sdl.SDLEvents.SDL_EVENT_WINDOW_FOCUS_LOST
import org.lwjgl.sdl.SDLEvents.SDL_EVENT_WINDOW_ICCPROF_CHANGED
import org.lwjgl.sdl.SDLEvents.SDL_EVENT_WINDOW_MOUSE_ENTER
import org.lwjgl.sdl.SDLEvents.SDL_EVENT_WINDOW_MOUSE_LEAVE
import org.lwjgl.sdl.SDLEvents.SDL_EVENT_WINDOW_MOVED
import org.lwjgl.sdl.SDLEvents.SDL_EVENT_WINDOW_PIXEL_SIZE_CHANGED
import org.lwjgl.sdl.SDLEvents.SDL_PollEvent
import org.lwjgl.sdl.SDLKeycode.SDL_KMOD_ALT
import org.lwjgl.sdl.SDLKeycode.SDL_KMOD_CTRL
import org.lwjgl.sdl.SDLKeycode.SDL_KMOD_GUI
import org.lwjgl.sdl.SDLKeycode.SDL_KMOD_SHIFT
import org.lwjgl.sdl.SDLTimer.SDL_GetTicks
import org.lwjgl.sdl.SDLVideo.*
import org.lwjgl.sdl.SDL_Event

import org.openrndr.*
import org.openrndr.animatable.Animatable
import org.openrndr.animatable.Clock
import org.openrndr.draw.Drawer
import org.openrndr.internal.Driver
import org.openrndr.internal.gl3.*
import org.openrndr.application.sdl.ApplicationSDLConfiguration.fixWindowSize
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

    val defaultRenderTarget by lazy { ProgramRenderTargetGL3(program) }

    fun windowById(id: Int): ApplicationWindowSDL {
        if (SDL_GetWindowID(window.window) == id) {
            return window
        }
        return windowsById[id] ?: error("window with id $id not found")
    }

    private fun createPrimaryWindow() {
        SDL_GL_ResetAttributes()
        SDL_GL_SetAttribute(SDL_GL_DOUBLEBUFFER, 1);
        SDL_GL_SetAttribute(SDL_GL_ACCELERATED_VISUAL, 1);
        SDL_GL_SetAttribute(SDL_GL_RED_SIZE, 8);
        SDL_GL_SetAttribute(SDL_GL_GREEN_SIZE, 8);
        SDL_GL_SetAttribute(SDL_GL_BLUE_SIZE, 8);
        SDL_GL_SetAttribute(SDL_GL_ALPHA_SIZE, 8);
        SDL_GL_SetAttribute(SDL_GL_CONTEXT_PROFILE_MASK, SDL_GL_CONTEXT_PROFILE_CORE)

        if (DriverGL3Configuration.driverType == DriverTypeGL.GLES) {
            logger.info { "creating window with GLES context" }
            SDL_GL_SetAttribute(SDL_GL_CONTEXT_MAJOR_VERSION, 3)
            SDL_GL_SetAttribute(SDL_GL_CONTEXT_MINOR_VERSION, 2)
            SDL_GL_SetAttribute(SDL_GL_CONTEXT_PROFILE_MASK, SDL_GL_CONTEXT_PROFILE_ES)
        } else {
            SDL_GL_SetAttribute(SDL_GL_CONTEXT_FLAGS, SDL_GL_CONTEXT_FORWARD_COMPATIBLE_FLAG)
            SDL_GL_SetAttribute(SDL_GL_CONTEXT_MAJOR_VERSION, 4)
            SDL_GL_SetAttribute(SDL_GL_CONTEXT_MINOR_VERSION, 6)
            SDL_GL_SetAttribute(SDL_GL_CONTEXT_PROFILE_MASK, SDL_GL_CONTEXT_PROFILE_CORE)
        }

        var windowFlags = SDL_WINDOW_HIDDEN or SDL_WINDOW_OPENGL
        primaryWindow = SDL_CreateWindow("OPENRNDR - hidden window", 640, 480, windowFlags)
        require(primaryWindow != 0L) { "failed to create primary window" }
        primaryGlContext = SDL_GL_CreateContext(primaryWindow)
        require(primaryGlContext != 0L) { "failed to create primary GL context. ${SDL_GetError()}" }

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

        println("driver version: ${glGetString(GL_VERSION)}")
        println("driver vendor: ${glGetString(GL_VENDOR)}")
        println("driver renderer ${glGetString(GL_RENDERER)}")


        Driver.driver = DriverGLSDL(driverVersion)
    }

    override suspend fun setup() {
        createPrimaryWindow()
        SDL_GL_MakeCurrent(primaryWindow, primaryGlContext)

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
        )
        program.driver = Driver.instance
        program.drawer = Drawer(Driver.instance)

        Animatable.clock(object : Clock {
            override val time: Long
                get() = (program.seconds * 1E3).toLong()
            override val timeNanos: Long
                get() = (program.seconds * 1E6).toLong()
        })
        window = createApplicationWindowSDL(this, wc, program, program.drawer)
        windowsById[SDL_GetWindowID(window.window)] = window
    }

    private inline fun Vector2.toDisplayUnits(scale: Double):Vector2 {
        return if (!fixWindowSize) this else this / scale
    }

    private fun handleSDLEvent(event: SDL_Event) {
        when (event.type()) {
            SDL_EVENT_QUIT -> {
                logger.info { "received quit event" }
                exitRequested = true
            }

            SDL_EVENT_WINDOW_EXPOSED -> {
                println("window exposed")
            }
            SDL_EVENT_WINDOW_PIXEL_SIZE_CHANGED -> {
                println("pixel size changed")
            }
            SDL_EVENT_WINDOW_ICCPROF_CHANGED -> {
                println("icc profile changed")
            }
            SDL_EVENT_WINDOW_DISPLAY_CHANGED -> {
                println("display changed")
            }

            SDL_EVENT_WINDOW_DISPLAY_SCALE_CHANGED -> {
                println("display scale changed")
            }

            SDL_EVENT_WINDOW_MOVED -> {
                val windowEvent = event.window()
                val x = windowEvent.data1().toDouble()
                val y = windowEvent.data2().toDouble()
                windowById(event.window().windowID()).program.window.moved.trigger(WindowEvent(WindowEventType.MOVED, Vector2(x, y), Vector2.ZERO, true))
            }

            SDL_EVENT_WINDOW_FOCUS_GAINED -> {
                SDL_SetWindowMouseGrab(event.window().windowID().toLong(), true)
                windowById(event.window().windowID()).program.window.focused.trigger(WindowEvent(WindowEventType.FOCUSED, Vector2.ZERO, Vector2.ZERO, true))
            }

            SDL_EVENT_WINDOW_FOCUS_LOST -> {
                windowById(event.window().windowID()).program.window.unfocused.trigger(
                    WindowEvent(
                        WindowEventType.UNFOCUSED,
                        Vector2.ZERO,
                        Vector2.ZERO,
                        true
                    )
                )
            }

            SDL_EVENT_WINDOW_MOUSE_ENTER -> {
                windowById(event.window().windowID()).program.mouse.entered.trigger(
                    MouseEvent(
                        Vector2.ZERO,
                        Vector2.ZERO,
                        Vector2.ZERO,
                        MouseEventType.ENTERED,
                        MouseButton.NONE,
                        emptySet()
                    )
                )
            }

            SDL_EVENT_WINDOW_MOUSE_LEAVE -> {
                windowById(event.window().windowID()).program.mouse.exited.trigger(
                    MouseEvent(
                        Vector2.ZERO,
                        Vector2.ZERO,
                        Vector2.ZERO,
                        MouseEventType.EXITED,
                        MouseButton.NONE,
                        emptySet()
                    )
                )
            }

            SDL_EVENT_MOUSE_MOTION -> {
                val mouseEvent = event.motion()
                val window = windowById(event.window().windowID())
                val scale = window.windowContentScale
                val cursorPosition = Vector2(mouseEvent.x().toDouble(), mouseEvent.y().toDouble()).toDisplayUnits(scale)
                val displacement = Vector2(mouseEvent.xrel().toDouble(), mouseEvent.yrel().toDouble()).toDisplayUnits(scale)
                window.cursorPosition = cursorPosition

                if (!window.primaryButtonDown) {
                    window.program.mouse.moved.trigger(
                        MouseEvent(
                            cursorPosition, Vector2.ZERO, displacement, MouseEventType.MOVED,
                            MouseButton.NONE, emptySet()
                        )
                    )
                } else {
                    window.program.mouse.dragged.trigger(
                        MouseEvent(
                            cursorPosition, Vector2.ZERO, displacement, MouseEventType.MOVED,
                            MouseButton.NONE, emptySet()
                        )
                    )
                }
            }

            SDL_EVENT_MOUSE_BUTTON_UP -> {
                val mouseEvent = event.button()
                val window = windowById(event.window().windowID())
                val scale =  window.windowContentScale
                val cursorPosition = Vector2(mouseEvent.x().toDouble(), mouseEvent.y().toDouble()).toDisplayUnits(scale)
                window.cursorPosition = cursorPosition
                if (mouseEvent.button() == 1.toByte()) {
                    window.primaryButtonDown = false
                }
                window.program.mouse.buttonUp.trigger(
                    MouseEvent(
                        cursorPosition, Vector2.ZERO, Vector2.ZERO, MouseEventType.BUTTON_UP,
                        MouseButton.LEFT, emptySet()
                    )
                )
            }

            SDL_EVENT_MOUSE_BUTTON_DOWN -> {
                val mouseEvent = event.button()
                val window = windowById(event.window().windowID())
                val scale = window.windowContentScale
                val cursorPosition = Vector2(mouseEvent.x().toDouble(), mouseEvent.y().toDouble()).toDisplayUnits(scale)

                window.cursorPosition = cursorPosition
                if (mouseEvent.button() == 1.toByte()) {
                    window.primaryButtonDown = true
                }
                window.program.mouse.buttonDown.trigger(
                    MouseEvent(
                        cursorPosition, Vector2.ZERO, Vector2.ZERO, MouseEventType.BUTTON_DOWN,
                        MouseButton.LEFT, emptySet()
                    )
                )
            }

            SDL_EVENT_CLIPBOARD_UPDATE -> {
                println("clipboard updated")
            }

            SDL_EVENT_MOUSE_WHEEL -> {
                val mouseEvent = event.wheel()



                val window = windowById(event.window().windowID())
                cursorPosition = Vector2(mouseEvent.mouse_x().toDouble(), mouseEvent.mouse_y().toDouble()).toDisplayUnits(window.windowContentScale)
                val rotation = Vector2(mouseEvent.x().toDouble(), mouseEvent.y().toDouble())
                window.program.mouse.scrolled.trigger(
                    MouseEvent(
                        cursorPosition, rotation, Vector2.ZERO, MouseEventType.BUTTON_DOWN,
                        MouseButton.LEFT, emptySet()
                    )
                )
            }

            SDL_EVENT_FINGER_DOWN -> {
                val fingerEvent = event.tfinger()
                window.program.pointers.pointerDown.trigger(
                    PointerEvent(
                        fingerEvent.fingerID(),
                        Vector2(fingerEvent.x().toDouble(),fingerEvent.y().toDouble()) * program.window.size,
                        Vector2(fingerEvent.dx().toDouble(), fingerEvent.dy().toDouble()) * program.window.size,
                        fingerEvent.pressure().toDouble()
                    )
                )
            }

            SDL_EVENT_FINGER_UP -> {
                val fingerEvent = event.tfinger()
                window.program.pointers.pointerUp.trigger(
                    PointerEvent(
                        fingerEvent.fingerID(),
                        Vector2.ZERO,
                        Vector2.ZERO,
                        0.0)
                )
            }

            SDL_EVENT_FINGER_MOTION -> {
                val fingerEvent = event.tfinger()
                window.program.pointers.moved.trigger(
                    PointerEvent(
                        fingerEvent.fingerID(),
                        Vector2(fingerEvent.x().toDouble(),fingerEvent.y().toDouble()) * program.window.size,
                        Vector2(fingerEvent.dx().toDouble(), fingerEvent.dy().toDouble()) * program.window.size,
                        fingerEvent.pressure().toDouble()
                    )
                )
            }

            SDL_EVENT_FINGER_CANCELED -> {
                val fingerEvent = event.tfinger()
                window.program.pointers.pointerUp.trigger(
                    PointerEvent(
                        fingerEvent.fingerID(),
                        Vector2.ZERO,
                        Vector2.ZERO,
                        0.0)
                )
            }


            SDL_EVENT_TEXT_INPUT -> {
                val textEvent = event.text()
                windowById(event.window().windowID()).program.keyboard.character.trigger(
                    CharacterEvent(textEvent.textString()?.first() ?: ' ', emptySet())
                )
            }

            SDL_EVENT_KEY_UP -> {
                val keyEvent = event.key()
                val modifiers = modifiersFromSdl(keyEvent.mod().toInt())
                windowById(event.window().windowID()).program.keyboard.keyUp.trigger(
                    KeyEvent(
                        KeyEventType.KEY_UP,
                        keyEvent.key(),
                        "not implemented yet",
                        modifiers
                    )
                )
            }

            SDL_EVENT_KEY_DOWN -> {
                val keyEvent = event.key()
                val modifiers = modifiersFromSdl(keyEvent.mod().toInt())
                windowById(event.window().windowID()).program.keyboard.keyUp.trigger(
                    KeyEvent(
                        KeyEventType.KEY_DOWN,
                        keyEvent.key(),
                        "not implemented yet",
                        modifiers
                    )
                )
            }
            SDL_EVENT_WINDOW_CLOSE_REQUESTED -> {
                //println("close requested")
                windowById(event.window().windowID()).closeRequested = true
            }

            else -> {
                //println("got event: ${event.type()}")
            }
        }
    }

    override fun loop() {
        defaultRenderTarget.bind()


        window.setupSizes()
        runBlocking {
            program.setup()
        }

        val event = SDL_Event.create()
        while (!exitRequested && !window.closeRequested) {
            while(SDL_PollEvent(event)) {
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