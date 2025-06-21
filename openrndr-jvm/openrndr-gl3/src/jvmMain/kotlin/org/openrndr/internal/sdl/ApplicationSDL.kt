package org.openrndr.internal.sdl

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.lwjgl.opengl.GL11.GL_VERSION
import org.lwjgl.opengl.GL30.GL_MAJOR_VERSION
import org.lwjgl.opengl.GL30.GL_MINOR_VERSION
import org.lwjgl.sdl.SDLEvents.*
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
import org.openrndr.internal.gl3.DriverGL3
import org.openrndr.internal.gl3.DriverGL3Configuration
import org.openrndr.internal.gl3.DriverTypeGL
import org.openrndr.internal.gl3.DriverVersionGL
import org.openrndr.internal.gl3.ProgramRenderTargetGL3
import org.openrndr.internal.gl3.glBindVertexArray
import org.openrndr.internal.gl3.glGetInteger
import org.openrndr.internal.gl3.glGetString
import org.openrndr.internal.gl3.glViewport
import org.openrndr.math.Vector2
import kotlin.use

private val logger = KotlinLogging.logger {}

class ApplicationSDL(override var program: Program, override var configuration: Configuration) : Application() {

    init {
        program.application = this
    }
    var window = 0L
    var glContext = 0L

    private val vaos = IntArray(1)

    override fun requestDraw() {
        TODO("Not yet implemented")
    }

    override fun requestFocus() {
        TODO("Not yet implemented")
    }

    override fun exit() {
        TODO("Not yet implemented")
    }

    val defaultRenderTarget by lazy { ProgramRenderTargetGL3(program) }


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
            logger.info { "window content scale: ${program.window.contentScale}" }


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
//        SDL_InitSubSystem(SDL_INIT_VIDEO)
        window = SDL_CreateWindow(configuration.title, configuration.width, configuration.height, flags)

        glContext = SDL_GL_CreateContext(window)

        val caps = org.lwjgl.opengl.GL.createCapabilities()

        println(glGetString(GL_VERSION))
        println("${glGetInteger( GL_MAJOR_VERSION)}.${glGetInteger( GL_MINOR_VERSION)}")


        val driverVersion = DriverVersionGL.find(DriverTypeGL.GL, glGetInteger( GL_MAJOR_VERSION), glGetInteger( GL_MINOR_VERSION)) ?: error("unsupported driver version")

        Driver.driver = DriverGL3(driverVersion)
        println(DriverGL3Configuration.driverType)



    }

    fun handleSDLEvent(event: SDL_Event) {
        when (event.type()) {
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

            else -> {
                println("got event: ${event.type()}")
            }
        }
    }

    override fun loop() {

        SDL_ShowWindow(window)


        defaultRenderTarget.bind()

        program.driver = Driver.instance
        program.drawer = Drawer(Driver.instance)
        runBlocking {
            program.setup()
        }

        MemoryStack.stackPush().use { stack ->
            val event = SDL_Event.calloc(stack)

            while (true) {
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
        TODO("Not yet implemented")
    }


}