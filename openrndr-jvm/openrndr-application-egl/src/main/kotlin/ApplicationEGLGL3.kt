package org.openrndr.internal.gl3

import kotlinx.coroutines.runBlocking
import io.github.oshai.kotlinlogging.KotlinLogging
import org.lwjgl.BufferUtils
import org.lwjgl.egl.EGL15.*
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL33C.*
import org.lwjgl.system.MemoryStack.stackPush
import org.openrndr.*
import org.openrndr.animatable.Animatable
import org.openrndr.animatable.Clock
import org.openrndr.draw.DrawThread
import org.openrndr.draw.Drawer
import org.openrndr.draw.Session
import org.openrndr.draw.renderTarget
import org.openrndr.internal.Driver
import org.openrndr.internal.ResourceThread
import org.openrndr.math.Vector2

private val logger = KotlinLogging.logger {}

@Suppress("UNUSED_PARAMETER")
class ApplicationEGLGL3(override var program: Program, override var configuration: Configuration) : Application() {

    override var cursorVisible: Boolean = false
    override var cursorHideMode: MouseCursorHideMode = MouseCursorHideMode.HIDE

    override var cursorType: CursorType = CursorType.ARROW_CURSOR

    override var cursorPosition: Vector2
        get() = Vector2(0.0, 0.0)
        set(value) {}
    private var driver = object: DriverGL3(DriverVersionGL.GL_VERSION_3_3) {
        override val contextID: Long
            get() = eglGetCurrentContext()

        override fun createResourceThread(
            session: Session?,
            f: () -> Unit
        ): ResourceThread {
            TODO("Not yet implemented")
        }

        override fun createDrawThread(session: Session?): DrawThread {
            TODO("Not yet implemented")
        }

    }
    private var exitRequested = false
    private var startTime = System.currentTimeMillis()
    private val vaos = IntArray(1)

    init {
        Driver.driver = driver
        program.application = this
    }

    override fun requestDraw() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun requestFocus() {

    }

    override fun exit() {
        exitRequested = true
    }

    override fun windowClose() {

    }

    override fun windowMaximize() {

    }

    override fun windowMinimize() {

    }

    override fun windowFullscreen(mode: Fullscreen) {

    }


    override suspend fun setup() {

        val display = eglGetDisplay(EGL_DEFAULT_DISPLAY)
        println("display $display")
        val major = BufferUtils.createIntBuffer(1)
        val minor = BufferUtils.createIntBuffer(1)

        eglInitialize(display, major, minor)

        println("EGL version ${major[0]}.${minor[0]}")

        stackPush().use {
            val configCount = it.mallocInt(1)

            val attributes = it.ints(
                EGL_SURFACE_TYPE, EGL_PBUFFER_BIT,
                EGL_BLUE_SIZE, 8,
                EGL_GREEN_SIZE, 8,
                EGL_RED_SIZE, 8,
                EGL_DEPTH_SIZE, 24,
                EGL_STENCIL_SIZE, 8,
                EGL_RENDERABLE_TYPE, EGL_OPENGL_BIT,
                EGL_NONE
            )

            if (!eglChooseConfig(display, attributes, null, configCount)) {
                throw RuntimeException("${eglGetError()}")
            }

            println("number of configs found : ${configCount[0]}")
            val configs = it.mallocPointer(configCount[0])
            eglChooseConfig(display, attributes, configs, configCount)

            val surface = eglCreatePbufferSurface(display, configs[0], attributes)

            eglBindAPI(EGL_OPENGL_API)
            val EGL_CONTEXT_FLAGS = 0x30FC

            val contextAttributes = it.ints(
                EGL_CONTEXT_MAJOR_VERSION, 3,
                EGL_CONTEXT_MINOR_VERSION, 3,
                EGL_CONTEXT_FLAGS, EGL_CONTEXT_OPENGL_CORE_PROFILE_BIT,
                EGL_NONE
            )

            val context = eglCreateContext(display, configs[0], EGL_NO_CONTEXT, contextAttributes)
            println("context: $context")
            println("client api ${eglQueryString(display, EGL_CLIENT_APIS)}")
            eglMakeCurrent(display, surface, surface, context)
            GL.createCapabilities()
            println("${glGetString(GL_VENDOR)}")
            println("opengl version: ${glGetString(GL_VERSION)}")

            glGenVertexArrays(vaos)
            glBindVertexArray(vaos[0])

            startTime = System.currentTimeMillis()


            Animatable.clock(object : Clock {
                override val time: Long
                    get() = (program.seconds * 1E3).toLong()
                override val timeNanos: Long
                    get() = (program.seconds * 1E6).toLong()
            })


            val defaultRenderTarget = ProgramRenderTargetGL3(program)
            defaultRenderTarget.bind()

            setupPreload(program, configuration)

            program.drawer = Drawer(driver)
            runBlocking {
                program.setup()
            }

        }
    }

    override fun loop() {
        val defaultRenderTarget = renderTarget(configuration.width, configuration.height) {
            colorBuffer()
            depthBuffer()
        }
        defaultRenderTarget.bind()

        while (!exitRequested) {
            glBindVertexArray(vaos[0])
            @Suppress("DEPRECATION")
            program.drawer.reset()
            program.drawer.ortho()
            program.drawImpl()
        }
        for (extension in program.extensions) {
            extension.shutdown(program)
        }

        program.ended.trigger(ProgramEvent(ProgramEventType.ENDED))
    }

    override var clipboardContents: String?
        get() = ""
        set(value) {}
    override var windowTitle: String
        get() = ""
        set(value) {}
    override var windowPosition: Vector2
        get() = Vector2.ZERO
        set(value) {}
    override var windowSize: Vector2
        get() = Vector2(configuration.width.toDouble(), configuration.height.toDouble())
        set(value) {}

    override var windowMultisample: WindowMultisample
        get() = WindowMultisample.Disabled
        set(value) {
            logger.warn { "Setting window multisampling is not supported" }
        }
    override var windowResizable: Boolean
        get() = false
        set(value) {
            if (value) {
                logger.warn { "Resizable windows are not supported" }
            }
        }

    override val seconds: Double
        get() = (System.currentTimeMillis() - startTime) / 1000.0
    override var presentationMode: PresentationMode
        get() = PresentationMode.AUTOMATIC
        set(value) {}
    override var windowContentScale: Double
        get() = 1.0
        set(value) {}

    override fun createChildWindow(configuration: WindowConfiguration, program: Program): ApplicationWindow {
        TODO("Not yet implemented")
    }

}