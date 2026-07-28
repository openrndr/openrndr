package org.openrndr.draw.slug

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.Drawer
import org.openrndr.draw.MagnifyingFilter
import org.openrndr.draw.MinifyingFilter
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.font.Face
import org.openrndr.draw.font.internal.ShapeResult
import org.openrndr.draw.font.internal.TextShapingDriver
import org.openrndr.math.Vector2
import org.openrndr.math.transforms.transform
import org.openrndr.shape.Rectangle
import kotlin.jvm.JvmRecord
import kotlin.math.abs

private data class KPBox(val width: Double, val shapeResults: List<ShapeResult>)
private data class KPGlue(val width: Double, val stretch: Double, val shrink: Double)
private data class KPPenalty(val width: Double, val penalty: Double, val flagged: Boolean)

private sealed class KPItem {
    class Box(val box: KPBox) : KPItem()
    class Glue(val glue: KPGlue) : KPItem()
    class Penalty(val penalty: KPPenalty) : KPItem()
}

private data class KPBreakpoint(
    val index: Int,
    val demerits: Double,
    val line: Int,
    val totalWidth: Double,
    val totalStretch: Double,
    val totalShrink: Double,
    val previous: KPBreakpoint?
)

/**
 * Represents a style configuration for text rendering.
 *
 * @property justify Determines whether the text should be justified.
 * @property lineHeightInEm Specifies the line height multiplier for the text.
 * @property horizontalAlign Defines the horizontal alignment of the text, typically in the range [0.0, 1.0].
 * @property verticalAlign Defines the vertical alignment of the text, typically in the range [0.0, 1.0].
 */

@JvmRecord
data class TextStyle(
    val face: Face? = null,
    val sizeInEm: Double? = null,
    val justify: Boolean? = null,
    val lineHeightInEm: Double? = null,
    val characterSpacingInEm: Double? = null,
    val baselineShiftInEm: Double? = null,
    val horizontalAlign: Double? = null,
    val verticalAlign: Double? = null,
    val fill: ColorRGBa? = null,
    val stroke: ColorRGBa? = null,
    val strokeWeight: Double? = null,
) {
    fun cascade(other: TextStyle): TextStyle {
        return copy(
            face = other.face ?: face,
            sizeInEm = other.sizeInEm ?: sizeInEm,
            justify = other.justify ?: justify,
            lineHeightInEm = other.lineHeightInEm ?: lineHeightInEm,
            characterSpacingInEm = other.characterSpacingInEm ?: characterSpacingInEm,
            baselineShiftInEm = other.baselineShiftInEm ?: baselineShiftInEm,
            horizontalAlign = other.horizontalAlign ?: horizontalAlign,
            verticalAlign = other.verticalAlign ?: verticalAlign,
            fill = other.fill ?: fill,
            stroke = other.stroke ?: stroke,
            strokeWeight = other.strokeWeight ?: strokeWeight
        )
    }
    companion object {
        val Defaults = TextStyle(
            sizeInEm = 1.0,
            justify = false,
            lineHeightInEm = 1.0,
            characterSpacingInEm = 0.0,
            baselineShiftInEm = 0.0,
            horizontalAlign = 0.0,
            verticalAlign = 0.0,
            fill = ColorRGBa.WHITE,
        )
    }
}

class MutableTextStyle(
    var face: Face? = null,
    var sizeInEm: Double? = 1.0,
    var justify: Boolean = false,
    var lineHeight: Double = 1.0,
    var characterSpacingInEm: Double = 0.0,
    var baselineShiftInEm: Double = 0.0,
    var horizontalAlign: Double = 0.0,
    var verticalAlign: Double = 0.0,
    var fill: ColorRGBa? = null,
    var stroke: ColorRGBa? = null,
    var strokeWeight: Double? = null
) {

    fun cascade(other: TextStyle) {
        face = other.face ?: face
        sizeInEm = other.sizeInEm ?: sizeInEm
        justify = other.justify ?: justify
        lineHeight = other.lineHeightInEm ?: lineHeight
        characterSpacingInEm = other.characterSpacingInEm ?: characterSpacingInEm
        horizontalAlign = other.horizontalAlign ?: horizontalAlign
        verticalAlign = other.verticalAlign ?: verticalAlign
        fill = other.fill ?: fill
        stroke = other.stroke ?: stroke
        strokeWeight = other.strokeWeight ?: strokeWeight
    }

    fun cascade(other: MutableTextStyle) {
        face = other.face ?: face
        sizeInEm = other.sizeInEm ?: sizeInEm
        justify = other.justify ?: justify
        lineHeight = other.lineHeight ?: lineHeight
        characterSpacingInEm = other.characterSpacingInEm ?: characterSpacingInEm
        horizontalAlign = other.horizontalAlign ?: horizontalAlign
        verticalAlign = other.verticalAlign ?: verticalAlign
        fill = other.fill ?: fill
        stroke = other.stroke ?: stroke
        strokeWeight = other.strokeWeight ?: strokeWeight
    }


}

data class TextSpan(val text: String, val style: TextStyle?)

data class TextBox(val box: Rectangle, val spans: List<TextSpan>)

class SlugTextDrawer {

    val shaper = TextShapingDriver.instance

    val slugMap = SlugMap(
        colorBuffer(4096, 64, type = ColorType.FLOAT32, format = ColorFormat.RG),
        colorBuffer(4096, 64, type = ColorType.UINT16_INT, format = ColorFormat.RGBa)
    ).apply {
        bands.filter(MinifyingFilter.NEAREST, MagnifyingFilter.NEAREST)
    }

    val slugGlyphMap = SlugGlyphMap(slugMap)

    val slugDrawer = SlugDrawer()

    val commands = mutableListOf<SlugCommand>()

    var style = TextStyle.Defaults

    fun pushStyle() {
        styleStack.add(style)
    }

    fun popStyle() {
        style = styleStack.removeLast()
    }

    val styleStack = mutableListOf<TextStyle>()

    fun addText(text: String, box: Rectangle, style: TextStyle = TextStyle()) {

        val style = this.style.cascade(style)

        style.lineHeightInEm ?: error("lineHeightInEm not set")
        style.verticalAlign ?: error("verticalAlign not set")
        style.horizontalAlign ?: error("horizontalAlign not set")
        style.justify ?: error("justify not set")


        val face = style.face ?: error("face not set")
        val lineHeight = (face.ascent - face.descent + face.lineGap) * style.lineHeightInEm
        val lineWidth = box.width

        // Build items list from text (Box, Glue, Penalty per Knuth-Plass)
        val items = mutableListOf<KPItem>()
        val words = mutableListOf<String>()
        val currentWord = StringBuilder()

        for (ch in text) {
            when (ch) {
                ' ', '\t' -> {
                    if (currentWord.isNotEmpty()) {
                        words.add(currentWord.toString())
                        currentWord.clear()
                    }
                    words.add(" ")
                }
                '\n' -> {
                    if (currentWord.isNotEmpty()) {
                        words.add(currentWord.toString())
                        currentWord.clear()
                    }
                    words.add("\n")
                }
                else -> {
                    currentWord.append(ch)
                }
            }
        }
        if (currentWord.isNotEmpty()) {
            words.add(currentWord.toString())
        }

        // Measure space width
        val spaceGlyph = face.glyphForCharacter(' ')
        val spaceWidth = spaceGlyph.advanceWidth()

        for (word in words) {
            when (word) {
                " " -> {
                    items.add(KPItem.Glue(KPGlue(spaceWidth, spaceWidth / 2.0, spaceWidth / 3.0)))
                }
                "\n" -> {
                    items.add(KPItem.Penalty(KPPenalty(0.0, Double.POSITIVE_INFINITY, false)))
                    items.add(KPItem.Glue(KPGlue(0.0, 10000.0, 0.0)))
                    items.add(KPItem.Penalty(KPPenalty(0.0, -10000.0, false)))
                }
                else -> {
                    val shaped = shaper.shape(face, word)
                    var wordWidth = 0.0
                    for (sr in shaped) {
                        wordWidth += sr.advance.x
                    }
                    items.add(KPItem.Box(KPBox(wordWidth, shaped)))
                }
            }
        }

        // Add finishing penalty (forced break at end)
        items.add(KPItem.Penalty(KPPenalty(0.0, Double.POSITIVE_INFINITY, false)))
        items.add(KPItem.Glue(KPGlue(0.0, 10000.0, 0.0)))
        items.add(KPItem.Penalty(KPPenalty(0.0, -10000.0, false)))

        // Compute cumulative widths
        val cumWidth = DoubleArray(items.size + 1)
        val cumStretch = DoubleArray(items.size + 1)
        val cumShrink = DoubleArray(items.size + 1)

        for (i in items.indices) {
            cumWidth[i + 1] = cumWidth[i] + when (val item = items[i]) {
                is KPItem.Box -> item.box.width
                is KPItem.Glue -> item.glue.width
                is KPItem.Penalty -> 0.0
            }
            cumStretch[i + 1] = cumStretch[i] + when (val item = items[i]) {
                is KPItem.Glue -> item.glue.stretch
                else -> 0.0
            }
            cumShrink[i + 1] = cumShrink[i] + when (val item = items[i]) {
                is KPItem.Glue -> item.glue.shrink
                else -> 0.0
            }
        }

        // Knuth-Plass breakpoint finding
        val activeBreakpoints = mutableListOf(
            KPBreakpoint(0, 0.0, 0, 0.0, 0.0, 0.0, null)
        )

        fun computeAdjustmentRatio(bp: KPBreakpoint, itemIndex: Int): Double {
            val w = cumWidth[itemIndex] - bp.totalWidth
            return if (w < lineWidth) {
                val stretch = cumStretch[itemIndex] - bp.totalStretch
                if (stretch > 0) (lineWidth - w) / stretch else 10000.0
            } else if (w > lineWidth) {
                val shrink = cumShrink[itemIndex] - bp.totalShrink
                if (shrink > 0) (lineWidth - w) / shrink else -10000.0
            } else {
                0.0
            }
        }

        fun computeDemerits(penalty: Double, r: Double): Double {
            val badness = if (r < -1.0) 10000.0 else 100.0 * abs(r).let { it * it * it }
            return if (penalty >= 0) {
                (1.0 + badness + penalty) * (1.0 + badness + penalty)
            } else if (penalty > -10000.0) {
                (1.0 + badness) * (1.0 + badness) - penalty * penalty
            } else {
                (1.0 + badness) * (1.0 + badness)
            }
        }

        for (i in items.indices) {
            val item = items[i]
            if (item is KPItem.Penalty && item.penalty.penalty >= 10000.0) continue
            if (item !is KPItem.Penalty && item !is KPItem.Glue) continue
            if (item is KPItem.Glue && (i == 0 || items[i - 1] !is KPItem.Box)) continue

            val breakIndex = i
            val newWidth = cumWidth[breakIndex + 1]
            val newStretch = cumStretch[breakIndex + 1]
            val newShrink = cumShrink[breakIndex + 1]

            val toRemove = mutableListOf<KPBreakpoint>()
            var bestCandidate: KPBreakpoint? = null
            var bestDemerits = Double.MAX_VALUE

            for (bp in activeBreakpoints) {
                val r = computeAdjustmentRatio(bp, breakIndex)

                if (r < -1.0 || (item is KPItem.Penalty && item.penalty.penalty == -10000.0)) {
                    toRemove.add(bp)
                }

                if (r >= -1.0 && r <= 10000.0) {
                    val penalty = if (item is KPItem.Penalty) item.penalty.penalty else 0.0
                    val d = bp.demerits + computeDemerits(penalty, r)
                    if (d < bestDemerits) {
                        bestDemerits = d
                        bestCandidate = bp
                    }
                }
            }

            activeBreakpoints.removeAll(toRemove)

            if (bestCandidate != null) {
                activeBreakpoints.add(
                    KPBreakpoint(
                        breakIndex,
                        bestDemerits,
                        bestCandidate.line + 1,
                        newWidth,
                        newStretch,
                        newShrink,
                        bestCandidate
                    )
                )
            }
        }

        // Find best final breakpoint
        val finalBp = activeBreakpoints.minByOrNull { it.demerits } ?: return

        // Collect breakpoints in order
        val bpChain = mutableListOf<KPBreakpoint>()
        var bp: KPBreakpoint? = finalBp
        while (bp != null) {
            bpChain.add(bp)
            bp = bp.previous
        }
        bpChain.reverse()

        // Render lines
        val totalLines = bpChain.size - 1
        val totalTextHeight = face.ascent + (totalLines - 1) * lineHeight - face.descent
        val verticalOffset = (box.height - totalTextHeight) * style.verticalAlign
        var y = box.y + face.ascent + verticalOffset

        for (lineIdx in 0 until bpChain.size - 1) {
            val startBp = bpChain[lineIdx]
            val endBp = bpChain[lineIdx + 1]
            val start = startBp.index
            val end = endBp.index

            // Compute adjustment ratio for this line
            val r = computeAdjustmentRatio(startBp, end)

            // Skip leading glue at the start of each line (after a line break)
            var lineStart = start
            if (lineIdx > 0) {
                while (lineStart < end && items[lineStart] is KPItem.Glue) {
                    lineStart++
                }
            }

            // Compute natural line width for horizontal alignment
            val naturalLineWidth = run {
                var w = 0.0
                val lr = computeAdjustmentRatio(startBp, end)
                for (idx in lineStart until end) {
                    when (val itm = items[idx]) {
                        is KPItem.Box -> w += itm.box.width
                        is KPItem.Glue -> {
                            val isLastLine = lineIdx == bpChain.size - 2
                            w += if (!style.justify || isLastLine) {
                                if (lr < 0) itm.glue.width + lr * itm.glue.shrink else itm.glue.width
                            } else if (lr >= 0) {
                                itm.glue.width + lr * itm.glue.stretch
                            } else {
                                itm.glue.width + lr * itm.glue.shrink
                            }
                        }
                        is KPItem.Penalty -> {}
                    }
                }
                w
            }
            val horizontalOffset = (lineWidth - naturalLineWidth) * style.horizontalAlign
            var cursor = Vector2(box.x + horizontalOffset, y)

            for (idx in lineStart until end) {
                when (val itm = items[idx]) {
                    is KPItem.Box -> {
                        for (sr in itm.box.shapeResults) {
                            slugGlyphMap.getSlugForGlyphIndex(face, sr.glyphIndex)
                        }
                        for (sr in itm.box.shapeResults) {
                            val slugIndex = slugGlyphMap.getSlugForGlyphIndex(face, sr.glyphIndex)
                            val command = SlugCommand(
                                slugIndex,
                                transform {
                                    translate(cursor + sr.offset * 0.0)
                                },
                                ColorRGBa.WHITE,
                                ColorRGBa.TRANSPARENT,
                                0.0,
                            )
                            commands.add(command)
                            cursor += sr.advance
                        }
                    }
                    is KPItem.Glue -> {
                        val isLastLine = lineIdx == bpChain.size - 2
                        val adjustedWidth = if (!style.justify || isLastLine) {
                            // When not justified or on the last line, only shrink to prevent overflow
                            if (r < 0) {
                                itm.glue.width + r * itm.glue.shrink
                            } else {
                                itm.glue.width
                            }
                        } else if (r >= 0) {
                            itm.glue.width + r * itm.glue.stretch
                        } else {
                            itm.glue.width + r * itm.glue.shrink
                        }
                        cursor += Vector2(adjustedWidth, 0.0)
                    }
                    is KPItem.Penalty -> { /* skip */ }
                }
            }

            y += lineHeight
        }
    }

    fun addText(face: Face, text: String, position: Vector2) {
        shaper.shape(face, text)

        val shapeResult = shaper.shape(face, text)

        for (item in shapeResult) {
            slugGlyphMap.getSlugForGlyphIndex(face, item.glyphIndex)
        }

        var cursor = position

        for ((sindex, i) in shapeResult.withIndex()) {
            val slugIndex = slugGlyphMap.getSlugForGlyphIndex(face, i.glyphIndex)
            val command = SlugCommand(
                slugIndex,
                transform {
                    translate(cursor + i.offset * 0.0)
                },
                ColorRGBa.WHITE,
                ColorRGBa.TRANSPARENT,
                0.0,

            )
            commands.add(command)
            cursor += shapeResult[sindex].advance
        }
    }

    fun clear() {
        commands.clear()
    }

    fun draw(drawer: Drawer) {
        slugDrawer.prepare(slugMap, commands)
        slugDrawer.draw(drawer, slugMap)
    }

}