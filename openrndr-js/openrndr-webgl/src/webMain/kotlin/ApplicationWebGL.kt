package org.openrndr.webgl

import io.github.oshai.kotlinlogging.KotlinLogging
import js.core.JsPrimitives.toDouble
import org.openrndr.*
import org.openrndr.draw.Drawer
import org.openrndr.internal.Driver
import org.openrndr.math.Vector2
import web.animations.requestAnimationFrame
import web.device.devicePixelRatio
import web.dom.ElementId
import web.dom.document
import web.events.EventHandler
import web.events.EventType
import web.events.addEventListener
import web.file.File
import web.file.FileReader
import web.gl.ID
import web.gl.WebGL2RenderingContext
import web.gl.WebGLContextAttributes
import web.html.HTMLCanvasElement
import web.mouse.AUXILIARY
import web.mouse.MAIN
import web.mouse.SECONDARY
import web.mouse.WheelEvent
import web.performance.performance
import web.resize.ResizeObserver
import web.window.window
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.JsName
import kotlin.js.JsString
import kotlin.js.Promise
import kotlin.js.unsafeCast
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import web.keyboard.KeyboardEvent as HtmlKeyboardEvent
import web.mouse.MouseButton as HtmlMouseButton
import web.mouse.MouseEvent as HtmlMouseEvent

private val logger = KotlinLogging.logger {  }

@OptIn(ExperimentalWasmJsInterop::class)
@JsName("Object")
external object JsObject {
    fun create(proto: JsAny?): JsAny
}

class ApplicationWebGL(override var program: Program, override var configuration: Configuration) : Application() {
    init {
        program.application = this
    }

    override fun requestDraw() {
        drawRequested = true
    }

    override fun requestFocus() {
        TODO("Not yet implemented")
    }

    override fun exit() {
        TODO("Not yet implemented")
    }

    private var drawRequested: Boolean = true
    private var referenceTime: Double = 0.0

    var canvas: HTMLCanvasElement? = null
    var context: WebGL2RenderingContext? = null
    var defaultRenderTarget: ProgramRenderTargetWebGL? = null
    @OptIn(ExperimentalWasmJsInterop::class)
    override suspend fun setup() {
        logger.info { "in setup()" }
        canvas = document.getElementById(ElementId( configuration.canvasId)) as? HTMLCanvasElement
            ?: error("failed to get canvas #${configuration.canvasId}")

        logger.info { "creating context" }
        val attrs = JsObject.create(null).unsafeCast<WebGLContextAttributes>()
        attrs.stencil = true
        attrs.preserveDrawingBuffer = true

        context = canvas?.getContext(WebGL2RenderingContext.ID, attrs)
            ?: error("failed to create webgl2 context")
        Driver.driver = DriverWebGL(context as WebGL2RenderingContext)
        program.drawer = Drawer(Driver.instance)
        referenceTime = performance.now().toDouble()

        val dpr = min(configuration.maxContentScale, devicePixelRatio)

        canvas?.width = (dpr * (canvas?.clientWidth ?: error("no width"))).toInt()
        canvas?.height = (dpr * (canvas?.clientHeight ?: error("no height"))).toInt()

        windowTitle = configuration.title

        val canvasParent = canvas?.parentElement

        val resizeNow: () -> Unit = resizeNow@{
            if (canvasParent == null || canvas == null) return@resizeNow

            val resizeDpr = min(configuration.maxContentScale, devicePixelRatio)

            val width = canvasParent.clientWidth
            val height = canvasParent.clientHeight
            val w = max(0.0, floor(width * resizeDpr)).toInt()
            val h = max(0.0, floor(height * resizeDpr)).toInt()
            if (canvas != null && (canvas!!.width != w || canvas!!.height != h)) {
                canvas!!.width = w
                canvas!!.height = h
            }

            program.window.sized.trigger(
                WindowEvent(
                    WindowEventType.RESIZED,
                    Vector2(0.0, 0.0),
                    Vector2(width.toDouble(), height.toDouble()),
                    true
                )
            )
        }

        // Debounce resize events
        val resize: () -> Unit = {
            requestAnimationFrame { resizeNow() }
        }

        if (canvasParent != null) {
            val ro = ResizeObserver { _, _ ->
                resize()
             }
            ro.observe(canvasParent)
        }

        // Keyboard
        window.addEventListener(EventType("keydown"), {
            it as HtmlKeyboardEvent
            program.keyboard.keyDown.trigger(
                KeyEvent(
                    KeyEventType.KEY_DOWN,
                    0,                      // TODO: investigate alternatives
                    it.key,
                    getModifiers(it)
                )
            )
        })

        window.addEventListener(EventType("keyup"), {
            it as HtmlKeyboardEvent
            program.keyboard.keyUp.trigger(
                KeyEvent(
                    KeyEventType.KEY_UP,
                    0,                      // TODO: investigate alternatives
                    it.key,
                    getModifiers(it)
                )
            )
        })

        // Mouse
        var lastDragPosition = Vector2.ZERO
        var down = false
        window.addEventListener(EventType("mousedown"), {
            it as HtmlMouseEvent
            down = true
            val x = it.clientX.toDouble()
            val y = it.clientY.toDouble()
            lastDragPosition = Vector2(x, y)

            program.mouse.buttonDown.trigger(
                MouseEvent(
                    lastDragPosition,
                    Vector2.ZERO,
                    Vector2.ZERO,
                    MouseEventType.BUTTON_DOWN,
                    when (it.button) {
                        HtmlMouseButton.MAIN -> MouseButton.LEFT
                        HtmlMouseButton.AUXILIARY -> MouseButton.CENTER
                        HtmlMouseButton.SECONDARY -> MouseButton.RIGHT
                    },
                    emptySet()
                )
            )
        })

        window.addEventListener(EventType("mouseup"), {
            it as HtmlMouseEvent
            down = false

            program.mouse.buttonUp.trigger(
                MouseEvent(
                    lastDragPosition,
                    Vector2.ZERO,
                    Vector2.ZERO,
                    MouseEventType.BUTTON_UP,
                    when (it.button) {
                        HtmlMouseButton.MAIN -> MouseButton.LEFT
                        HtmlMouseButton.AUXILIARY -> MouseButton.CENTER
                        HtmlMouseButton.SECONDARY -> MouseButton.RIGHT
                    },
                    emptySet()
                )
            )
        })

        window.addEventListener(EventType("wheel"), {
            it as WheelEvent
            program.mouse.scrolled.trigger(
                MouseEvent(
                    cursorPosition,
                    Vector2(it.deltaX, it.deltaY),
                    Vector2.ZERO,
                    MouseEventType.SCROLLED,
                    MouseButton.NONE,
                    emptySet()
                )
            )
        })

        window.addEventListener(EventType("pointermove"), {
            it as HtmlMouseEvent
            val x = it.clientX.toDouble()
            val y = it.clientY.toDouble()
            this.cursorPosition = Vector2(x, y)
            program.mouse.moved.trigger(
                MouseEvent(
                    cursorPosition,
                    Vector2.ZERO,
                    Vector2.ZERO,
                    MouseEventType.MOVED,
                    MouseButton.NONE,
                    emptySet()
                )
            )
        })

        window.addEventListener(EventType("mousemove"), {
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


        // Drag and drop
        canvas?.addEventListener(EventType("dragover"), {
            it.preventDefault()
            println("dragover")
        })

//        canvas?.addEventListener(EventType("drop"), {
//            it.preventDefault()
//            println("drop")
//            val files: FileList = js("it.dataTransfer.files")
//            val promises = files.asList().map { f -> readFileOrBlobAsDataUrl(f)}
//
//            Promise.all(promises.toTypedArray()).then { images ->
//                program.window.drop.trigger(
//                    DropEvent(
//                        this.cursorPosition,
//                        images.toList()
//                    )
//                )
//            }
//        })


        defaultRenderTarget = ProgramRenderTargetWebGL(context ?: error("no context"), program)
        defaultRenderTarget?.bind()


        val dims = windowSize
        program.width = dims.x.toInt()
        program.height = dims.y.toInt()

        logger.info { "calling program.setup()" }
        program.setup()
        logger.info { "that's done" }
    }

    @OptIn(ExperimentalWasmJsInterop::class)
    private fun readFileOrBlobAsDataUrl(file: File): Promise<JsString> {
        return Promise { resolve, _ ->
            val reader = FileReader()
            reader.onload = EventHandler {
                val result = reader.result
                if (result != null) {
                    // readAsDataURL yields a data URL string
                    resolve(result.unsafeCast<JsString>())
                } else {
                    error(Throwable("FileReader result is null"))
                }
            }
            reader.readAsDataURL(file)
        }
    }

    private fun getModifiers(e: HtmlKeyboardEvent): Set<KeyModifier> {
        val set = mutableSetOf<KeyModifier>()
        if (e.ctrlKey) set.add(KeyModifier.CTRL)
        if (e.altKey) set.add(KeyModifier.ALT)
        if (e.metaKey) set.add(KeyModifier.SUPER)
        if (e.shiftKey) set.add(KeyModifier.SHIFT)
        return set
    }

    override fun loop() {
        //("start loop")
        if (presentationMode == PresentationMode.AUTOMATIC || drawRequested) {
            drawRequested = false

            val dims = windowSize
            program.width = dims.x.toInt()
            program.height = dims.y.toInt()

            @Suppress("DEPRECATION")
            program.drawer.reset()
            program.drawer.ortho()

            defaultRenderTarget?.bindTarget()
            program.drawImpl()
        }

        requestAnimationFrame {
            loop()
        }
    }

    override var clipboardContents: String?
        get() = TODO("Not yet implemented")
        set(_) {}

    override var windowTitle: String
        get() = document.title
        set(text) {
            document.title = text
        }

    override var windowPosition: Vector2
        get() = Vector2(0.0, 0.0)
        set(_) {}
    override var windowSize: Vector2
        get() {
            val width = canvas?.clientWidth?.toDouble() ?: 0.0
            val height = canvas?.clientHeight?.toDouble() ?: 0.0
            return Vector2(width, height)
        }
        set(_) {
            error("not supported")
        }

    override var cursorPosition: Vector2 = Vector2(0.0, 0.0)


    override var cursorVisible: Boolean
        get() = TODO("Not yet implemented")
        set(_) {}

    override var cursorHideMode: MouseCursorHideMode
        get() = TODO("Not yet implemented")
        set(_) {}
    override var cursorType: CursorType = CursorType.ARROW_CURSOR
    override val seconds: Double
        get() {
            return (performance.now().toDouble() - referenceTime) / 1000.0
        }

    override var presentationMode: PresentationMode = PresentationMode.AUTOMATIC
    override var windowContentScale: Double
        get() = min(configuration.maxContentScale, devicePixelRatio)
        set(_) {}

    override var windowMultisample: WindowMultisample
        get() = WindowMultisample.Disabled
        set(_) {
            TODO("Not yet implemented")
        }

    override var windowResizable: Boolean
        get() = false
        set(_) {
            TODO("Not yet implemented")
        }
}