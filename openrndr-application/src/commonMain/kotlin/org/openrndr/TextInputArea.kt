package org.openrndr

import org.openrndr.shape.Rectangle
import kotlin.jvm.JvmRecord

/**
 * Where text is being typed in a window: the [area] of the text, for example the line the caret is on, and the
 * position of the caret, [cursor] units from the left edge of [area]. The area is in the units of the program,
 * those of `drawer.bounds`, with the origin at the top left of the window.
 *
 * @see Window.textInputArea
 */
@JvmRecord
data class TextInputArea(val area: Rectangle, val cursor: Double = 0.0)
