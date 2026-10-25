package org.openrndr.biditextdriver

import org.openrndr.draw.font.internal.BidiClass
import org.openrndr.draw.font.internal.BidiText
import org.openrndr.draw.font.internal.BidiTextDriver
import org.openrndr.draw.font.internal.Direction
import org.openrndr.draw.font.internal.Script

/**
 * [BidiTextDriver] implementing the Unicode Bidirectional Algorithm (UAX #9) and script itemization (UAX #24) in
 * pure Kotlin, using tables generated from the Unicode Character Database.
 */
class BidiTextDriverUax9 : BidiTextDriver {

    override fun analyze(text: String, direction: Direction?): BidiText {
        val level = when (direction) {
            null -> -1
            Direction.LEFT_TO_RIGHT -> 0
            Direction.RIGHT_TO_LEFT -> 1
            else -> throw IllegalArgumentException("unsupported paragraph direction $direction")
        }
        return analyzeText(text, level)
    }

    override fun bidiClass(codePoint: Int): BidiClass = BidiClass.entries[UnicodeData.bidiClass(codePoint).toInt()]

    override fun script(codePoint: Int): Script = UnicodeData.script(codePoint)

    override fun mirroredCodePoint(codePoint: Int): Int? = UnicodeData.mirror(codePoint)

    companion object {
        /** The version of the Unicode Character Database the tables are generated from */
        const val UNICODE_VERSION: String = org.openrndr.biditextdriver.UNICODE_VERSION
    }
}
