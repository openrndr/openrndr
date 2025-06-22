package org.openrndr.internal.sdl

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.lwjgl.glfw.GLFW.glfwGetCurrentContext
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.GL_RENDERER
import org.lwjgl.opengl.GL11.GL_VENDOR
import org.lwjgl.opengl.GL11.GL_VERSION
import org.lwjgl.opengl.GL30.GL_MAJOR_VERSION
import org.lwjgl.opengl.GL30.GL_MINOR_VERSION
import org.lwjgl.opengles.GLES
import org.lwjgl.sdl.SDLError.SDL_GetError
import org.lwjgl.sdl.SDLEvents.*
import org.lwjgl.sdl.SDLKeyboard.SDL_StartTextInput
import org.lwjgl.sdl.SDLKeycode.*
import org.lwjgl.sdl.SDLTimer.SDL_Delay
import org.lwjgl.sdl.SDLTimer.SDL_GetTicks
import org.lwjgl.sdl.SDLVideo.*
import org.lwjgl.sdl.SDL_Event
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.stackPush
import org.openrndr.*
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.internal.Driver
import org.openrndr.internal.gl3.*
import org.openrndr.math.Vector2
import java.util.concurrent.CopyOnWriteArrayList

private val logger = KotlinLogging.logger {}

class ApplicationSDL(override var program: Program, override var configuration: Configuration) : Application() {

    internal val windows: CopyOnWriteArrayList<ApplicationWindowSDL> = CopyOnWriteArrayList()

    init {
        program.application = this
    }
    private var defaultRenderTargetGL3: ProgramRenderTargetGL3? = null
    private var primaryWindow: Long = 0L
    private var primaryGlContext: Long = 0L

    private var window = 0L
    private var glContext = 0L

    private val vaos = IntArray(1)

    override fun requestFocus() {
        TODO("Not yet implemented")
    }

    val defaultRenderTarget by lazy { ProgramRenderTargetGL3(program) }

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
        println("driver bla ${glGetString(GL_RENDERER)}")


        Driver.driver = DriverGLSDL(driverVersion)
    }


    private fun drawFrame(): Throwable? {
        // reset cached values
        setupSizes()

        defaultRenderTarget.bindTarget()

        glBindVertexArray(vaos[0])
        @Suppress("DEPRECATION")
        program.drawer.reset()
        program.drawer.ortho()
        deliverEvents()
        program.dispatcher.execute()
        try {
            logger.trace { "window: ${program.window.size.x.toInt()}x${program.window.size.y.toInt()} program: ${program.width}x${program.height}" }
            program.drawImpl()
        } catch (e: Throwable) {
            logger.error { "Caught exception inside the program loop. (${e.message})" }
            return e
        }
        return null
    }

    private fun setupSizes() {
        stackPush().use { stack ->

            program.window.contentScale = SDL_GetWindowDisplayScale(window).toDouble()


            val fbw = stack.mallocInt(1)
            val fbh = stack.mallocInt(1)

            SDL_GetWindowSizeInPixels(window, fbw, fbh)
            logger.trace { "window framebuffer size: ${fbw.get(0)}x${fbh.get(0)}" }
            glViewport(0, 0, fbw[0], fbh[0])

            SDL_GetWindowSize(window, fbw, fbh)
            logger.trace { "window size: ${fbw.get(0)}x${fbh.get(0)}" }

            program.width = fbw[0]
            program.height = fbh[0]
        }
    }

    override suspend fun setup() {

        createPrimaryWindow()
        SDL_GL_MakeCurrent(primaryWindow, primaryGlContext)

        var flags = 0L
        if (configuration.hideWindowDecorations) {
            flags = flags or SDL_WINDOW_BORDERLESS
        }
        if (configuration.windowAlwaysOnTop) {
            flags = flags or SDL_WINDOW_ALWAYS_ON_TOP
        }
        if (configuration.windowResizable) {
            flags = flags or SDL_WINDOW_RESIZABLE
        }
        if (configuration.windowTransparent) {
            flags = flags or SDL_WINDOW_TRANSPARENT
        }

        flags = flags or SDL_WINDOW_OPENGL
        flags = flags or SDL_WINDOW_HIGH_PIXEL_DENSITY


        SDL_GL_SetAttribute(SDL_GL_DOUBLEBUFFER, 1);
        SDL_GL_SetAttribute(SDL_GL_ACCELERATED_VISUAL, 1);
        SDL_GL_SetAttribute(SDL_GL_CONTEXT_MAJOR_VERSION, 3)
        SDL_GL_SetAttribute(SDL_GL_RED_SIZE, 8);
        SDL_GL_SetAttribute(SDL_GL_GREEN_SIZE, 8);
        SDL_GL_SetAttribute(SDL_GL_BLUE_SIZE, 8);
        SDL_GL_SetAttribute(SDL_GL_ALPHA_SIZE, 8);
        SDL_GL_SetAttribute(SDL_GL_CONTEXT_PROFILE_MASK, SDL_GL_CONTEXT_PROFILE_CORE)

        SDL_GL_SetAttribute(SDL_GL_CONTEXT_MAJOR_VERSION, 4)
        SDL_GL_SetAttribute(SDL_GL_CONTEXT_MINOR_VERSION, 0)
        SDL_GL_SetAttribute(SDL_GL_CONTEXT_PROFILE_MASK, SDL_GL_CONTEXT_PROFILE_CORE)
        SDL_GL_SetAttribute(SDL_GL_SHARE_WITH_CURRENT_CONTEXT, 1)
        SDL_GL_SetAttribute(SDL_GL_FRAMEBUFFER_SRGB_CAPABLE, 1)

        SDL_GL_SetAttribute(SDL_GL_DEPTH_SIZE, 24)
        SDL_GL_SetAttribute(SDL_GL_STENCIL_SIZE, 8)

        when (val c = configuration.multisample) {
            is WindowMultisample.Disabled -> {}
            is WindowMultisample.SystemDefault -> {}
            is WindowMultisample.SampleCount -> SDL_GL_SetAttribute(SDL_GL_MULTISAMPLESAMPLES, c.count)
        }
        if (DriverGL3Configuration.driverType == DriverTypeGL.GLES) {
            SDL_GL_SetAttribute(SDL_GL_CONTEXT_MAJOR_VERSION, Driver.glVersion.majorVersion)
            SDL_GL_SetAttribute(SDL_GL_CONTEXT_MINOR_VERSION, Driver.glVersion.minorVersion)
            SDL_GL_SetAttribute(SDL_GL_CONTEXT_PROFILE_MASK, SDL_GL_CONTEXT_PROFILE_ES)
        } else {
            SDL_GL_SetAttribute(SDL_GL_CONTEXT_FLAGS, SDL_GL_CONTEXT_FORWARD_COMPATIBLE_FLAG)
            SDL_GL_SetAttribute(SDL_GL_CONTEXT_MAJOR_VERSION, Driver.glVersion.majorVersion)
            SDL_GL_SetAttribute(SDL_GL_CONTEXT_MINOR_VERSION, Driver.glVersion.minorVersion)
            SDL_GL_SetAttribute(SDL_GL_CONTEXT_PROFILE_MASK, SDL_GL_CONTEXT_PROFILE_CORE)
        }

        window = SDL_CreateWindow(configuration.title, configuration.width, configuration.height, flags)
        require(window != 0L) { "failed to create window" }


        glContext = SDL_GL_CreateContext(window)
        require(glContext != 0L) { "failed to create GL context. ${SDL_GetError()}" }
        SDL_GL_MakeCurrent(window, glContext)
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

    fun handleSDLEvent(event: SDL_Event) {
        when (event.type()) {
            SDL_EVENT_QUIT -> {
                exit()
            }

            SDL_EVENT_WINDOW_MOVED -> {
                val windowEvent = event.window()
                val x = windowEvent.data1().toDouble()
                val y = windowEvent.data2().toDouble()
                program.window.moved.trigger(WindowEvent(WindowEventType.MOVED, Vector2(x, y), Vector2.ZERO, true))
            }

            SDL_EVENT_WINDOW_FOCUS_GAINED -> {
                program.window.focused.trigger(WindowEvent(WindowEventType.FOCUSED, Vector2.ZERO, Vector2.ZERO, true))
            }

            SDL_EVENT_WINDOW_FOCUS_LOST -> {
                program.window.unfocused.trigger(
                    WindowEvent(
                        WindowEventType.UNFOCUSED,
                        Vector2.ZERO,
                        Vector2.ZERO,
                        true
                    )
                )
            }

            SDL_EVENT_WINDOW_MOUSE_ENTER -> {
                program.mouse.entered.trigger(
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
                program.mouse.exited.trigger(
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
                cursorPosition = Vector2(mouseEvent.x().toDouble(), mouseEvent.y().toDouble())
                program.mouse.moved.trigger(
                    MouseEvent(
                        cursorPosition, Vector2.ZERO, Vector2.ZERO, MouseEventType.MOVED,
                        MouseButton.NONE, emptySet()
                    )
                )
            }

            SDL_EVENT_MOUSE_BUTTON_UP -> {
                val mouseEvent = event.button()
                cursorPosition = Vector2(mouseEvent.x().toDouble(), mouseEvent.y().toDouble())
                program.mouse.buttonUp.trigger(
                    MouseEvent(
                        cursorPosition, Vector2.ZERO, Vector2.ZERO, MouseEventType.BUTTON_UP,
                        MouseButton.LEFT, emptySet()
                    )
                )
            }

            SDL_EVENT_MOUSE_BUTTON_DOWN -> {
                val mouseEvent = event.button()
                cursorPosition = Vector2(mouseEvent.x().toDouble(), mouseEvent.y().toDouble())
                program.mouse.buttonUp.trigger(
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

            }

            SDL_EVENT_FINGER_DOWN -> {
                val fingerEvent = event.tfinger()
            }

            SDL_EVENT_FINGER_UP -> {
                val fingerEvent = event.tfinger()
                //println(fingerEvent.fingerID())
            }

            SDL_EVENT_FINGER_MOTION -> {
                val fingerEvent = event.tfinger()
            }

            SDL_EVENT_FINGER_CANCELED -> {
                val fingerEvent = event.tfinger()
            }


            SDL_EVENT_TEXT_INPUT -> {
                val textEvent = event.text()
                program.keyboard.character.trigger(
                    CharacterEvent(textEvent.textString()?.first() ?: ' ', emptySet())
                )
            }

            SDL_EVENT_KEY_UP -> {
                val keyEvent = event.key()
                val modifiers = modifiersFromSdl(keyEvent.mod().toInt())
                program.keyboard.keyUp.trigger(
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
                program.keyboard.keyUp.trigger(
                    KeyEvent(
                        KeyEventType.KEY_DOWN,
                        keyEvent.key(),
                        "not implemented yet",
                        modifiers
                    )
                )
            }

            else -> {
                println("got event: ${event.type()}")
            }
        }
    }

    override fun loop() {

        SDL_ShowWindow(window)


        SDL_StartTextInput(window)
        defaultRenderTarget.bind()

        program.driver = Driver.instance
        program.drawer = Drawer(Driver.instance)
        runBlocking {
            program.setup()
        }

        MemoryStack.stackPush().use { stack ->
            val event = SDL_Event.calloc(stack)

            while (!exitRequested) {
                while (SDL_PollEvent(event)) {
                    handleSDLEvent(event)
                }
                Driver.instance.clear(ColorRGBa.BLACK)

                drawFrame()


                SDL_GL_SwapWindow(window);
                SDL_Delay(1);

            }
        }
    }

    override var clipboardContents: String?
        get() = TODO("Not yet implemented")
        set(value) {}
    override var windowTitle: String
        get() = TODO("Not yet implemented")
        set(value) {}
    override var windowPosition: Vector2
        get() = TODO("Not yet implemented")
        set(value) {}
    override var windowSize: Vector2
        get() {
            MemoryStack.stackPush().use { stack ->
                val x = stack.mallocInt(1)
                val y = stack.mallocInt(1)
                SDL_GetWindowSize(window, x, y)
                return Vector2(x.get(0).toDouble(), y.get(0).toDouble())
            }

        }
        set(value) {}
    override var windowResizable: Boolean
        get() = TODO("Not yet implemented")
        set(value) {}
    override var windowMultisample: WindowMultisample
        get() = TODO("Not yet implemented")
        set(value) {}
    override var cursorPosition: Vector2
        get() = field
        set(value) {
            field = value
        }
    override var cursorVisible: Boolean
        get() = TODO("Not yet implemented")
        set(value) {}
    override var cursorHideMode: MouseCursorHideMode
        get() = TODO("Not yet implemented")
        set(value) {}
    override var cursorType: CursorType
        get() = TODO("Not yet implemented")
        set(value) {}
    override val pointers: List<Pointer>
        get() = TODO("Not yet implemented")
    override val seconds: Double
        get() = SDL_GetTicks() / 1000.0
    override var presentationMode: PresentationMode
        get() = TODO("Not yet implemented")
        set(value) {}
    override var windowContentScale: Double
        get() =
            SDL_GetWindowDisplayScale(window).toDouble()
        set(value) {}

    override fun createChildWindow(
        configuration: WindowConfiguration,
        program: Program
    ): ApplicationWindow {
        // acquire current context, we may be calling this from another context that we want to return to
        //val currentActiveContext = SDL_GL_GetCurrentContext()

        return createApplicationWindowSDL(this, configuration, program)
    }
}