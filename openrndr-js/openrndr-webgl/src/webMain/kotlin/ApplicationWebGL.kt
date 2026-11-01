package org.openrndr.webgl

import io.github.oshai.kotlinlogging.KotlinLogging
import js.numbers.JsNumbers.toKotlinDouble
import org.openrndr.*
import org.openrndr.draw.Drawer
import org.openrndr.draw.font.internal.CachingFontDriver
import org.openrndr.draw.font.internal.FontDriver
import org.openrndr.draw.font.internal.TextShapingDriver
import org.openrndr.draw.defaultFontUrl
import org.openrndr.fontdriver.harfbuzzjs.DEFAULT_FONT_URL
import org.openrndr.fontdriver.harfbuzzjs.FontDriverHarfBuzzJs
import org.openrndr.textshapingdriver.harfbuzzjs.TextShapingDriverHarfBuzzJs
import org.openrndr.imagedriver.browser.ImageDriverBrowser
import org.openrndr.internal.Driver
import org.openrndr.internal.ImageDriver
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
import kotlin.js.Promise
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import web.keyboard.KeyboardEvent as HtmlKeyboardEvent
import web.mouse.MouseButton as HtmlMouseButton
import web.mouse.MouseEvent as HtmlMouseEvent

private val logger = KotlinLogging.logger {  }

/**
 * Calls [callback] with true when [canvas] comes within [margin] of the view, and with false when it leaves it
 */
@OptIn(ExperimentalWasmJsInterop::class)
private fun observeInView(canvas: HTMLCanvasElement, margin: String, callback: (Boolean) -> Unit): Unit = js(
    """new IntersectionObserver((entries) => {
        callback(entries[entries.length - 1].isIntersecting);
    }, { rootMargin: margin }).observe(canvas)"""
)

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

    /** is the canvas in view, or close to it? */
    private var canvasInView: Boolean = true
    private var referenceTime: Double = 0.0


    override var windowHitTest: ((Vector2) -> Hit)?
        get() =  null
        set(value) {
            logger.warn { "window hit test is not supported" }
        }

    var canvas: HTMLCanvasElement? = null
    var context: WebGL2RenderingContext? = null
    var defaultRenderTarget: ProgramRenderTargetWebGL? = null
    private var srgbFallback: SrgbCanvasFallbackWebGL? = null
    @OptIn(ExperimentalWasmJsInterop::class)
    override suspend fun setup() {
        logger.debug { "in setup()" }
        canvas = document.getElementById(ElementId( configuration.canvasId)) as? HTMLCanvasElement
            ?: error("failed to get canvas #${configuration.canvasId}")

        logger.debug { "creating context" }
        // the window holds sRGB colors like on the JVM: the drawing buffer is made sRGB, or the window is drawn into an
        // offscreen sRGB target that has its own depth, stencil and multisampling
        val srgbDrawingBuffer = canvasSupportsSrgbDrawingBuffer()
        val attrs = JsObject.create(null).unsafeCast<WebGLContextAttributes>()
        attrs.antialias = srgbDrawingBuffer
        attrs.depth = srgbDrawingBuffer
        attrs.stencil = srgbDrawingBuffer
        attrs.preserveDrawingBuffer = true

        context = canvas?.getContext(WebGL2RenderingContext.ID, attrs)
            ?: error("failed to create webgl2 context")

        if (Driver.driver == null) {
            Driver.driver = DriverWebGL(context as WebGL2RenderingContext)
        } else {
            (Driver.instance as DriverWebGL).context = context as WebGL2RenderingContext
        }
        context?.checkErrors("ApplicationWebGL.setup(), pre-existing errors")

        program.drawer = Drawer(Driver.instance)
        referenceTime = performance.now().toKotlinDouble()

        val dpr = min(configuration.maxContentScale, devicePixelRatio)

        canvas?.width = (dpr * (canvas?.clientWidth ?: error("no width"))).toInt()
        canvas?.height = (dpr * (canvas?.clientHeight ?: error("no height"))).toInt()

        // the drawing buffer can be smaller than the canvas, browsers limit its size
        val gl = context ?: error("no context")
        srgbFallback = if (srgbDrawingBuffer && useSrgbDrawingBuffer(gl, gl.drawingBufferWidth, gl.drawingBufferHeight)) {
            null
        } else {
            logger.info { "the canvas can not have an sRGB drawing buffer, drawing into an offscreen sRGB target" }
            SrgbCanvasFallbackWebGL(gl).apply { resize(gl.drawingBufferWidth, gl.drawingBufferHeight) }
        }

        windowTitle = configuration.title

        val canvasParent = canvas?.parentElement

        val resizeNow: () -> Unit = resizeNow@{
            if (canvasParent == null || canvas == null) return@resizeNow

            val resizeDpr = min(configuration.maxContentScale, devicePixelRatio)

            val width = canvas?.clientWidth!!
            val height = canvas?.clientHeight!!
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

        // the program is not drawn while its canvas is out of view, drawing starts a little before it enters
        if (!configuration.drawOutOfView) {
            observeInView(canvas ?: error("no canvas"), "200px") { inView ->
                if (inView != canvasInView) {
                    canvasInView = inView
                    val event = WindowEvent(
                        if (inView) WindowEventType.RESTORED else WindowEventType.MINIMIZED,
                        windowPosition, windowSize, false
                    )
                    if (inView) program.window.restored.trigger(event) else program.window.minimized.trigger(event)
                }
            }
        }

        // Keyboard
        canvas?.addEventListener(EventType("keydown"), {
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

        canvas?.addEventListener(EventType("keyup"), {
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
        canvas?.addEventListener(EventType("mousedown"), {
            it as HtmlMouseEvent
            down = true
            val x = it.offsetX
            val y = it.offsetY
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

        canvas?.addEventListener(EventType("contextmenu"), {
            it.preventDefault()
        })

        canvas?.addEventListener(EventType("mouseup"), {
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

        canvas?.addEventListener(EventType("wheel"), {
            it as WheelEvent
            // a wheel event with the control key held is a trackpad pinch, see listenToTouchInput
            if (it.ctrlKey) return@addEventListener
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

        canvas?.addEventListener(EventType("pointermove"), {
            it as HtmlMouseEvent
            val x = it.offsetX
            val y = it.offsetY
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

        canvas?.addEventListener(EventType("mousemove"), {
            if (down) {
                it as HtmlMouseEvent
                val x = it.offsetX
                val y = it.offsetY
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


        // touches, pens and pinches
        canvas?.let { listenToTouchInput(it, program) }

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


        defaultRenderTarget = ProgramRenderTargetWebGL(context ?: error("no context"), program, srgbFallback)
        defaultRenderTarget?.bind()


        val dims = windowSize
        program.width = dims.x.toInt()
        program.height = dims.y.toInt()

        // fonts and text shaping are backed by harfbuzzjs, which loads its WebAssembly module asynchronously. The
        // drivers are set up after the graphics, so the application is complete when this suspends.
        if (FontDriver.driver == null) {
            FontDriver.driver = CachingFontDriver(FontDriverHarfBuzzJs.create())
        }
        if (TextShapingDriver.driver == null) {
            TextShapingDriver.driver = TextShapingDriverHarfBuzzJs()
        }
        // images are decoded by the browser
        if (ImageDriver.driver == null) {
            ImageDriver.driver = ImageDriverBrowser()
        }
        // the default font is embedded, IBM Plex Mono like the default font on the JVM
        if (defaultFontUrl == null) {
            defaultFontUrl = DEFAULT_FONT_URL
        }

        // the setups of the applications of a page run interleaved while the drivers above load, the program
        // creates its resources in the context of its own canvas
        (Driver.instance as DriverWebGL).context = context as WebGL2RenderingContext
        program.setup()
        // the program listens to touches and pinches once it is set up
        canvas?.let { updateTouchAction(it, program) }
    }

    override fun windowClose() {

    }

    override fun windowMaximize() {

    }

    override fun windowFullscreen(mode: Fullscreen) {

    }

    override fun windowMinimize() {

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
        val storedContext = (Driver.instance as DriverWebGL).context
        (Driver.instance as DriverWebGL).context = context as WebGL2RenderingContext
        // events and launched coroutines are processed every frame, also when the program is not drawn
        program.dispatcher.execute()

        // a requested frame is drawn once the canvas is in view
        val inView = canvasInView || configuration.drawOutOfView
        if (inView && (presentationMode == PresentationMode.AUTOMATIC || drawRequested)) {
            drawRequested = false

            val dims = windowSize
            program.width = dims.x.toInt()
            program.height = dims.y.toInt()

            @Suppress("DEPRECATION")
            program.drawer.reset()
            program.drawer.ortho()

            context?.let { srgbFallback?.resize(it.drawingBufferWidth, it.drawingBufferHeight) }
            defaultRenderTarget?.bindTarget()
            program.drawImpl()
            srgbFallback?.present()
        }
        (Driver.instance as DriverWebGL).context = storedContext

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
            return (performance.now().toKotlinDouble() - referenceTime) / 1000.0
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