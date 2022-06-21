package org.openrndr.internal.gl3

import org.lwjgl.glfw.GLFWNativeWin32
import org.lwjgl.system.windows.*
import org.openrndr.Pointer
import org.openrndr.math.Vector2

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
    var msg = MSG.calloc()

    companion object {
        fun HIWORD(x: Long): Int {
            return ((x shr 16) and 0xffff).toInt()
        }

        fun LOWORD(x: Long): Int {
            return (x and 0xffff).toInt()
        }

        // copied from winuser.h
        val POINTER_FLAG_NONE = 0x00000000 // Default
        val POINTER_FLAG_NEW = 0x00000001 // New pointer
        val POINTER_FLAG_INRANGE = 0x00000002 // Pointer has not departed
        val POINTER_FLAG_INCONTACT = 0x00000004 // Pointer is in contact
        val POINTER_FLAG_FIRSTBUTTON = 0x00000010 // Primary action
        val POINTER_FLAG_SECONDBUTTON = 0x00000020 // Secondary action
        val POINTER_FLAG_THIRDBUTTON = 0x00000040 // Third button
        val POINTER_FLAG_FOURTHBUTTON = 0x00000080 // Fourth button
        val POINTER_FLAG_FIFTHBUTTON = 0x00000100 // Fifth button
        val POINTER_FLAG_PRIMARY = 0x00002000 // Pointer is primary
        val POINTER_FLAG_CONFIDENCE = 0x00004000 // Pointer is considered unlikely to be accidental
        val POINTER_FLAG_CANCELED = 0x00008000 // Pointer is departing in an abnormal manner
        val POINTER_FLAG_DOWN = 0x00010000 // Pointer transitioned to down state (made contact)
        val POINTER_FLAG_UPDATE = 0x00020000 // Pointer update
        val POINTER_FLAG_UP = 0x00040000 // Pointer transitioned from down state (broke contact)
        val POINTER_FLAG_WHEEL = 0x00080000 // Vertical wheel
        val POINTER_FLAG_HWHEEL = 0x00100000 // Horizontal wheel
        val POINTER_FLAG_CAPTURECHANGED = 0x00200000 // Lost capture
        val POINTER_FLAG_HASTRANSFORM = 0x00400000 // Input has a transform associated with it

        val WM_POINTERUPDATE = 0x0245
        val WM_POINTERDOWN = 0x0246
        val WM_POINTERUP = 0x0247
        val WM_POINTERENTER = 0x0249
        val WM_POINTERLEAVE = 0x024A
        val WM_POINTERACTIVATE = 0x024B
        val WM_POINTERCAPTURECHANGED = 0x024C
        val WM_TOUCHHITTESTING = 0x024D
        val WM_POINTERWHEEL = 0x024E
        val WM_POINTERHWHEEL = 0x024F

        val NID_MULTI_INPUT = 0x40
        val NID_READY = 0x80
    }

    private val nidReady: Boolean

    init {
        val value = User32.GetSystemMetrics(User32.SM_DIGITIZER)
        nidReady = value and NID_READY != 0
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

        while (User32.PeekMessage(msg, hwnd, WM_POINTERUPDATE, WM_POINTERHWHEEL, User32.PM_REMOVE)) {
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
            User32.DispatchMessage(msg)
            //User32.DefWindowProc(hwnd, msg.message(), msg.lParam(), msg.wParam())
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