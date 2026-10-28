package org.openrndr.biditextdriver

import org.openrndr.draw.font.internal.Script

/**
 * Lookup table for a code point property that is stored as runs, see generator/generate_unicode_tables.py
 */
internal class RunTable(data: String) {
    private val starts: IntArray
    private val values: IntArray

    init {
        val entries = data.split(',')
        starts = IntArray(entries.size)
        values = IntArray(entries.size)
        for ((i, entry) in entries.withIndex()) {
            val colon = entry.indexOf(':')
            starts[i] = entry.substring(0, colon).toInt(16)
            values[i] = entry.substring(colon + 1).toInt()
        }
    }

    operator fun get(codePoint: Int): Int {
        var low = 0
        var high = starts.size - 1
        while (low < high) {
            val mid = (low + high + 1) ushr 1
            if (starts[mid] <= codePoint) low = mid else high = mid - 1
        }
        return values[low]
    }
}

internal object UnicodeData {
    private val bidiClasses = RunTable(BIDI_CLASS_DATA)

    // Bidi classes of the first 256 code points, avoids the table search for the most common text
    private val latin1BidiClasses = ByteArray(256) { bidiClasses[it].toByte() }

    private val scripts = RunTable(SCRIPT_DATA)
    private val scriptValues: Array<Script> = run {
        val byTag = Script.entries.associateBy { it.tag }
        Array(SCRIPT_TAGS.size) { byTag[SCRIPT_TAGS[it]] ?: Script.UNKNOWN }
    }

    /** opening bracket -> closing bracket */
    private val openingBrackets = HashMap<Int, Int>()

    /** closing bracket -> opening bracket */
    private val closingBrackets = HashMap<Int, Int>()

    private val mirrors = HashMap<Int, Int>()

    init {
        for (entry in BIDI_BRACKET_DATA.split(',')) {
            val (codePoint, paired, type) = entry.split(':')
            when (type) {
                "o" -> openingBrackets[codePoint.toInt(16)] = paired.toInt(16)
                "c" -> closingBrackets[codePoint.toInt(16)] = paired.toInt(16)
            }
        }
        for (entry in BIDI_MIRRORING_DATA.split(',')) {
            val (codePoint, mirror) = entry.split(':')
            mirrors[codePoint.toInt(16)] = mirror.toInt(16)
        }
    }

    fun bidiClass(codePoint: Int): Byte =
        if (codePoint in 0..255) latin1BidiClasses[codePoint] else bidiClasses[codePoint].toByte()

    fun script(codePoint: Int): Script = scriptValues[scripts[codePoint]]

    fun mirror(codePoint: Int): Int? = mirrors[codePoint]

    /**
     * Returns the closing bracket paired with the opening bracket [codePoint], normalized to its canonical
     * equivalent, or -1 when [codePoint] is not an opening bracket.
     */
    fun closingBracketFor(codePoint: Int): Int {
        val closing = openingBrackets[codePoint] ?: return -1
        return canonicalBracket(closing)
    }

    /**
     * Returns [codePoint] normalized to its canonical equivalent when it is a closing bracket, or -1 when
     * [codePoint] is not a closing bracket.
     */
    fun closingBracket(codePoint: Int): Int {
        if (codePoint !in closingBrackets) return -1
        return canonicalBracket(codePoint)
    }

    /**
     * Brackets are matched under canonical equivalence (BD16), the only bracket characters with a canonical
     * decomposition are the angle brackets U+2329 and U+232A.
     */
    private fun canonicalBracket(codePoint: Int): Int = when (codePoint) {
        0x2329 -> 0x3008
        0x232A -> 0x3009
        else -> codePoint
    }
}
