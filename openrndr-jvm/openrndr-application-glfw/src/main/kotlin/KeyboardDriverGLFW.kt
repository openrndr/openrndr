package org.openrndr.internal.gl3

import org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE
import org.lwjgl.glfw.GLFW.GLFW_KEY_CAPS_LOCK
import org.lwjgl.glfw.GLFW.GLFW_KEY_DELETE
import org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN
import org.lwjgl.glfw.GLFW.GLFW_KEY_END
import org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER
import org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE
import org.lwjgl.glfw.GLFW.GLFW_KEY_F1
import org.lwjgl.glfw.GLFW.GLFW_KEY_F10
import org.lwjgl.glfw.GLFW.GLFW_KEY_F11
import org.lwjgl.glfw.GLFW.GLFW_KEY_F12
import org.lwjgl.glfw.GLFW.GLFW_KEY_F2
import org.lwjgl.glfw.GLFW.GLFW_KEY_F3
import org.lwjgl.glfw.GLFW.GLFW_KEY_F4
import org.lwjgl.glfw.GLFW.GLFW_KEY_F5
import org.lwjgl.glfw.GLFW.GLFW_KEY_F6
import org.lwjgl.glfw.GLFW.GLFW_KEY_F7
import org.lwjgl.glfw.GLFW.GLFW_KEY_F8
import org.lwjgl.glfw.GLFW.GLFW_KEY_F9
import org.lwjgl.glfw.GLFW.GLFW_KEY_HOME
import org.lwjgl.glfw.GLFW.GLFW_KEY_INSERT
import org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT
import org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_ALT
import org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL
import org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT
import org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SUPER
import org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_DOWN
import org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_UP
import org.lwjgl.glfw.GLFW.GLFW_KEY_PRINT_SCREEN
import org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT
import org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_ALT
import org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_CONTROL
import org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT
import org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SUPER
import org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE
import org.lwjgl.glfw.GLFW.GLFW_KEY_TAB
import org.lwjgl.glfw.GLFW.GLFW_KEY_UP
import org.lwjgl.glfw.GLFW.glfwGetKeyName
import org.lwjgl.glfw.GLFW.glfwGetKeyScancode
import org.openrndr.KEYNAME_ARROW_DOWN
import org.openrndr.KEYNAME_ARROW_LEFT
import org.openrndr.KEYNAME_ARROW_RIGHT
import org.openrndr.KEYNAME_ARROW_UP
import org.openrndr.KEYNAME_BACKSPACE
import org.openrndr.KEYNAME_CAPSLOCK
import org.openrndr.KEYNAME_DELETE
import org.openrndr.KEYNAME_END
import org.openrndr.KEYNAME_ENTER
import org.openrndr.KEYNAME_ESCAPE
import org.openrndr.KEYNAME_F1
import org.openrndr.KEYNAME_F10
import org.openrndr.KEYNAME_F11
import org.openrndr.KEYNAME_F12
import org.openrndr.KEYNAME_F2
import org.openrndr.KEYNAME_F3
import org.openrndr.KEYNAME_F4
import org.openrndr.KEYNAME_F5
import org.openrndr.KEYNAME_F6
import org.openrndr.KEYNAME_F7
import org.openrndr.KEYNAME_F8
import org.openrndr.KEYNAME_F9
import org.openrndr.KEYNAME_HOME
import org.openrndr.KEYNAME_INSERT
import org.openrndr.KEYNAME_LEFT_ALT
import org.openrndr.KEYNAME_LEFT_CONTROL
import org.openrndr.KEYNAME_LEFT_SHIFT
import org.openrndr.KEYNAME_LEFT_SUPER
import org.openrndr.KEYNAME_PAGE_DOWN
import org.openrndr.KEYNAME_PAGE_UP
import org.openrndr.KEYNAME_PRINT_SCREEN
import org.openrndr.KEYNAME_RIGHT_ALT
import org.openrndr.KEYNAME_RIGHT_CONTROL
import org.openrndr.KEYNAME_RIGHT_SHIFT
import org.openrndr.KEYNAME_RIGHT_SUPER
import org.openrndr.KEYNAME_SPACEBAR
import org.openrndr.KEYNAME_TAB
import org.openrndr.internal.KeyboardDriver

class KeyboardDriverGLFW : KeyboardDriver {

    override fun getKeyId(key: String): Int {
        return when (key) {
            KEYNAME_SPACEBAR -> GLFW_KEY_SPACE
            KEYNAME_ESCAPE -> GLFW_KEY_ESCAPE
            KEYNAME_ENTER -> GLFW_KEY_ENTER
            KEYNAME_TAB -> GLFW_KEY_TAB
            KEYNAME_BACKSPACE -> GLFW_KEY_BACKSPACE
            KEYNAME_INSERT -> GLFW_KEY_INSERT
            KEYNAME_DELETE -> GLFW_KEY_DELETE
            KEYNAME_ARROW_RIGHT -> GLFW_KEY_RIGHT
            KEYNAME_ARROW_LEFT -> GLFW_KEY_RIGHT
            KEYNAME_ARROW_DOWN -> GLFW_KEY_DOWN
            KEYNAME_ARROW_UP -> GLFW_KEY_UP
            KEYNAME_PAGE_UP -> GLFW_KEY_PAGE_UP
            KEYNAME_PAGE_DOWN -> GLFW_KEY_PAGE_DOWN
            KEYNAME_HOME -> GLFW_KEY_HOME
            KEYNAME_END -> GLFW_KEY_END
            KEYNAME_LEFT_SHIFT -> GLFW_KEY_LEFT_SHIFT
            KEYNAME_RIGHT_SHIFT -> GLFW_KEY_RIGHT_SHIFT
            KEYNAME_LEFT_ALT -> GLFW_KEY_LEFT_ALT
            KEYNAME_RIGHT_ALT -> GLFW_KEY_RIGHT_ALT
            KEYNAME_LEFT_CONTROL -> GLFW_KEY_LEFT_CONTROL
            KEYNAME_RIGHT_CONTROL -> GLFW_KEY_RIGHT_CONTROL
            KEYNAME_LEFT_SUPER -> GLFW_KEY_LEFT_SUPER
            KEYNAME_RIGHT_SUPER -> GLFW_KEY_RIGHT_SUPER
            KEYNAME_CAPSLOCK -> GLFW_KEY_CAPS_LOCK
            KEYNAME_PRINT_SCREEN -> GLFW_KEY_PRINT_SCREEN
            KEYNAME_F1 -> GLFW_KEY_F1
            KEYNAME_F2 -> GLFW_KEY_F2
            KEYNAME_F3 -> GLFW_KEY_F3
            KEYNAME_F4 -> GLFW_KEY_F4
            KEYNAME_F5 -> GLFW_KEY_F5
            KEYNAME_F6 -> GLFW_KEY_F6
            KEYNAME_F7 -> GLFW_KEY_F7
            KEYNAME_F8 -> GLFW_KEY_F8
            KEYNAME_F9 -> GLFW_KEY_F9
            KEYNAME_F10 -> GLFW_KEY_F10
            KEYNAME_F11 -> GLFW_KEY_F11
            KEYNAME_F12 -> GLFW_KEY_F12
            else -> error("unknown key: $key")
        }
    }

    override fun getKeyName(keyId: Int): String {
        return when (keyId) {
            GLFW_KEY_SPACE -> KEYNAME_SPACEBAR
            GLFW_KEY_ENTER -> KEYNAME_ENTER
            GLFW_KEY_TAB -> KEYNAME_TAB
            GLFW_KEY_ESCAPE -> KEYNAME_ESCAPE
            GLFW_KEY_UP -> KEYNAME_ARROW_UP
            GLFW_KEY_DOWN -> KEYNAME_ARROW_DOWN
            GLFW_KEY_LEFT -> KEYNAME_ARROW_LEFT
            GLFW_KEY_RIGHT -> KEYNAME_ARROW_RIGHT
            GLFW_KEY_PRINT_SCREEN -> KEYNAME_PRINT_SCREEN
            GLFW_KEY_PAGE_DOWN -> KEYNAME_PAGE_DOWN
            GLFW_KEY_PAGE_UP -> KEYNAME_PAGE_UP
            GLFW_KEY_HOME -> KEYNAME_HOME
            GLFW_KEY_END -> KEYNAME_END
            GLFW_KEY_BACKSPACE -> KEYNAME_BACKSPACE
            GLFW_KEY_LEFT_ALT -> KEYNAME_LEFT_ALT
            GLFW_KEY_RIGHT_ALT -> KEYNAME_RIGHT_ALT
            GLFW_KEY_LEFT_CONTROL -> KEYNAME_LEFT_CONTROL
            GLFW_KEY_RIGHT_CONTROL -> KEYNAME_RIGHT_CONTROL
            GLFW_KEY_INSERT -> KEYNAME_INSERT
            GLFW_KEY_DELETE -> KEYNAME_DELETE
            GLFW_KEY_LEFT_SHIFT -> KEYNAME_LEFT_SHIFT
            GLFW_KEY_RIGHT_SHIFT -> KEYNAME_RIGHT_SHIFT
            GLFW_KEY_LEFT_SUPER -> KEYNAME_LEFT_SUPER
            GLFW_KEY_RIGHT_SUPER -> KEYNAME_RIGHT_SUPER
            GLFW_KEY_F1 -> KEYNAME_F1
            GLFW_KEY_F2 -> KEYNAME_F2
            GLFW_KEY_F3 -> KEYNAME_F3
            GLFW_KEY_F4 -> KEYNAME_F4
            GLFW_KEY_F5 -> KEYNAME_F5
            GLFW_KEY_F6 -> KEYNAME_F6
            GLFW_KEY_F7 -> KEYNAME_F7
            GLFW_KEY_F8 -> KEYNAME_F8
            GLFW_KEY_F9 -> KEYNAME_F9
            GLFW_KEY_F10 -> KEYNAME_F10
            GLFW_KEY_F11 -> KEYNAME_F11
            GLFW_KEY_F12 -> KEYNAME_F12
            GLFW_KEY_CAPS_LOCK -> KEYNAME_F12
            else -> {
                val scancode = glfwGetKeyScancode(keyId)
                glfwGetKeyName(keyId, scancode) ?: "<null>"
            }
        }
    }


}