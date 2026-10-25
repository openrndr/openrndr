package org.openrndr.biditextdriver

import org.openrndr.draw.font.internal.Script

private fun isUnresolvedScript(script: Script) =
    script == Script.COMMON || script == Script.INHERITED || script == Script.UNKNOWN

/**
 * Resolves the script of every code point of a paragraph (UAX #24, section 5.1).
 *
 * - Inherited characters (combining marks) take the script of the preceding character.
 * - Common and unknown characters take the script of the preceding text.
 * - A closing bracket takes the script of its matching opening bracket.
 * - Characters before the first character with a real script take that script.
 *
 * When the paragraph contains no characters with a real script the original Common or Inherited script is kept.
 */
internal fun itemizeScripts(codePoints: IntArray): Array<Script> {
    val count = codePoints.size
    val result = arrayOfNulls<Script>(count)

    // stack of open brackets, the script is null while no real script has been seen
    val stackClosing = IntArray(64)
    val stackScripts = arrayOfNulls<Script>(64)
    var stackSize = 0

    var current: Script? = null

    for (i in 0 until count) {
        val codePoint = codePoints[i]
        val script = UnicodeData.script(codePoint)

        if (!isUnresolvedScript(script)) {
            if (current == null) {
                for (j in 0 until i) {
                    if (result[j] == null) result[j] = script
                }
                for (s in 0 until stackSize) {
                    if (stackScripts[s] == null) stackScripts[s] = script
                }
            }
            current = script
            result[i] = script
            continue
        }

        if (script == Script.INHERITED) {
            result[i] = if (i > 0) result[i - 1] else null
            continue
        }

        val closingFor = UnicodeData.closingBracketFor(codePoint)
        if (closingFor != -1) {
            if (stackSize == stackClosing.size) {
                // drop the outermost bracket
                stackClosing.copyInto(stackClosing, 0, 1)
                stackScripts.copyInto(stackScripts, 0, 1)
                stackSize--
            }
            stackClosing[stackSize] = closingFor
            stackScripts[stackSize] = current
            stackSize++
            result[i] = current
            continue
        }

        val closing = UnicodeData.closingBracket(codePoint)
        if (closing != -1) {
            for (s in stackSize - 1 downTo 0) {
                if (stackClosing[s] == closing) {
                    result[i] = stackScripts[s] ?: current
                    stackSize = s
                    break
                }
            }
            if (result[i] == null) result[i] = current
            continue
        }
        result[i] = current
    }

    return Array(count) { result[it] ?: UnicodeData.script(codePoints[it]).let { s -> if (s == Script.UNKNOWN) Script.COMMON else s } }
}
