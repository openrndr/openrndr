package org.openrndr.internal.gl3

import mu.KotlinLogging

import java.lang.IllegalStateException
import java.lang.RuntimeException
import java.io.File

import org.lwjgl.PointerBuffer
import org.lwjgl.glfw.Callbacks.glfwFreeCallbacks
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.system.MemoryUtil.NULL
import org.lwjgl.glfw.GLFW.glfwSetWindowPos
import org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor
import org.lwjgl.glfw.GLFW.glfwGetVideoMode
import org.lwjgl.opengl.GL.createCapabilities
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.glfw.GLFW.glfwPollEvents
import org.lwjgl.glfw.GLFW.glfwSwapBuffers
import org.lwjgl.glfw.GLFW.glfwWindowShouldClose
import org.lwjgl.glfw.GLFW.GLFW_RELEASE
import org.lwjgl.glfw.GLFW.glfwSetKeyCallback
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL30.glBindVertexArray
import org.lwjgl.opengl.GL30.glGenVertexArrays
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.ARBDebugOutput.glDebugMessageCallbackARB
import org.lwjgl.opengl.GL43.glDebugMessageCallback
import org.lwjgl.opengl.GLUtil

import org.openrndr.*
import org.openrndr.draw.Drawer
import org.openrndr.internal.Driver
import org.openrndr.math.Vector2
import java.util.*

private val logger = KotlinLogging.logger {}

private var primaryWindow:Long = NULL

class ApplicationGL3(private val program: Program, private val configuration: Configuration) : Application() {


    private var windowFocused = true

    var window: Long = NULL
    private var driver: DriverGL3
    private var realWindowTitle = configuration.title

    private var exitRequested = false

    override var windowPosition: Vector2
        get() {
            val x = IntArray(1)
            val y = IntArray(1)
            glfwGetWindowPos(window, x, y)
            return Vector2(x[0].toDouble(), y[0].toDouble())
        }
        set(value) {
            glfwSetWindowPos(window, value.x.toInt(), value.y.toInt())
        }

    override var clipboardContents: String?
        get() {
            try {
                val result = glfwGetClipboardString(window)
                return result
            } catch (e: Exception) {
                return ""
            }
        }
        set(value) {
            if (value != null) {
                glfwSetClipboardString(window, value)
            } else {
                throw RuntimeException("clipboard contents can't be null")
            }
        }

    var startTimeMillis = System.currentTimeMillis()
    override val seconds: Double
        get() = (System.currentTimeMillis() - startTimeMillis) / 1000.0


    override var windowTitle: String
        get() = realWindowTitle
        set(value) {
            glfwSetWindowTitle(window, value)
            realWindowTitle = value
        }

    companion object {
        var once = true
    }

    init {
        logger.debug { "debug output enabled" }
        logger.trace { "trace level enabled" }

        driver = DriverGL3()
        Driver.driver = driver
        program.application = this


        createPrimaryWindow()

    }

    override fun setup() {
        glfwDefaultWindowHints()
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3)
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3)
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE)
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE)

        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE)
//       glfwWindowHint(GLFW_DECORATED, GLFW_FALSE)

        glfwWindowHint(GL_RED_BITS, 8)
        glfwWindowHint(GL_GREEN_BITS, 8)
        glfwWindowHint(GL_BLUE_BITS, 8)
        glfwWindowHint(GLFW_STENCIL_BITS, 8)
        glfwWindowHint(GLFW_DEPTH_BITS, 24)


        // should make a configuration flag for this
        // glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GLFW_TRUE)

        val xscale = FloatArray(1)
        val yscale = FloatArray(1)
        glfwGetMonitorContentScale(glfwGetPrimaryMonitor(), xscale, yscale)

        if (configuration.fullscreen) {
            xscale[0] = 1.0f
            yscale[0] = 1.0f
        }

        logger.debug { "content scale ${xscale[0]} ${yscale[0]}" }
        program.window.scale = Vector2(xscale[0].toDouble(), yscale[0].toDouble())


        logger.debug { "creating window" }
        window = if (!configuration.fullscreen) {
            val adjustedWidth = (xscale[0] * configuration.width).toInt()
            val adjustedHeight = (yscale[0] * configuration.height).toInt()

            glfwCreateWindow(adjustedWidth,
                    adjustedHeight,
                    configuration.title, NULL, primaryWindow)
        } else {
            logger.info { "creating fullscreen window" }

            var requestWidth = configuration.width
            var requestHeight = configuration.height

            if (requestWidth == -1 || requestHeight == -1) {
                val mode = glfwGetVideoMode(glfwGetPrimaryMonitor())
                if (mode != null) {
                    requestWidth = mode.width()
                    requestHeight = mode.height()
                } else {
                    throw RuntimeException("failed to determine current video mode")
                }
            }

            glfwCreateWindow(requestWidth,
                    requestHeight,
                    configuration.title, glfwGetPrimaryMonitor(), primaryWindow)


        }

        val mode = glfwGetVideoMode(glfwGetPrimaryMonitor())

        logger.debug { "window created: $window" }

        if (window == NULL) {
            throw RuntimeException("Failed to create the GLFW window")
        }

        // Get the thread stack and push a new frame
        stackPush().let { stack ->
            val pWidth = stack.mallocInt(1) // int*
            val pHeight = stack.mallocInt(1) // int*


            // Get the window size passed to glfwCreateWindow
            glfwGetWindowSize(window, pWidth, pHeight)

            // Get the resolution of the primary monitor
            val vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor())


            if (configuration.position == null) {
                if (vidmode != null) {
                    // Center the window
                    glfwSetWindowPos(
                            window,
                            (vidmode.width() - pWidth.get(0)) / 2,
                            (vidmode.height() - pHeight.get(0)) / 2
                    )
                }
            } else {
                configuration.position?.let {
                    glfwSetWindowPos(window, it.x, it.y)
                }
            }
            Unit
        }

        //val adjustedWidth = (configuration.width * xscale[0]).toInt()
        //val adjustedHeight = (configuration.height * yscale[0]).toInt()

        //glfwSetWindowSize(window, adjustedWidth, adjustedHeight)

        logger.debug { "making context current" }
        glfwMakeContextCurrent(window)
        // Enable v-sync
        glfwSwapInterval(1)

        var readyFrames = 0




        glfwSetFramebufferSizeCallback(window) { window, width, height ->
            logger.debug { "resizing window to ${width}x${height} " }

            val xscale = FloatArray(1)
            val yscale = FloatArray(1)

            glfwGetWindowContentScale(window, xscale, yscale)

            val fbw = IntArray(1)
            val fbh = IntArray(1)

            glfwGetFramebufferSize(window, fbw, fbh)

            val fl = IntArray(1)
            val ft = IntArray(1)
            val fr = IntArray(1)
            val fb = IntArray(1)


            val monitor = glfwGetPrimaryMonitor()
            val mode = glfwGetVideoMode(monitor)

            glfwGetWindowFrameSize(window, fl, ft, fr, fb)
            logger.debug {
                "window scale: ${xscale[0]} ${yscale[0]}"
            }

            val ww = IntArray(1)
            val wh = IntArray(1)
            glfwGetWindowSize(window, ww, wh)

            logger.debug { "window frame size: ${fr[0] - fl[0]} ${fb[0] - ft[0]} ${fl[0]} ${fr[0]} ${ft[0]} ${fb[0]}" }
            logger.debug { "window size      : ${ww[0]} ${wh[0]}" }
            logger.debug { "frame buffer size: ${fbw[0]} ${fbh[0]}" }

            program.window.scale = Vector2(xscale[0].toDouble(), yscale[0].toDouble())
            program.window.size = Vector2(width.toDouble(), height.toDouble())


            if (readyFrames > 0) {
                glClearColor(0.5f, 0.5f, 0.5f, 1.0f)
                glClear(GL_COLOR_BUFFER_BIT)
                glfwGetFramebufferSize(window, fbw, fbh)
                program.window.size = Vector2(fbw[0] * 1.0, fbh[0] * 1.0)

                program.width = (fbw[0] / program.window.scale.x).toInt() //(program.window.size.x/program.window.scale.x).toInt()
                program.height = (fbh[0] / program.window.scale.y).toInt() // (program.window.size.y/program.window.scale.y).toInt()
                program.drawer.width = program.width
                program.drawer.height = program.height

                program.drawer.reset()
                program.drawer.ortho()

                program.keyboard.keyDown.deliver()
                program.keyboard.keyUp.deliver()
                program.keyboard.keyRepeat.deliver()
                program.mouse.moved.deliver()
                program.mouse.buttonDown.deliver()
                program.mouse.buttonUp.deliver()
                program.mouse.dragged.deliver()

                try {
                    logger.debug { program.window.size }
                    logger.debug { "${program.width} ${program.height}" }
                    glViewport(0, 0, fbw[0], fbh[0])
                    program.drawImpl()
                } catch (e: Throwable) {
                    logger.error { "caught exception, breaking animation loop" }
                    //                  exception = e
//                    break
                }
                glfwSwapBuffers(window)
            }
            readyFrames++
            logger.debug { "all ok" }
        }

        glfwSetWindowPosCallback(window) { _, x, y ->
            logger.debug { "window has moved to $x $y" }
            program.window.moved.trigger(WindowEvent(WindowEventType.MOVED, Vector2(x.toDouble(), y.toDouble()), Vector2(0.0, 0.0), true))
        }


        glfwSetWindowFocusCallback(window) { _, focused ->
            logger.debug { "window focus has changed; focused=$focused" }
            windowFocused = focused
            if (focused) {

                program.window.focused.trigger(
                        WindowEvent(WindowEventType.FOCUSED, Vector2(0.0, 0.0), Vector2(0.0, 0.0), true))
            } else {
                program.window.unfocused.trigger(
                        WindowEvent(WindowEventType.FOCUSED, Vector2(0.0, 0.0), Vector2(0.0, 0.0), false))
            }
        }
        logger.debug { "glfw version: ${glfwGetVersionString()}" }
        logger.debug { "showing window" }
        glfwShowWindow(window)

    }

    private fun createPrimaryWindow() {
        if (primaryWindow == NULL) {

            glfwSetErrorCallback(GLFWErrorCallback.create { error, description ->
                logger.error(
                        "LWJGL Error - Code: {}, Description: {}",
                        Integer.toHexString(error),
                        GLFWErrorCallback.getDescription(description)
                )
            })

            if (!glfwInit()) {
                throw IllegalStateException("Unable to initialize GLFW")
            }

            glfwDefaultWindowHints()
            glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3)
            glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3)
            glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE)
            glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE)
            glfwWindowHint(GL_RED_BITS, 8)
            glfwWindowHint(GL_GREEN_BITS, 8)
            glfwWindowHint(GL_BLUE_BITS, 8)
            glfwWindowHint(GLFW_STENCIL_BITS, 8)
            glfwWindowHint(GLFW_DEPTH_BITS, 24)

            glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
            primaryWindow = glfwCreateWindow(640, 480, "OPENRNDR primary window", NULL, NULL)
        }
    }
    fun cb(source:Int, type:Int, id:Int, severity:Int,length:Int, message:Long, userParam:Long) {

        println("errorrrr: ${source} ${type} ${severity}")
    }

    override fun loop() {

        //glDebugMessageCallback(::cb, NULL)

        logger.debug { "starting loop" }

        createCapabilities()
        // check configuration for debug setting
        //val debugProc = GLUtil.setupDebugMessageCallback();
        // create default VAO, as per the OpenGL spec a VAO should bound at all times
        val vaos = IntArray(1)
        glGenVertexArrays(vaos)
        glBindVertexArray(vaos[0])

        driver = DriverGL3()
        driver.defaultVAO = vaos[0]
        program.driver = driver
        program.drawer = Drawer(driver)

        program.drawer.width = (program.window.size.x / program.window.scale.x).toInt()
        program.drawer.height = (program.window.size.y / program.window.scale.y).toInt()

        logger.debug { "program.drawer size ${program.drawer.width} ${program.drawer.height}" }

        program.width = program.drawer.width
        program.height = program.drawer.height

        program.drawer.ortho()

        var globalModifiers = setOf<KeyboardModifier>()

        glfwSetKeyCallback(window) { _, key, scancode, action, mods ->
            val modifiers = modifierSet(mods)
            val name = glfwGetKeyName(key, scancode) ?: "<null>"

            globalModifiers = modifiers
            when (action) {
                GLFW_PRESS -> program.keyboard.keyDown.trigger(KeyEvent(KeyEventType.KEY_DOWN, key, scancode, name, modifiers))
                GLFW_RELEASE -> program.keyboard.keyUp.trigger(KeyEvent(KeyEventType.KEY_UP, key, scancode, name, modifiers))
                GLFW_REPEAT -> program.keyboard.keyRepeat.trigger(KeyEvent(KeyEventType.KEY_REPEAT, key, scancode, name, modifiers))
            }
        }

        glfwSetDropCallback(window, { _, count, names ->
            logger.debug { "$count file(s) have been dropped" }

            val pointers = PointerBuffer.create(names, count)

            val files = (0 until count).map {
                File(pointers.getStringUTF8(0))
            }

            program.window.drop.trigger(DropEvent(Vector2(0.0, 0.0), files))
        })

        var down = false

        glfwSetScrollCallback(window, { _, xoffset, yoffset ->
            program.mouse.scrolled.trigger(Program.Mouse.MouseEvent(program.mouse.position, Vector2(xoffset, yoffset), MouseEventType.SCROLLED, MouseButton.NONE, globalModifiers))
        })

        glfwSetMouseButtonCallback(window, { _, button, action, mods ->
            val mouseButton = when (button) {
                GLFW_MOUSE_BUTTON_LEFT -> MouseButton.LEFT
                GLFW_MOUSE_BUTTON_RIGHT -> MouseButton.RIGHT
                GLFW_MOUSE_BUTTON_MIDDLE -> MouseButton.CENTER
                else -> MouseButton.NONE
            }

            val modifiers = mutableSetOf<KeyboardModifier>()
            val buttonsDown= BitSet()


            if (mods and GLFW_MOD_SHIFT != 0) {
                modifiers.add(KeyboardModifier.SHIFT)
            }
            if (mods and GLFW_MOD_ALT != 0) {
                modifiers.add(KeyboardModifier.ALT)
            }
            if (mods and GLFW_MOD_CONTROL != 0) {
                modifiers.add(KeyboardModifier.CTRL)
            }
            if (mods and GLFW_MOD_SUPER != 0) {
                modifiers.add(KeyboardModifier.SUPER)
            }

            if (action == GLFW_PRESS) {
                down = true
                program.mouse.buttonDown.trigger(
                        Program.Mouse.MouseEvent(program.mouse.position , Vector2.ZERO, MouseEventType.BUTTON_DOWN, mouseButton, modifiers)
                )
                buttonsDown.set(button, true)

            }

            if (action == GLFW_RELEASE) {
                down = false
                program.mouse.buttonUp.trigger(
                        Program.Mouse.MouseEvent(program.mouse.position , Vector2.ZERO, MouseEventType.BUTTON_UP, mouseButton, modifiers)
                )
                buttonsDown.set(button, false)

                program.mouse.clicked.trigger(
                        Program.Mouse.MouseEvent(program.mouse.position, Vector2.ZERO, MouseEventType.CLICKED, mouseButton, modifiers)
                )
            }
        })

        glfwSetCursorPosCallback(window, { _, xpos, ypos ->
            val position = Vector2(xpos, ypos) / program.window.scale
            logger.debug { "mouse moved $xpos $ypos -- $position" }
            program.mouse.position = position
            program.mouse.moved.trigger(Program.Mouse.MouseEvent(position, Vector2.ZERO, MouseEventType.MOVED, MouseButton.NONE, globalModifiers))
            if (down) {
                program.mouse.dragged.trigger(Program.Mouse.MouseEvent(position, Vector2.ZERO, MouseEventType.DRAGGED, MouseButton.NONE, emptySet()))
            }
        })

        glfwSetCursorEnterCallback(window, { window, entered ->
            logger.debug { "cursor state changed; inside window = $entered" }
            if (entered) {
                glfwFocusWindow(window)
            }
        })

        val defaultRenderTarget = ProgramRenderTargetGL3(program)
        defaultRenderTarget.bind()

        if (configuration.showBeforeSetup) {
            logger.debug { "clearing and displaying pre-setup" }

            // clear the front buffer
            glDepthMask(true)
            glClearColor(0.5f, 0.5f, 0.5f, 0.0f)
            glClear(GL_COLOR_BUFFER_BIT or GL_STENCIL_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

            // swap the color buffers
            glfwSwapBuffers(window)

            // clear the back buffer
            glClearColor(0.5f, 0.5f, 0.5f, 0.0f)
            glClear(GL_COLOR_BUFFER_BIT or GL_STENCIL_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
            glDepthMask(false)

            glfwPollEvents()
        }

        logger.debug { "opengl vendor: ${glGetString(GL_VENDOR)}" }
        logger.debug { "opengl version: ${glGetString(GL_VERSION)}" }

        logger.debug { "calling program.setup" }

        val fbw = IntArray(1)
        val fbh = IntArray(1)


        glfwGetFramebufferSize(window, fbw, fbh)
        logger.info { "frame buffer size: ${fbw[0]} ${fbh[0]}" }
        program.window.size = Vector2(fbw[0] * 1.0, fbh[0] * 1.0)

        program.width = (fbw[0] / program.window.scale.x).toInt()
        program.height = (fbh[0] / program.window.scale.y).toInt()
        program.drawer.width = program.width
        program.drawer.height = program.height
        program.window.size = Vector2(fbw[0] * 1.0, fbh[0] * 1.0)

        program.drawer.reset()
        program.drawer.ortho()


        if (configuration.hideCursor) {
            glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_HIDDEN)
        }

        program.setup()

        startTimeMillis = System.currentTimeMillis()

        var exception: Throwable? = null
        glfwSwapInterval(1)

        while (!exitRequested && !glfwWindowShouldClose(window)) {

            glBindVertexArray(vaos[0])

    //            val fbw = IntArray(1)
    //            val fbh = IntArray(1)
    //
    //            glfwGetFramebufferSize(window, fbw, fbh)
//            program.window.size = Vector2(fbw[0] * 1.0, fbh[0] * 1.0)
//
//            program.width = (fbw[0] / program.window.scale.x).toInt() //(program.window.size.x/program.window.scale.x).toInt()
//            program.height = (fbh[0] / program.window.scale.y).toInt() // (program.window.size.y/program.window.scale.y).toInt()
//            program.drawer.width = program.width
//            program.drawer.height = program.height

            program.drawer.reset()
            program.drawer.ortho()

            program.window.drop.deliver()
            program.keyboard.keyDown.deliver()
            program.keyboard.keyUp.deliver()
            program.keyboard.keyRepeat.deliver()
            program.mouse.moved.deliver()
            program.mouse.scrolled.deliver()
            program.mouse.clicked.deliver()
            program.mouse.buttonDown.deliver()
            program.mouse.buttonUp.deliver()
            program.mouse.dragged.deliver()

            try {
                logger.debug { "window: ${program.window.size.x.toInt()}x${program.window.size.y.toInt()} program: ${program.width}x${program.height}" }
                //glViewport(16, 16, (program.window.size.x).toInt()-32, (program.window.size.y-86).toInt())
                glViewport(0, 0, fbw[0], fbh[0])
                program.drawImpl()
            } catch (e: Throwable) {
                logger.error { "caught exception, breaking animation loop" }
                exception = e
                break
            }
            glfwSwapBuffers(window) // swap the color buffers
            if (!windowFocused && configuration.unfocusBehaviour == UnfocusBehaviour.THROTTLE) {
                Thread.sleep(100)
            }
            glfwPollEvents()
        }

        logger.info { "exiting loop" }

        glfwFreeCallbacks(window)
        glfwDestroyWindow(window)

        //glfwTerminate()
        //glfwSetErrorCallback(null)?.free()

        logger.info { "done" }

        exception?.let {
            throw it
        }
    }

    private fun modifierSet(mods: Int): Set<KeyboardModifier> {
        val modifiers = mutableSetOf<KeyboardModifier>()
        if (mods and GLFW_MOD_SHIFT != 0) {
            modifiers.add(KeyboardModifier.SHIFT)
        }
        if (mods and GLFW_MOD_ALT != 0) {
            modifiers.add(KeyboardModifier.ALT)
        }
        if (mods and GLFW_MOD_CONTROL != 0) {
            modifiers.add(KeyboardModifier.CTRL)
        }
        if (mods and GLFW_MOD_SUPER != 0) {
            modifiers.add(KeyboardModifier.SUPER)
        }
        return modifiers
    }

    override fun exit() {
        exitRequested = true
    }
}
