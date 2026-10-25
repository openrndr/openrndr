package org.openrndr.biditextdriver

import org.openrndr.draw.font.internal.BidiParagraph
import org.openrndr.draw.font.internal.BidiRun
import org.openrndr.draw.font.internal.BidiText
import org.openrndr.draw.font.internal.Script

/**
 * Analyzes [text], resolving embedding levels and scripts per paragraph.
 * @param requestedLevel the paragraph embedding level (0 or 1) or -1 to determine it per paragraph
 */
internal fun analyzeText(text: String, requestedLevel: Int): BidiTextImpl {
    val length = text.length

    // decode code points, codePointStarts maps code point index to UTF-16 index
    val codePoints = IntArray(length)
    val codePointStarts = IntArray(length + 1)
    var count = 0
    var i = 0
    while (i < length) {
        val c = text[i]
        codePointStarts[count] = i
        if (c.isHighSurrogate() && i + 1 < length && text[i + 1].isLowSurrogate()) {
            codePoints[count++] = 0x10000 + ((c.code - 0xD800) shl 10) + (text[i + 1].code - 0xDC00)
            i += 2
        } else {
            codePoints[count++] = c.code
            i++
        }
    }
    codePointStarts[count] = length

    val classes = ByteArray(count) { UnicodeData.bidiClass(codePoints[it]) }
    val textLevels = ByteArray(length)
    val textClasses = ByteArray(length)
    val textScripts = arrayOfNulls<Script>(length)
    val paragraphs = mutableListOf<BidiParagraph>()

    // P1: split into paragraphs, the paragraph separator is kept with the preceding paragraph and CR LF is treated
    // as a single separator
    var paragraphStart = 0
    while (paragraphStart < count || (count == 0 && paragraphs.isEmpty())) {
        var paragraphEnd = paragraphStart
        while (paragraphEnd < count && classes[paragraphEnd] != B) {
            paragraphEnd++
        }
        if (paragraphEnd < count) {
            if (codePoints[paragraphEnd] == 0x0D && paragraphEnd + 1 < count && codePoints[paragraphEnd + 1] == 0x0A) {
                paragraphEnd++
            }
            paragraphEnd++
        }

        val paragraphCodePoints = codePoints.copyOfRange(paragraphStart, paragraphEnd)
        val resolver = ParagraphResolver(
            paragraphCodePoints,
            classes.copyOfRange(paragraphStart, paragraphEnd),
            requestedLevel
        )
        val scripts = itemizeScripts(paragraphCodePoints)

        for (cp in paragraphStart until paragraphEnd) {
            for (u in codePointStarts[cp] until codePointStarts[cp + 1]) {
                textLevels[u] = resolver.levels[cp - paragraphStart]
                textClasses[u] = classes[cp]
                textScripts[u] = scripts[cp - paragraphStart]
            }
        }
        paragraphs.add(BidiParagraph(codePointStarts[paragraphStart], codePointStarts[paragraphEnd], resolver.paragraphLevel))
        paragraphStart = paragraphEnd
    }

    @Suppress("UNCHECKED_CAST")
    return BidiTextImpl(text, paragraphs, textLevels, textClasses, textScripts as Array<Script>)
}

/**
 * Rule L2: returns the permutation that reorders `levels[from until to]` from logical to visual order. Element `i`
 * of the result is the index (offset by [from]) of the element displayed at visual position `i`.
 */
internal fun reorderPermutation(levels: ByteArray, from: Int, to: Int): IntArray {
    val count = to - from
    val order = IntArray(count) { from + it }
    if (count == 0) return order
    val visualLevels = levels.copyOfRange(from, to)

    var highest = 0
    var lowestOdd = Int.MAX_VALUE
    for (level in visualLevels) {
        val l = level.toInt()
        if (l > highest) highest = l
        if (l and 1 == 1 && l < lowestOdd) lowestOdd = l
    }

    for (level in highest downTo lowestOdd) {
        var i = 0
        while (i < count) {
            if (visualLevels[i] >= level) {
                val start = i
                while (i < count && visualLevels[i] >= level) {
                    i++
                }
                order.reverse(start, i)
                visualLevels.reverse(start, i)
            } else {
                i++
            }
        }
    }
    return order
}

internal class BidiTextImpl(
    override val text: String,
    override val paragraphs: List<BidiParagraph>,
    override val levels: ByteArray,
    private val classes: ByteArray,
    private val scripts: Array<Script>
) : BidiText {

    private fun checkRange(start: Int, end: Int) {
        require(start in 0..end && end <= text.length) { "invalid range [$start, $end) for text of length ${text.length}" }
    }

    override fun scriptAt(index: Int): Script = scripts[index]

    override fun paragraphAt(index: Int): BidiParagraph {
        require(index in 0..text.length) { "index $index out of bounds for text of length ${text.length}" }
        var low = 0
        var high = paragraphs.size - 1
        while (low < high) {
            val mid = (low + high + 1) ushr 1
            if (paragraphs[mid].start <= index) low = mid else high = mid - 1
        }
        return paragraphs[low]
    }

    /** Calls [block] for every part of `[start, end)` that lies in a single paragraph */
    private inline fun forEachParagraphSegment(start: Int, end: Int, block: (paragraph: BidiParagraph, from: Int, to: Int) -> Unit) {
        var position = start
        while (position < end) {
            val paragraph = paragraphAt(position)
            val to = minOf(end, paragraph.end)
            block(paragraph, position, to)
            position = to
        }
    }

    /** Splits `[from, to)` into runs where [levels] and the script change */
    private fun splitRuns(levels: ByteArray, levelOffset: Int, from: Int, to: Int, target: MutableList<BidiRun>) {
        var runStart = from
        for (i in from + 1..to) {
            if (i == to || levels[i - levelOffset] != levels[runStart - levelOffset] || scripts[i] != scripts[runStart]) {
                target.add(BidiRun(runStart, i, levels[runStart - levelOffset].toInt(), scripts[runStart]))
                runStart = i
            }
        }
    }

    override fun runs(start: Int, end: Int): List<BidiRun> {
        checkRange(start, end)
        val result = mutableListOf<BidiRun>()
        forEachParagraphSegment(start, end) { _, from, to ->
            splitRuns(levels, 0, from, to, result)
        }
        return result
    }

    override fun lineLevels(lineStart: Int, lineEnd: Int): ByteArray {
        checkRange(lineStart, lineEnd)
        val result = levels.copyOfRange(lineStart, lineEnd)
        forEachParagraphSegment(lineStart, lineEnd) { paragraph, from, to ->
            val paragraphLevel = paragraph.level.toByte()
            for (i in from until to) {
                val t = classes[i]
                if (t == B || t == S) {
                    result[i - lineStart] = paragraphLevel
                    var j = i - 1
                    while (j >= from && isWhitespaceForL1(classes[j])) {
                        result[j - lineStart] = paragraphLevel
                        j--
                    }
                }
            }
            // trailing whitespace, a segment that ends before the end of the line ends in a paragraph separator
            var j = to - 1
            while (j >= from && isWhitespaceForL1(classes[j])) {
                result[j - lineStart] = paragraphLevel
                j--
            }
        }
        return result
    }

    override fun visualOrder(lineStart: Int, lineEnd: Int): IntArray {
        val lineLevels = lineLevels(lineStart, lineEnd)
        val result = IntArray(lineEnd - lineStart)
        forEachParagraphSegment(lineStart, lineEnd) { _, from, to ->
            val order = reorderPermutation(lineLevels, from - lineStart, to - lineStart)
            for (i in order.indices) {
                result[from - lineStart + i] = order[i] + lineStart
            }
        }
        // keep surrogate pairs in logical order
        var i = 0
        while (i < result.size - 1) {
            val index = result[i]
            if (text[index].isLowSurrogate() && result[i + 1] == index - 1 && text[index - 1].isHighSurrogate()) {
                result[i] = index - 1
                result[i + 1] = index
                i += 2
            } else {
                i++
            }
        }
        return result
    }

    override fun visualRuns(lineStart: Int, lineEnd: Int): List<BidiRun> {
        val lineLevels = lineLevels(lineStart, lineEnd)
        val result = mutableListOf<BidiRun>()
        forEachParagraphSegment(lineStart, lineEnd) { _, from, to ->
            val runs = mutableListOf<BidiRun>()
            splitRuns(lineLevels, lineStart, from, to, runs)
            val runLevels = ByteArray(runs.size) { runs[it].level.toByte() }
            for (index in reorderPermutation(runLevels, 0, runs.size)) {
                result.add(runs[index])
            }
        }
        return result
    }
}
