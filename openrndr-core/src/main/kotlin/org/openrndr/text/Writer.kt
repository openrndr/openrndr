package org.openrndr.text

import org.openrndr.Program
import org.openrndr.draw.DrawStyle
import org.openrndr.draw.Drawer
import org.openrndr.draw.FontImageMap
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import java.util.*

class Cursor(var x: Double = 0.0, var y: Double = 0.0) {
    constructor(cursor: Cursor) : this(cursor.x, cursor.y)
}

@Suppress("unused")
class RenderToken(val token: String, val x: Double, val y: Double, val width: Double, val tracking: Double)

class WriteStyle {
    var leading = 0.0
    var tracking = 0.0
    var ellipsis: String? = "…"
}

@Suppress("unused", "UNUSED_PARAMETER")
class Writer(val drawer: Drawer?) {

    var cursor = Cursor()
    var box = Rectangle(Vector2.ZERO, drawer?.width?.toDouble() ?: Double.POSITIVE_INFINITY, drawer?.height?.toDouble()
            ?: Double.POSITIVE_INFINITY)
        set(value) {
            field = value
            cursor.x = value.corner.x
            cursor.y = value.corner.y
        }

    var style = WriteStyle()
    val styleStack = Stack<WriteStyle>()


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
            return drawer?.drawStyle ?: field
        }
        set(value: DrawStyle) {
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
            text.sumByDouble { (drawStyle.fontMap as FontImageMap).glyphMetrics[it]?.advanceWidth ?: 0.0 } +
                    (text.length - 1).coerceAtLeast(0) * style.tracking

    fun text(text: String, visible: Boolean = true) {
        val renderTokens = makeRenderTokens(text, false)


        if (visible) {
            drawer?.let { d ->
                val renderer = d.fontImageMapDrawer
                renderTokens.forEach { renderer.queueText(d.drawStyle.fontMap!!, it.token, it.x, it.y, style.tracking) }
                renderer.flush(d.context, d.drawStyle)
            }
        }
    }

    private fun makeRenderTokens(text: String, mustFit: Boolean = false): List<RenderToken> {
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

            val renderTokens = mutableListOf<RenderToken>()

            tokenLoop@ for (i in 0 until tokens.size) {

                val token = tokens[i]
                if (token == "\n") {
                    localCursor.x = box.corner.x
                    localCursor.y += verticalSpace
                } else {
                    val tokenWidth = token.sumByDouble {
                        font.glyphMetrics[it]?.advanceWidth ?: 0.0
                    } + style.tracking * token.length
                    if (localCursor.x + tokenWidth < box.x + box.width && localCursor.y <= box.y + box.height) run {
                        val renderToken = RenderToken(token, localCursor.x, localCursor.y, tokenWidth, style.tracking)
                        emitToken(localCursor, renderTokens, renderToken)
                    } else {

                        if (localCursor.y > box.corner.y + box.height) {
                            fits = false
                        }
                        if (localCursor.y + verticalSpace <= box.y + box.height) {
                            localCursor.y += verticalSpace
                            localCursor.x = box.x

                            emitToken(localCursor, renderTokens, RenderToken(token, localCursor.x, localCursor.y, tokenWidth, style.tracking))
                        } else {
                            if (!mustFit && style.ellipsis != null && cursor.y <= box.y + box.height) {
                                emitToken(localCursor, renderTokens, RenderToken(style.ellipsis
                                        ?: "", localCursor.x, localCursor.y, tokenWidth, style.tracking))
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
                renderTokens.clear()
            }

            return renderTokens
        }
        return emptyList()
    }

    private fun emitToken(cursor: Cursor, renderTokens: MutableList<RenderToken>, renderToken: RenderToken) {
        renderTokens.add(renderToken)
    }
}

fun writer(drawer: Drawer, f: Writer.() -> Unit) {
    val writer = Writer(drawer)
    writer.f()
}

fun Program.writer(f: Writer.() -> Unit) {
    writer(drawer, f)
}

@JvmName("drawerWriter")
fun Drawer.writer(f: Writer.() -> Unit) {
    writer(this, f)
}
