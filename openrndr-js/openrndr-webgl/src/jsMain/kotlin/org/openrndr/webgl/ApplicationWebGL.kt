package org.openrndr.webgl

import kotlinx.browser.document
import kotlinx.browser.window
import org.khronos.webgl.WebGLRenderingContext
import org.openrndr.*
import org.openrndr.draw.Drawer
import org.openrndr.internal.Driver
import org.openrndr.math.Vector2
import org.w3c.dom.HTMLCanvasElement

val applicationWebGLInitializer = object {
    init {
        console.log("setting up ApplicationWebGL")
        applicationFunc = { program, configuration ->
            ApplicationWebGL(program, configuration)
        }
    }
}

class ApplicationWebGL(private val program: Program, private val configuration: Configuration) : Application(){
    init {
        program.application = this
    }

    override fun requestDraw() {
        TODO("Not yet implemented")
    }

    override fun requestFocus() {
        TODO("Not yet implemented")
    }

    override fun exit() {
        TODO("Not yet implemented")
    }

    override fun setup() {
        val canvas = document.getElementById(configuration.canvasId) as? HTMLCanvasElement ?: error("failed to get canvas #${configuration.canvasId}")
        val context = canvas.getContext("webgl") as? WebGLRenderingContext ?: error("failed to create webgl context")
        Driver.driver = DriverWebGL(context)
        program.drawer = Drawer(Driver.instance)
        program.setup()

        val defaultRenderTarget = ProgramRenderTargetWebGL(context, program)
        defaultRenderTarget.bind()
    }

    override fun loop() {
        program.drawImpl()
        window.requestAnimationFrame {
            loop()
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
    override var cursorType: CursorType = CursorType.ARROW_CURSOR
    override val seconds: Double = 0.0

    override var presentationMode: PresentationMode = PresentationMode.AUTOMATIC
}