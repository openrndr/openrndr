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

@JvmRecord
data class TextStyle(val justify: Boolean)

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


    fun addText(face: Face, text: String, box: Rectangle, style: TextStyle = TextStyle(false)) {
        
        
        val lineHeight = face.ascent - face.descent + face.lineGap
        val lineWidth = box.width

        // Build items list from text (Box, Glue, Penalty per Knuth-Plass)
        val items = mutableListOf<KPItem>()
        val words = mutableListOf<String>()
        val currentWord = StringBuilder()

        for (ch in text) {
            if (ch == ' ' || ch == '\t') {
                if (currentWord.isNotEmpty()) {
                    words.add(currentWord.toString())
                    currentWord.clear()
                }
                words.add(" ")
            } else if (ch == '\n') {
                if (currentWord.isNotEmpty()) {
                    words.add(currentWord.toString())
                    currentWord.clear()
                }
                words.add("\n")
            } else {
                currentWord.append(ch)
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
        var y = box.y + face.ascent

        for (lineIdx in 0 until bpChain.size - 1) {
            val startBp = bpChain[lineIdx]
            val endBp = bpChain[lineIdx + 1]
            val start = startBp.index
            val end = endBp.index

            // Compute adjustment ratio for this line
            val r = computeAdjustmentRatio(startBp, end)

            var cursor = Vector2(box.x, y)

            // Skip leading glue at the start of each line (after a line break)
            var lineStart = start
            if (lineIdx > 0) {
                while (lineStart < end && items[lineStart] is KPItem.Glue) {
                    lineStart++
                }
            }

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
                        val adjustedWidth = if (r >= 0) {
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