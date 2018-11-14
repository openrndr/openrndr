package org.openrndr.internal.gl3

import org.lwjgl.BufferUtils
import org.lwjgl.egl.EGL15.*
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL33C.*
import org.lwjgl.system.MemoryStack.stackPush
import org.openrndr.Application
import org.openrndr.Configuration
import org.openrndr.PresentationMode
import org.openrndr.Program
import org.openrndr.draw.Drawer
import org.openrndr.internal.Driver
import org.openrndr.math.Vector2

@Suppress("UNUSED_PARAMETER")
class ApplicationEGLGL3(private val program: Program, private val configuration: Configuration):Application() {
    private var driver: DriverGL3
    private var exitRequested = false
    private var startTime = System.currentTimeMillis()
    private val vaos = IntArray(1)

    init {
        driver = DriverGL3()
        Driver.driver = driver
    }

    override fun requestDraw() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun exit() {
        exitRequested = true
    }

    override fun setup() {
        val display = eglGetDisplay(EGL_DEFAULT_DISPLAY)
        println("display $display")
        val major = BufferUtils.createIntBuffer(1)
        val minor = BufferUtils.createIntBuffer(1)

        eglInitialize(display, major, minor)

        println("EGL version ${major[0]}.${minor[0]}")

         stackPush().use {
             val configCount = it.mallocInt(1)

             val attributes = it.ints(EGL_SURFACE_TYPE, EGL_PBUFFER_BIT,
                     EGL_BLUE_SIZE, 8,
                     EGL_GREEN_SIZE, 8,
                     EGL_RED_SIZE, 8,
                     EGL_DEPTH_SIZE, 24,
                     EGL_STENCIL_SIZE, 8,
                     EGL_RENDERABLE_TYPE, EGL_OPENGL_BIT,
                     EGL_NONE)

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
                     EGL_NONE)

             val context = eglCreateContext(display, configs[0], EGL_NO_CONTEXT,contextAttributes)
             println("context: $context")
             println("client api ${eglQueryString(display, EGL_CLIENT_APIS)}" )
             eglMakeCurrent(display, surface, surface, context)
             GL.createCapabilities()
             println("${glGetString(GL_VENDOR)}")
             println( "opengl version: ${glGetString(GL_VERSION)}" )

             glGenVertexArrays(vaos)
             glBindVertexArray(vaos[0])

             startTime = System.currentTimeMillis()
             val defaultRenderTarget = ProgramRenderTargetGL3(program)
             defaultRenderTarget.bind()
             program.drawer = Drawer(driver)
             program.setup()

         }
    }

    override fun loop() {
        while (!exitRequested) {
            glBindVertexArray(vaos[0])
            program.drawer.reset()
            program.drawer.ortho()
            program.draw()
        }
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
    override val seconds: Double
        get() = (System.currentTimeMillis()-startTime)/1000.0
    override var presentationMode: PresentationMode
        get() = PresentationMode.AUTOMATIC
        set(value) {}
}