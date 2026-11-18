package org.openrndr.webgl

import org.openrndr.*
import org.openrndr.internal.KeyboardDriver

/**
 * Keyboard driver for the browser. Key ids identify physical keys, they are derived from `KeyboardEvent.code` and use
 * the same values as GLFW does on the JVM.
 */
class KeyboardDriverWebGL : KeyboardDriver {
    override fun getKeyId(key: String): Int = keyIds[key] ?: error("unknown key: $key")

    override fun getKeyName(keyId: Int): String = keyNames[keyId] ?: "<null>"

    /**
     * Returns the key id for a `KeyboardEvent.code` value, or -1 for codes without an id
     */
    fun getKeyIdForCode(code: String): Int = codeIds[code] ?: -1

    companion object {
        private val codeIds: Map<String, Int> = buildMap {
            put("Space", 32)
            put("Quote", 39)
            put("Comma", 44)
            put("Minus", 45)
            put("Period", 46)
            put("Slash", 47)
            for (i in 0..9) put("Digit$i", 48 + i)
            put("Semicolon", 59)
            put("Equal", 61)
            for (c in 'A'..'Z') put("Key$c", c.code)
            put("BracketLeft", 91)
            put("Backslash", 92)
            put("BracketRight", 93)
            put("Backquote", 96)
            put("Escape", 256)
            put("Enter", 257)
            put("Tab", 258)
            put("Backspace", 259)
            put("Insert", 260)
            put("Delete", 261)
            put("ArrowRight", 262)
            put("ArrowLeft", 263)
            put("ArrowDown", 264)
            put("ArrowUp", 265)
            put("PageUp", 266)
            put("PageDown", 267)
            put("Home", 268)
            put("End", 269)
            put("CapsLock", 280)
            put("ScrollLock", 281)
            put("NumLock", 282)
            put("PrintScreen", 283)
            put("Pause", 284)
            for (i in 1..12) put("F$i", 289 + i)
            for (i in 0..9) put("Numpad$i", 320 + i)
            put("NumpadDecimal", 330)
            put("NumpadDivide", 331)
            put("NumpadMultiply", 332)
            put("NumpadSubtract", 333)
            put("NumpadAdd", 334)
            put("NumpadEnter", 335)
            put("NumpadEqual", 336)
            put("ShiftLeft", 340)
            put("ControlLeft", 341)
            put("AltLeft", 342)
            put("MetaLeft", 343)
            put("ShiftRight", 344)
            put("ControlRight", 345)
            put("AltRight", 346)
            put("MetaRight", 347)
            put("ContextMenu", 348)
        }

        private val keyIds: Map<String, Int> = mapOf(
            KEYNAME_SPACEBAR to 32,
            KEYNAME_ESCAPE to 256,
            KEYNAME_ENTER to 257,
            KEYNAME_TAB to 258,
            KEYNAME_BACKSPACE to 259,
            KEYNAME_INSERT to 260,
            KEYNAME_DELETE to 261,
            KEYNAME_ARROW_RIGHT to 262,
            KEYNAME_ARROW_LEFT to 263,
            KEYNAME_ARROW_DOWN to 264,
            KEYNAME_ARROW_UP to 265,
            KEYNAME_PAGE_UP to 266,
            KEYNAME_PAGE_DOWN to 267,
            KEYNAME_HOME to 268,
            KEYNAME_END to 269,
            KEYNAME_CAPSLOCK to 280,
            KEYNAME_PRINT_SCREEN to 283,
            KEYNAME_F1 to 290,
            KEYNAME_F2 to 291,
            KEYNAME_F3 to 292,
            KEYNAME_F4 to 293,
            KEYNAME_F5 to 294,
            KEYNAME_F6 to 295,
            KEYNAME_F7 to 296,
            KEYNAME_F8 to 297,
            KEYNAME_F9 to 298,
            KEYNAME_F10 to 299,
            KEYNAME_F11 to 300,
            KEYNAME_F12 to 301,
            KEYNAME_LEFT_SHIFT to 340,
            KEYNAME_LEFT_CONTROL to 341,
            KEYNAME_LEFT_ALT to 342,
            KEYNAME_LEFT_SUPER to 343,
            KEYNAME_RIGHT_SHIFT to 344,
            KEYNAME_RIGHT_CONTROL to 345,
            KEYNAME_RIGHT_ALT to 346,
            KEYNAME_RIGHT_SUPER to 347,
        )

        private val keyNames: Map<Int, String> = keyIds.entries.associate { (name, id) -> id to name }
    }
}
