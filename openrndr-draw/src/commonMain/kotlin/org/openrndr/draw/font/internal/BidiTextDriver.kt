package org.openrndr.draw.font.internal

import kotlin.jvm.JvmRecord

/**
 * Bidirectional character types as defined in Unicode Standard Annex #9.
 */
enum class BidiClass {
    /** Left-to-right */
    L,
    /** Right-to-left */
    R,
    /** Right-to-left Arabic */
    AL,
    /** European number */
    EN,
    /** European number separator */
    ES,
    /** European number terminator */
    ET,
    /** Arabic number */
    AN,
    /** Common number separator */
    CS,
    /** Nonspacing mark */
    NSM,
    /** Boundary neutral */
    BN,
    /** Paragraph separator */
    B,
    /** Segment separator */
    S,
    /** Whitespace */
    WS,
    /** Other neutrals */
    ON,
    /** Left-to-right embedding */
    LRE,
    /** Left-to-right override */
    LRO,
    /** Right-to-left embedding */
    RLE,
    /** Right-to-left override */
    RLO,
    /** Pop directional format */
    PDF,
    /** Left-to-right isolate */
    LRI,
    /** Right-to-left isolate */
    RLI,
    /** First strong isolate */
    FSI,
    /** Pop directional isolate */
    PDI,
}

/**
 * Converts an embedding level to its direction, even levels are left-to-right, odd levels are right-to-left.
 */
fun bidiLevelDirection(level: Int): Direction =
    if (level and 1 == 0) Direction.LEFT_TO_RIGHT else Direction.RIGHT_TO_LEFT

/**
 * A paragraph in a [BidiText].
 *
 * @property start the index of the first UTF-16 unit of the paragraph
 * @property end the index after the last UTF-16 unit of the paragraph, the paragraph separator is included
 * @property level the paragraph embedding level, 0 for left-to-right paragraphs and 1 for right-to-left paragraphs
 */
@JvmRecord
data class BidiParagraph(val start: Int, val end: Int, val level: Int) {
    val direction: Direction get() = bidiLevelDirection(level)
}

/**
 * A run of text with a single embedding level and a single script.
 *
 * Runs are the units of text that can be passed to [TextShapingDriver.shape], using [direction] and [script].
 *
 * @property start the index of the first UTF-16 unit of the run
 * @property end the index after the last UTF-16 unit of the run
 * @property level the embedding level of the run
 * @property script the resolved script of the run
 */
@JvmRecord
data class BidiRun(val start: Int, val end: Int, val level: Int, val script: Script) {
    val direction: Direction get() = bidiLevelDirection(level)
    val length: Int get() = end - start
}

/**
 * The result of the bidirectional analysis of a text.
 *
 * The analysis resolves embedding levels for whole paragraphs, reordering happens per line. A typical use is:
 *  1. shape every run returned by [runs] using its direction and script
 *  2. break lines on the shaped runs (in logical order)
 *  3. call [visualRuns] for every line to find the order in which the line's runs are displayed
 *
 * All indices are UTF-16 indices into [text], line ranges are half-open (`lineEnd` is exclusive).
 */
interface BidiText {
    /** The analyzed text */
    val text: String

    /** The paragraphs in [text], in logical order */
    val paragraphs: List<BidiParagraph>

    /**
     * The resolved embedding levels per UTF-16 unit, before applying the line based rule L1.
     * Both units of a surrogate pair have the same level. The array must not be modified.
     */
    val levels: ByteArray

    /**
     * Returns the resolved script of the character at UTF-16 [index]. Characters of the Common and Inherited scripts
     * are resolved to the script of the surrounding text, they are only reported as [Script.COMMON] or
     * [Script.INHERITED] when the paragraph has no other script.
     */
    fun scriptAt(index: Int): Script

    /** Returns the paragraph containing the UTF-16 [index] */
    fun paragraphAt(index: Int): BidiParagraph

    /**
     * Returns the runs of uniform embedding level and script in `[start, end)` in logical order.
     * Runs never cross paragraph boundaries.
     */
    fun runs(start: Int = 0, end: Int = text.length): List<BidiRun>

    /**
     * Returns the embedding levels for the line `[lineStart, lineEnd)` after applying rule L1: segment separators,
     * paragraph separators and trailing whitespace are reset to the paragraph level.
     */
    fun lineLevels(lineStart: Int, lineEnd: Int): ByteArray

    /**
     * Returns the visual order of the line `[lineStart, lineEnd)` (rules L1 and L2): element `i` is the UTF-16 index
     * of the unit displayed at visual position `i`, from left to right. Surrogate pairs are kept in logical order.
     * Characters are not mirrored, see [BidiTextDriver.mirroredCodePoint].
     */
    fun visualOrder(lineStart: Int, lineEnd: Int): IntArray

    /**
     * Returns the runs of the line `[lineStart, lineEnd)` in visual order, from left to right, with levels after rule
     * L1. The characters of a run with a right-to-left [BidiRun.direction] are displayed right to left; shaping such a
     * run with [Direction.RIGHT_TO_LEFT] produces glyphs in visual order.
     */
    fun visualRuns(lineStart: Int, lineEnd: Int): List<BidiRun>
}

/**
 * Interface for bidirectional text analysis according to the Unicode Bidirectional Algorithm (UAX #9) and script
 * itemization.
 */
interface BidiTextDriver {

    /**
     * Analyzes [text].
     *
     * @param text the text to analyze, it may contain multiple paragraphs
     * @param direction the paragraph direction, either [Direction.LEFT_TO_RIGHT] or [Direction.RIGHT_TO_LEFT].
     * When null the direction of every paragraph is determined from its first strong character (rules P2 and P3).
     */
    fun analyze(text: String, direction: Direction? = null): BidiText

    /** Returns the bidirectional character type of [codePoint] */
    fun bidiClass(codePoint: Int): BidiClass

    /** Returns the script of [codePoint] as defined in the Unicode Scripts property, without resolving */
    fun script(codePoint: Int): Script

    /** Returns the mirrored code point of [codePoint] (Bidi_Mirroring_Glyph) or null when it has none */
    fun mirroredCodePoint(codePoint: Int): Int?

    companion object {
        var driver: BidiTextDriver? = null
        val instance: BidiTextDriver
            get() {
                return driver ?: error("BidiTextDriver not initialized")
            }
    }
}
