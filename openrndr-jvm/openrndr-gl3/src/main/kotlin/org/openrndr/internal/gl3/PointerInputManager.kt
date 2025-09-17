package org.openrndr.internal.gl3

import io.github.oshai.kotlinlogging.KotlinLogging
import org.lwjgl.glfw.GLFWNativeWin32
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.windows.*
import org.openrndr.Pointer
import org.openrndr.math.Vector2

private val logger = KotlinLogging.logger { }

internal abstract class PointerInputManager {
    abstract fun pollEvents()
}

@JvmInline
private value class PointerWParam(val value: Long) {
    val new: Boolean
        get() = PointerInputManagerWin32.HIWORD(value) and PointerInputManagerWin32.POINTER_FLAG_NEW != 0

    val primary: Boolean
        get() = PointerInputManagerWin32.HIWORD(value) and PointerInputManagerWin32.POINTER_FLAG_PRIMARY != 0

    val id: Int
        get() = PointerInputManagerWin32.LOWORD(value)
}

@JvmInline
private value class PointerLParam(val value: Long) {
    val x: Int
        get() = (value and 0xffff).toInt()

    val y: Int
        get() = (value.shr(16) and 0xffff).toInt()
}


internal class PointerInputManagerWin32(val window: Long, val application: ApplicationGLFWGL3) :
    PointerInputManager() {
    val hwnd = GLFWNativeWin32.glfwGetWin32Window(window)
    var msg: MSG = MSG.calloc()

    var touchHwnd = 0L

    fun createOverlayWindow() {
        val windowClass = WNDCLASSEX.calloc()

        windowClass.cbSize(WNDCLASSEX.SIZEOF)
        windowClass.lpfnWndProc { hwnd, uMsg, wParam, lParam ->
            when (uMsg) {
                in User32.WM_MOUSEMOVE..User32.WM_MOUSEWHEEL,
                in User32.WM_KEYDOWN..User32.WM_UNICHAR -> {
                    User32.SendMessage(null, this.hwnd, uMsg, wParam, lParam)
                    0
                }

                else -> {
                    User32.DefWindowProc(hwnd, uMsg, wParam, lParam)
                }
            }
        }
        windowClass.hbrBackground(0)
        windowClass.lpszClassName(MemoryUtil.memUTF16("touchs", true))
        windowClass.hIcon(0L)

        val res = User32.RegisterClassEx(null, windowClass)
        require(res != 0.toShort()) {
            "RegisterClassEx failed"
        }

        logger.info { "parent window $hwnd" }
        touchHwnd = User32.CreateWindowEx(
            null,
            /* dwExStyle = */ 0,
            /* lpClassName = */ "touchs",
            /* lpWindowName = */ "touch",
            /* dwStyle = */ User32.WS_CHILD,
            /* x = */ 0,
            /* y = */ 0,
            /* nWidth = */ 0,
            /* nHeight = */ 0,
            /* hWndParent = */ hwnd,
            /* hMenu = */ 0,
            /* hInstance = */0L,
            /* lpParam = */ 0L
        )
        require(touchHwnd != 0L) { "CreateWindowEx failed" }

        val windowStyle = User32.GetWindowLongPtr(null, hwnd, User32.GWL_STYLE)
        User32.SetWindowLongPtr(null, hwnd, User32.GWL_STYLE, windowStyle xor User32.WS_CLIPCHILDREN.toLong())

        logger.info { "created touch window window $touchHwnd" }
        User32.RegisterTouchWindow(null, touchHwnd, 0)
        User32.ShowWindow(touchHwnd, User32.SW_SHOW)

        require(User32.MoveWindow(null, touchHwnd, 0, 0, 8192, 8192, false))
    }

    companion object {
        @Suppress("FunctionName")
        fun HIWORD(x: Long): Int {
            return ((x shr 16) and 0xffff).toInt()
        }

        @Suppress("FunctionName")
        fun LOWORD(x: Long): Int {
            return (x and 0xffff).toInt()
        }

        // copied from winuser.h
        const val POINTER_FLAG_NONE = 0x00000000 // Default
        const val POINTER_FLAG_NEW = 0x00000001 // New pointer
        const val POINTER_FLAG_INRANGE = 0x00000002 // Pointer has not departed
        const val POINTER_FLAG_INCONTACT = 0x00000004 // Pointer is in contact
        const val POINTER_FLAG_FIRSTBUTTON = 0x00000010 // Primary action
        const val POINTER_FLAG_SECONDBUTTON = 0x00000020 // Secondary action
        const val POINTER_FLAG_THIRDBUTTON = 0x00000040 // Third button
        const val POINTER_FLAG_FOURTHBUTTON = 0x00000080 // Fourth button
        const val POINTER_FLAG_FIFTHBUTTON = 0x00000100 // Fifth button
        const val POINTER_FLAG_PRIMARY = 0x00002000 // Pointer is primary
        const val POINTER_FLAG_CONFIDENCE = 0x00004000 // Pointer is considered unlikely to be accidental
        const val POINTER_FLAG_CANCELED = 0x00008000 // Pointer is departing in an abnormal manner
        const val POINTER_FLAG_DOWN = 0x00010000 // Pointer transitioned to down state (made contact)
        const val POINTER_FLAG_UPDATE = 0x00020000 // Pointer update
        const val POINTER_FLAG_UP = 0x00040000 // Pointer transitioned from down state (broke contact)
        const val POINTER_FLAG_WHEEL = 0x00080000 // Vertical wheel
        const val POINTER_FLAG_HWHEEL = 0x00100000 // Horizontal wheel
        const val POINTER_FLAG_CAPTURECHANGED = 0x00200000 // Lost capture
        const val POINTER_FLAG_HASTRANSFORM = 0x00400000 // Input has a transform associated with it

        const val WM_POINTERUPDATE = 0x0245
        const val WM_POINTERDOWN = 0x0246
        const val WM_POINTERUP = 0x0247
        const val WM_POINTERENTER = 0x0249
        const val WM_POINTERLEAVE = 0x024A
        const val WM_POINTERACTIVATE = 0x024B
        const val WM_POINTERCAPTURECHANGED = 0x024C
        const val WM_TOUCHHITTESTING = 0x024D
        const val WM_POINTERWHEEL = 0x024E
        const val WM_POINTERHWHEEL = 0x024F

        const val NID_MULTI_INPUT = 0x40
        const val NID_READY = 0x80
    }

    private val nidReady: Boolean

    init {
        val value = User32.GetSystemMetrics(User32.SM_DIGITIZER)
        nidReady = value and NID_READY != 0
        createOverlayWindow()
    }

    val pointers = mutableMapOf<Int, Pointer>()
    override fun pollEvents() {
        if (!nidReady) {
            return
        }

        var changed = false
        val contentScale = application.windowContentScale
        val windowPosition = application.windowPosition
        val ts = System.currentTimeMillis()

        while (User32.PeekMessage(msg, touchHwnd, WM_POINTERUPDATE, WM_POINTERHWHEEL, User32.PM_REMOVE)) {
            val w = PointerWParam(msg.wParam())
            val l = PointerLParam(msg.lParam())

            val position = Vector2(
                l.x.toDouble() / contentScale - windowPosition.x,
                l.y.toDouble() / contentScale - windowPosition.y
            )

            when (msg.message()) {
                WM_POINTERDOWN -> {
                }

                WM_POINTERENTER -> {
                    pointers[w.id] = Pointer(position, w.primary, ts)
                    changed = true
                }

                WM_POINTERUP -> {
                }

                WM_POINTERUPDATE -> {
                    pointers[w.id] = Pointer(position, w.primary, ts)
                    changed = true
                }

                WM_POINTERLEAVE -> {
                    pointers.remove(w.id)
                    changed = true
                    User32.GetMessageExtraInfo()
                }

                else -> {

                }
            }
            User32.TranslateMessage(msg)
            User32.DispatchMessage(msg)
        }

        for (pointer in pointers) {
            if (ts - pointer.value.timestamp > 500) {
                pointers.remove(pointer.key)
                changed = true
                break
            }
        }

        if (changed) {
            (application.pointers as MutableList<Pointer>).apply {
                clear()
                addAll(pointers.values)
            }
        }
    }
}