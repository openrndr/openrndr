package org.openrndr.application.sdl

import org.lwjgl.sdl.SDLKeyboard.SDL_GetKeyName
import org.lwjgl.sdl.SDLKeycode.SDLK_BACKSPACE
import org.lwjgl.sdl.SDLKeycode.SDLK_CAPSLOCK
import org.lwjgl.sdl.SDLKeycode.SDLK_DELETE
import org.lwjgl.sdl.SDLKeycode.SDLK_DOWN
import org.lwjgl.sdl.SDLKeycode.SDLK_END
import org.lwjgl.sdl.SDLKeycode.SDLK_ESCAPE
import org.lwjgl.sdl.SDLKeycode.SDLK_F1
import org.lwjgl.sdl.SDLKeycode.SDLK_F10
import org.lwjgl.sdl.SDLKeycode.SDLK_F11
import org.lwjgl.sdl.SDLKeycode.SDLK_F12
import org.lwjgl.sdl.SDLKeycode.SDLK_F2
import org.lwjgl.sdl.SDLKeycode.SDLK_F3
import org.lwjgl.sdl.SDLKeycode.SDLK_F4
import org.lwjgl.sdl.SDLKeycode.SDLK_F5
import org.lwjgl.sdl.SDLKeycode.SDLK_F6
import org.lwjgl.sdl.SDLKeycode.SDLK_F7
import org.lwjgl.sdl.SDLKeycode.SDLK_F8
import org.lwjgl.sdl.SDLKeycode.SDLK_F9
import org.lwjgl.sdl.SDLKeycode.SDLK_HOME
import org.lwjgl.sdl.SDLKeycode.SDLK_INSERT
import org.lwjgl.sdl.SDLKeycode.SDLK_LALT
import org.lwjgl.sdl.SDLKeycode.SDLK_LCTRL
import org.lwjgl.sdl.SDLKeycode.SDLK_LEFT
import org.lwjgl.sdl.SDLKeycode.SDLK_LGUI
import org.lwjgl.sdl.SDLKeycode.SDLK_LSHIFT
import org.lwjgl.sdl.SDLKeycode.SDLK_PAGEDOWN
import org.lwjgl.sdl.SDLKeycode.SDLK_PAGEUP
import org.lwjgl.sdl.SDLKeycode.SDLK_PRINTSCREEN
import org.lwjgl.sdl.SDLKeycode.SDLK_RALT
import org.lwjgl.sdl.SDLKeycode.SDLK_RCTRL
import org.lwjgl.sdl.SDLKeycode.SDLK_RETURN
import org.lwjgl.sdl.SDLKeycode.SDLK_RGUI
import org.lwjgl.sdl.SDLKeycode.SDLK_RIGHT
import org.lwjgl.sdl.SDLKeycode.SDLK_RSHIFT
import org.lwjgl.sdl.SDLKeycode.SDLK_SPACE
import org.lwjgl.sdl.SDLKeycode.SDLK_TAB
import org.lwjgl.sdl.SDLKeycode.SDLK_UP
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

class KeyboardDriverSDL : KeyboardDriver {
    override fun getKeyId(key: String): Int {
        return when (key) {
            KEYNAME_SPACEBAR -> SDLK_SPACE
            KEYNAME_ESCAPE -> SDLK_ESCAPE
            KEYNAME_ENTER -> SDLK_RETURN
            KEYNAME_TAB -> SDLK_TAB
            KEYNAME_BACKSPACE -> SDLK_BACKSPACE
            KEYNAME_INSERT -> SDLK_INSERT
            KEYNAME_DELETE -> SDLK_DELETE
            KEYNAME_ARROW_RIGHT -> SDLK_RIGHT
            KEYNAME_ARROW_LEFT -> SDLK_LEFT
            KEYNAME_ARROW_DOWN -> SDLK_DOWN
            KEYNAME_ARROW_UP -> SDLK_UP
            KEYNAME_PAGE_UP -> SDLK_PAGEUP
            KEYNAME_PAGE_DOWN -> SDLK_PAGEDOWN
            KEYNAME_HOME -> SDLK_HOME
            KEYNAME_END -> SDLK_END
            KEYNAME_LEFT_SHIFT -> SDLK_LSHIFT
            KEYNAME_RIGHT_SHIFT -> SDLK_RSHIFT
            KEYNAME_LEFT_ALT -> SDLK_LALT
            KEYNAME_RIGHT_ALT -> SDLK_RALT
            KEYNAME_LEFT_CONTROL -> SDLK_LCTRL
            KEYNAME_RIGHT_CONTROL -> SDLK_RCTRL
            KEYNAME_LEFT_SUPER -> SDLK_LGUI
            KEYNAME_RIGHT_SUPER -> SDLK_RGUI
            KEYNAME_CAPSLOCK -> SDLK_CAPSLOCK
            KEYNAME_PRINT_SCREEN -> SDLK_PRINTSCREEN
            KEYNAME_F1 -> SDLK_F1
            KEYNAME_F2 -> SDLK_F2
            KEYNAME_F3 -> SDLK_F3
            KEYNAME_F4 -> SDLK_F4
            KEYNAME_F5 -> SDLK_F5
            KEYNAME_F6 -> SDLK_F6
            KEYNAME_F7 -> SDLK_F7
            KEYNAME_F8 -> SDLK_F8
            KEYNAME_F9 -> SDLK_F9
            KEYNAME_F10 -> SDLK_F10
            KEYNAME_F11 -> SDLK_F11
            KEYNAME_F12 -> SDLK_F12
            else -> error("unknown key: $key")
        }
    }

    override fun getKeyName(keyId: Int):String = when (keyId) {
        SDLK_SPACE -> KEYNAME_SPACEBAR
        SDLK_RETURN -> KEYNAME_ENTER
        SDLK_TAB -> KEYNAME_TAB
        SDLK_ESCAPE -> KEYNAME_ESCAPE
        SDLK_UP -> KEYNAME_ARROW_UP
        SDLK_DOWN -> KEYNAME_ARROW_DOWN
        SDLK_LEFT -> KEYNAME_ARROW_LEFT
        SDLK_RIGHT -> KEYNAME_ARROW_RIGHT
        SDLK_PRINTSCREEN -> KEYNAME_PRINT_SCREEN
        SDLK_PAGEDOWN -> KEYNAME_PAGE_DOWN
        SDLK_PAGEUP -> KEYNAME_PAGE_UP
        SDLK_HOME -> KEYNAME_HOME
        SDLK_END -> KEYNAME_END
        SDLK_BACKSPACE -> KEYNAME_BACKSPACE
        SDLK_LALT -> KEYNAME_LEFT_ALT
        SDLK_RALT -> KEYNAME_RIGHT_ALT
        SDLK_LCTRL -> KEYNAME_LEFT_CONTROL
        SDLK_RCTRL -> KEYNAME_RIGHT_CONTROL
        SDLK_INSERT -> KEYNAME_INSERT
        SDLK_DELETE -> KEYNAME_DELETE
        SDLK_LSHIFT -> KEYNAME_LEFT_SHIFT
        SDLK_RSHIFT -> KEYNAME_RIGHT_SHIFT
        SDLK_LGUI -> KEYNAME_LEFT_SUPER
        SDLK_RGUI -> KEYNAME_RIGHT_SUPER
        SDLK_F1 -> KEYNAME_F1
        SDLK_F2 -> KEYNAME_F2
        SDLK_F3 -> KEYNAME_F3
        SDLK_F4 -> KEYNAME_F4
        SDLK_F5 -> KEYNAME_F5
        SDLK_F6 -> KEYNAME_F6
        SDLK_F7 -> KEYNAME_F7
        SDLK_F8 -> KEYNAME_F8
        SDLK_F9 -> KEYNAME_F9
        SDLK_F10 -> KEYNAME_F10
        SDLK_F11 -> KEYNAME_F11
        SDLK_F12 -> KEYNAME_F12
        SDLK_CAPSLOCK -> KEYNAME_CAPSLOCK
        else -> SDL_GetKeyName(keyId)?.lowercase() ?: "<null>"
    }
}