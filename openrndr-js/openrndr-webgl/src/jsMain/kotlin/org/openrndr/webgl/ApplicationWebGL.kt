package org.openrndr.webgl

import kotlinx.browser.document
import kotlinx.browser.window
import org.khronos.webgl.WebGLRenderingContext
import org.openrndr.*
import org.openrndr.draw.Drawer
import org.openrndr.internal.Driver
import org.openrndr.math.Vector2
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.events.MouseEvent as HtmlMouseEvent
import org.w3c.dom.events.UIEvent
import kotlin.math.roundToInt

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

    private var referenceTime: Double = 0.0

    var canvas: HTMLCanvasElement? = null
    var context: WebGLRenderingContext? = null
    var defaultRenderTarget: ProgramRenderTargetWebGL? = null
    override fun setup() {
        canvas = document.getElementById(configuration.canvasId) as? HTMLCanvasElement ?: error("failed to get canvas #${configuration.canvasId}")
        context = canvas?.getContext("webgl") as? WebGLRenderingContext ?: error("failed to create webgl context")
        Driver.driver = DriverWebGL(context?:error("no context"))
        program.drawer = Drawer(Driver.instance)
        referenceTime = window.performance.now()

        val dpr = window.devicePixelRatio

        canvas?.width = (dpr * (canvas?.clientWidth?:error("no width"))).toInt()
        canvas?.height = (dpr * (canvas?.clientHeight?:error("no height"))).toInt()

        program.setup()

        window.addEventListener("resize", {
            val dpr = window.devicePixelRatio
            canvas?.width = (dpr * (canvas?.clientWidth?:error("no width"))).toInt()
            canvas?.height = (dpr * (canvas?.clientHeight?:error("no height"))).toInt()
        })


        var lastDragPosition = Vector2.ZERO
        var down = false
        window.addEventListener("mousedown", {
            it as HtmlMouseEvent
            down = true
            val x = it.clientX.toDouble()
            val y = it.clientY.toDouble()
            lastDragPosition = Vector2(x, y)
        })

        window.addEventListener("mouseup", {
            down = false
        })

        window.addEventListener("pointermove", {
            it as HtmlMouseEvent
            val x = it.clientX.toDouble()
            val y = it.clientY.toDouble()
            this.cursorPosition = Vector2(x, y)
            program.mouse.moved.trigger(MouseEvent(cursorPosition, Vector2.ZERO, Vector2.ZERO, MouseEventType.MOVED, MouseButton.NONE, emptySet()))
        })

        window.addEventListener("mousemove", {
            if (down) {
                it as HtmlMouseEvent
                val x = it.clientX.toDouble()
                val y = it.clientY.toDouble()
                this.cursorPosition = Vector2(x, y)
                program.mouse.dragged.trigger(
                    MouseEvent(
                        cursorPosition,
                        Vector2.ZERO,
                        this.cursorPosition - lastDragPosition,
                        MouseEventType.MOVED,
                        MouseButton.NONE,
                        emptySet()
                    )
                )
                lastDragPosition = this.cursorPosition
            }
        })


        defaultRenderTarget = ProgramRenderTargetWebGL(context?:error("no context"), program)
        defaultRenderTarget?.bind()
    }

    override fun loop() {
        val dims = windowSize
        program.width = dims.x.toInt()
        program.height = dims.y.toInt()
        program.drawer.width = dims.x.toInt()
        program.drawer.height = dims.y.toInt()

        program.drawer.reset()
        program.drawer.ortho()

        defaultRenderTarget?.bindTarget()
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
        get() = Vector2(0.0, 0.0)
        set(value) {}
    override var windowSize: Vector2
        get() {
            val width = canvas?.clientWidth?.toDouble()?:0.0
            val height = canvas?.clientHeight?.toDouble()?:0.0
            return Vector2(width, height)
        }
        set(value) {
            error("not supported")
        }

    override var cursorPosition: Vector2 = Vector2(0.0, 0.0)


    override var cursorVisible: Boolean
        get() = TODO("Not yet implemented")
        set(value) {}
    override var cursorHideMode: MouseCursorHideMode
        get() = TODO("Not yet implemented")
        set(value) {}
    override var cursorType: CursorType = CursorType.ARROW_CURSOR
    override val seconds: Double
    get() {
        return (window.performance.now() - referenceTime) / 1000.0
    }

    override var presentationMode: PresentationMode = PresentationMode.AUTOMATIC
    override var windowContentScale: Double
        get() = window.devicePixelRatio
        set(value) {}
}