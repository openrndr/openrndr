package org.openrndr.draw

import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import kotlin.jvm.JvmName

class Cursor(var x: Double = 0.0, var y: Double = 0.0) {
    constructor(cursor: Cursor) : this(cursor.x, cursor.y)
}

@Suppress("unused")
class TextToken(val token: String, val x: Double, val y: Double, val width: Double, val tracking: Double)

class WriteStyle {
    var leading = 0.0
    var tracking = 0.0
    var ellipsis: String? = "…"
}

@Suppress("unused", "UNUSED_PARAMETER")
class Writer(val drawerRef: Drawer?) {
    var cursor = Cursor()
    var box = Rectangle(
        Vector2.ZERO, drawerRef?.width?.toDouble() ?: Double.POSITIVE_INFINITY, drawerRef?.height?.toDouble()
            ?: Double.POSITIVE_INFINITY
    )
        set(value) {
            field = value
            cursor.x = value.corner.x
            cursor.y = value.corner.y
        }

    var style = WriteStyle()
    val styleStack = ArrayDeque<WriteStyle>()


    var leading
        get() = style.leading
        set(value) {
            style.leading = value
        }

    var tracking
        get() = style.tracking
        set(value) {
            style.tracking = value
        }

    var ellipsis
        get() = style.ellipsis
        set(value) {
            style.ellipsis = value
        }


    var drawStyle: DrawStyle = DrawStyle()
        get() {
            return drawerRef?.drawStyle ?: field
        }
        set(value) {
            field = drawStyle
        }

    fun newLine() {
        cursor.x = box.corner.x
        cursor.y += /*(drawer.drawStyle.fontMap?.height ?: 0.0)*/ +(drawStyle.fontMap?.leading
            ?: 0.0) + style.leading
    }

    fun gaplessNewLine() {
        cursor.x = box.corner.x
        cursor.y += drawStyle.fontMap?.height ?: 0.0
    }

    fun move(x: Double, y: Double) {
        cursor.x += x
        cursor.y += y
    }

    fun textWidth(text: String): Double =
        text.sumOf { (drawStyle.fontMap as FontImageMap).glyphMetrics[it]?.advanceWidth ?: 0.0 } +
                (text.length - 1).coerceAtLeast(0) * style.tracking

    /**
     * Draw text
     * @param text the text to write, may contain newlines
     * @param visible draw the text when set to true, when set to false only type setting is performed
     * @return a list of [TextToken] instances
     */
    fun text(text: String, visible: Boolean = true) : List<TextToken> {
        // Triggers loading the default font (if needed) by accessing .fontMap
        // otherwise makeRenderTokens() is not aware of the default font.
        drawerRef?.fontMap

        val renderTokens = makeTextTokens(text, false)

        if (visible) {
            drawTextTokens(renderTokens)
        }
        return renderTokens
    }

    /**
     * Draw pre-set text tokens.
     * @param tokens a list of [TextToken] instances
     * @since 0.4.3
     */
    fun drawTextTokens(tokens: List<TextToken>) {
        drawerRef?.let { d ->
            val renderer = d.fontImageMapDrawer
            tokens.forEach {
                renderer.queueText(
                    fontMap = d.drawStyle.fontMap!!,
                    text = it.token,
                    x = it.x,
                    y = it.y,
                    tracking = style.tracking,
                    kerning = drawStyle.kerning,
                    textSetting = drawStyle.textSetting
                )
            }
            renderer.flush(d.context, d.drawStyle)
        }
    }

    private fun makeTextTokens(text: String, mustFit: Boolean = false): List<TextToken> {
        drawStyle.fontMap?.let { font ->

            var fits = true
            font as FontImageMap
            val lines = text.split("((?<=\n)|(?=\n))".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val tokens = mutableListOf<String>()
            lines.forEach { line ->
                val lineTokens = line.split(" ")
                tokens.addAll(lineTokens)
            }

            val localCursor = Cursor(cursor)

            val spaceWidth = font.glyphMetrics[' ']!!.advanceWidth
            val verticalSpace = style.leading + font.leading

            val textTokens = mutableListOf<TextToken>()

            tokenLoop@ for (i in 0 until tokens.size) {

                val token = tokens[i]
                if (token == "\n") {
                    localCursor.x = box.corner.x
                    localCursor.y += verticalSpace
                } else {
                    val tokenWidth = token.sumOf {
                        font.glyphMetrics[it]?.advanceWidth ?: 0.0
                    } + style.tracking * token.length
                    if (localCursor.x + tokenWidth < box.x + box.width && localCursor.y <= box.y + box.height) run {
                        val textToken = TextToken(token, localCursor.x, localCursor.y, tokenWidth, style.tracking)
                        emitToken(localCursor, textTokens, textToken)
                    } else {

                        if (localCursor.y > box.corner.y + box.height) {
                            fits = false
                        }
                        if (localCursor.y + verticalSpace <= box.y + box.height) {
                            localCursor.y += verticalSpace
                            localCursor.x = box.x

                            emitToken(
                                localCursor,
                                textTokens,
                                TextToken(token, localCursor.x, localCursor.y, tokenWidth, style.tracking)
                            )
                        } else {
                            if (!mustFit && style.ellipsis != null && cursor.y <= box.y + box.height) {
                                emitToken(
                                    localCursor, textTokens, TextToken(
                                        style.ellipsis
                                            ?: "", localCursor.x, localCursor.y, tokenWidth, style.tracking
                                    )
                                )
                                break@tokenLoop
                            } else {
                                fits = false
                            }
                        }
                    }
                    localCursor.x += tokenWidth

                    if (i != tokens.size - 1) {
                        localCursor.x += spaceWidth
                    }
                }
            }
            if (fits || (!fits && !mustFit)) {
                cursor = Cursor(localCursor)
            } else {
                textTokens.clear()
            }
            return textTokens
        }
        return emptyList()
    }

    private fun emitToken(cursor: Cursor, textTokens: MutableList<TextToken>, textToken: TextToken) {
        textTokens.add(textToken)
    }
}

fun <T> writer(drawer: Drawer, f: Writer.() -> T): T {
    val writer = Writer(drawer)
    return writer.f()
}


@JvmName("drawerWriter")
fun <T> Drawer.writer(f: Writer.() -> T): T {
    return writer(this, f)
}
