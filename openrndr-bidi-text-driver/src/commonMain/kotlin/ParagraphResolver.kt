package org.openrndr.biditextdriver

// Bidi classes, ordinals of org.openrndr.draw.font.internal.BidiClass
internal const val L: Byte = 0
internal const val R: Byte = 1
internal const val AL: Byte = 2
internal const val EN: Byte = 3
internal const val ES: Byte = 4
internal const val ET: Byte = 5
internal const val AN: Byte = 6
internal const val CS: Byte = 7
internal const val NSM: Byte = 8
internal const val BN: Byte = 9
internal const val B: Byte = 10
internal const val S: Byte = 11
internal const val WS: Byte = 12
internal const val ON: Byte = 13
internal const val LRE: Byte = 14
internal const val LRO: Byte = 15
internal const val RLE: Byte = 16
internal const val RLO: Byte = 17
internal const val PDF: Byte = 18
internal const val LRI: Byte = 19
internal const val RLI: Byte = 20
internal const val FSI: Byte = 21
internal const val PDI: Byte = 22

/** Maximum explicit embedding level (BD2) */
private const val MAX_DEPTH = 125

/** Maximum depth of the bracket pair stack (BD16) */
private const val MAX_PAIRING_DEPTH = 63

internal fun isIsolateInitiator(t: Byte) = t == LRI || t == RLI || t == FSI

internal fun isRemovedByX9(t: Byte) = t == LRE || t == RLE || t == LRO || t == RLO || t == PDF || t == BN

/** Characters that are reset to the paragraph level by rule L1 when they precede a separator or the end of a line */
internal fun isWhitespaceForL1(t: Byte) =
    t == WS || t == BN || t == LRE || t == RLE || t == LRO || t == RLO || t == PDF || isIsolateInitiator(t) || t == PDI

private fun typeForLevel(level: Int): Byte = if (level and 1 == 0) L else R

/**
 * Resolves the embedding levels of a single paragraph following the Unicode Bidirectional Algorithm (UAX #9),
 * rules P2 through I2. The line based rules L1 and L2 are applied by [BidiTextImpl].
 *
 * @param codePoints the code points of the paragraph, used to identify bracket pairs
 * @param initialTypes the bidi classes of the code points
 * @param requestedLevel the paragraph embedding level (0 or 1) or -1 to determine it from the text (P2, P3)
 */
internal class ParagraphResolver(
    private val codePoints: IntArray,
    private val initialTypes: ByteArray,
    requestedLevel: Int
) {
    private val length = initialTypes.size
    private val resultTypes = initialTypes.copyOf()

    /** The resolved embedding levels, per code point */
    val levels = ByteArray(length)

    /** Index of the matching PDI for isolate initiators, [length] when there is no match */
    private val matchingPDI = IntArray(length) { length }

    /** Index of the matching isolate initiator for PDIs, -1 when there is no match */
    private val matchingIsolateInitiator = IntArray(length) { -1 }

    /** The resolved paragraph embedding level */
    val paragraphLevel: Int

    init {
        determineMatchingIsolates()
        paragraphLevel = if (requestedLevel >= 0) requestedLevel else determineParagraphEmbeddingLevel(0, length)
        determineExplicitEmbeddingLevels()
        for (sequence in determineIsolatingRunSequences(determineLevelRuns())) {
            sequence.resolve()
        }
        assignLevelsToCharactersRemovedByX9()
    }

    /** BD9: match isolate initiators with PDIs */
    private fun determineMatchingIsolates() {
        for (i in 0 until length) {
            if (isIsolateInitiator(initialTypes[i])) {
                var depth = 1
                for (j in i + 1 until length) {
                    val t = initialTypes[j]
                    if (isIsolateInitiator(t)) {
                        depth++
                    } else if (t == PDI) {
                        depth--
                        if (depth == 0) {
                            matchingPDI[i] = j
                            matchingIsolateInitiator[j] = i
                            break
                        }
                    }
                }
            }
        }
    }

    /**
     * P2, P3: find the first strong character in `[start, end)`, skipping isolated content
     * @return 1 when the first strong character is R or AL, 0 otherwise
     */
    private fun determineParagraphEmbeddingLevel(start: Int, end: Int): Int {
        var i = start
        while (i < end) {
            when (initialTypes[i]) {
                L -> return 0
                R, AL -> return 1
                LRI, RLI, FSI -> i = matchingPDI[i]
            }
            i++
        }
        return 0
    }

    /** X1 - X8 */
    private fun determineExplicitEmbeddingLevels() {
        val stackLevels = IntArray(MAX_DEPTH + 2)
        val stackOverrides = ByteArray(MAX_DEPTH + 2)
        val stackIsolates = BooleanArray(MAX_DEPTH + 2)
        var depth = 0

        fun push(level: Int, override: Byte, isolate: Boolean) {
            stackLevels[depth] = level
            stackOverrides[depth] = override
            stackIsolates[depth] = isolate
            depth++
        }

        push(paragraphLevel, ON, false)

        var overflowIsolateCount = 0
        var overflowEmbeddingCount = 0
        var validIsolateCount = 0

        for (i in 0 until length) {
            when (val t = resultTypes[i]) {
                RLE, LRE, RLO, LRO, RLI, LRI, FSI -> {
                    val isIsolate = isIsolateInitiator(t)
                    val isRtl = when (t) {
                        RLE, RLO, RLI -> true
                        FSI -> determineParagraphEmbeddingLevel(i + 1, matchingPDI[i]) == 1
                        else -> false
                    }
                    if (isIsolate) {
                        levels[i] = stackLevels[depth - 1].toByte()
                        if (stackOverrides[depth - 1] != ON) {
                            resultTypes[i] = stackOverrides[depth - 1]
                        }
                    }
                    val lastLevel = stackLevels[depth - 1]
                    val newLevel = if (isRtl) (lastLevel + 1) or 1 else (lastLevel + 2) and 1.inv()

                    if (newLevel <= MAX_DEPTH && overflowIsolateCount == 0 && overflowEmbeddingCount == 0) {
                        if (isIsolate) {
                            validIsolateCount++
                        }
                        push(
                            newLevel, when (t) {
                                LRO -> L
                                RLO -> R
                                else -> ON
                            }, isIsolate
                        )
                        // not part of the specification, embedding characters are removed by X9 and get their level later
                        if (!isIsolate) {
                            levels[i] = newLevel.toByte()
                        }
                    } else {
                        if (isIsolate) {
                            overflowIsolateCount++
                        } else if (overflowIsolateCount == 0) {
                            overflowEmbeddingCount++
                        }
                    }
                }

                PDI -> {
                    if (overflowIsolateCount > 0) {
                        overflowIsolateCount--
                    } else if (validIsolateCount > 0) {
                        overflowEmbeddingCount = 0
                        while (!stackIsolates[depth - 1]) {
                            depth--
                        }
                        depth--
                        validIsolateCount--
                    }
                    levels[i] = stackLevels[depth - 1].toByte()
                    if (stackOverrides[depth - 1] != ON) {
                        resultTypes[i] = stackOverrides[depth - 1]
                    }
                }

                PDF -> {
                    levels[i] = stackLevels[depth - 1].toByte()
                    if (overflowIsolateCount > 0) {
                        // do nothing
                    } else if (overflowEmbeddingCount > 0) {
                        overflowEmbeddingCount--
                    } else if (!stackIsolates[depth - 1] && depth >= 2) {
                        depth--
                    }
                }

                B -> {
                    // X8, a paragraph separator can only be the last character of a paragraph
                    levels[i] = paragraphLevel.toByte()
                }

                else -> {
                    levels[i] = stackLevels[depth - 1].toByte()
                    if (stackOverrides[depth - 1] != ON) {
                        resultTypes[i] = stackOverrides[depth - 1]
                    }
                }
            }
        }
    }

    /** BD7: maximal runs of characters with the same level, ignoring characters removed by X9 */
    private fun determineLevelRuns(): List<IntArray> {
        val runs = mutableListOf<IntArray>()
        val run = IntArray(length)
        var runLength = 0
        var currentLevel = -1
        for (i in 0 until length) {
            if (!isRemovedByX9(initialTypes[i])) {
                if (levels[i].toInt() != currentLevel) {
                    if (runLength > 0) {
                        runs.add(run.copyOf(runLength))
                        runLength = 0
                    }
                    currentLevel = levels[i].toInt()
                }
                run[runLength++] = i
            }
        }
        if (runLength > 0) {
            runs.add(run.copyOf(runLength))
        }
        return runs
    }

    /** BD13, X10: join level runs connected by matching isolate initiators and PDIs */
    private fun determineIsolatingRunSequences(levelRuns: List<IntArray>): List<IsolatingRunSequence> {
        val runForCharacter = IntArray(length)
        for ((runIndex, run) in levelRuns.withIndex()) {
            for (i in run) {
                runForCharacter[i] = runIndex
            }
        }

        val sequences = mutableListOf<IsolatingRunSequence>()
        val sequence = IntArray(length)
        for (run in levelRuns) {
            val first = run[0]
            if (initialTypes[first] != PDI || matchingIsolateInitiator[first] == -1) {
                var sequenceLength = 0
                var currentRun = run
                while (true) {
                    currentRun.copyInto(sequence, sequenceLength)
                    sequenceLength += currentRun.size

                    val last = sequence[sequenceLength - 1]
                    if (isIsolateInitiator(initialTypes[last]) && matchingPDI[last] != length) {
                        currentRun = levelRuns[runForCharacter[matchingPDI[last]]]
                    } else {
                        break
                    }
                }
                sequences.add(IsolatingRunSequence(sequence.copyOf(sequenceLength)))
            }
        }
        return sequences
    }

    /** Characters removed by X9 take the level of the preceding character (implementation notes 5.2) */
    private fun assignLevelsToCharactersRemovedByX9() {
        for (i in 0 until length) {
            if (isRemovedByX9(initialTypes[i])) {
                levels[i] = if (i == 0) paragraphLevel.toByte() else levels[i - 1]
            }
        }
    }

    private inner class IsolatingRunSequence(private val indexes: IntArray) {
        private val count = indexes.size
        private val types = ByteArray(count) { resultTypes[indexes[it]] }
        private val level = levels[indexes[0]].toInt()
        private val embeddingDirection = typeForLevel(level)
        private val sos: Byte
        private val eos: Byte

        init {
            var previous = indexes[0] - 1
            while (previous >= 0 && isRemovedByX9(initialTypes[previous])) {
                previous--
            }
            val previousLevel = if (previous >= 0) levels[previous].toInt() else paragraphLevel
            sos = typeForLevel(maxOf(previousLevel, level))

            val last = indexes[count - 1]
            val successorLevel = if (isIsolateInitiator(initialTypes[last])) {
                paragraphLevel
            } else {
                var next = last + 1
                while (next < length && isRemovedByX9(initialTypes[next])) {
                    next++
                }
                if (next < length) levels[next].toInt() else paragraphLevel
            }
            eos = typeForLevel(maxOf(successorLevel, level))
        }

        fun resolve() {
            resolveWeakTypes()
            resolvePairedBrackets()
            resolveNeutralTypes()
            resolveImplicitLevels()
        }

        /** Returns the end of the run of types in [set] starting at [start] */
        private inline fun runLimit(start: Int, set: (Byte) -> Boolean): Int {
            var i = start
            while (i < count && set(types[i])) {
                i++
            }
            return i
        }

        /** W1 - W7 */
        private fun resolveWeakTypes() {
            // W1
            var preceding = sos
            for (i in 0 until count) {
                val t = types[i]
                if (t == NSM) {
                    types[i] = preceding
                } else {
                    preceding = if (isIsolateInitiator(t) || t == PDI) ON else t
                }
            }

            // W2
            var lastStrong = sos
            for (i in 0 until count) {
                when (types[i]) {
                    L, R, AL -> lastStrong = types[i]
                    EN -> if (lastStrong == AL) types[i] = AN
                }
            }

            // W3
            for (i in 0 until count) {
                if (types[i] == AL) types[i] = R
            }

            // W4
            for (i in 1 until count - 1) {
                val t = types[i]
                val previous = types[i - 1]
                val next = types[i + 1]
                if (t == ES && previous == EN && next == EN) {
                    types[i] = EN
                } else if (t == CS && previous == EN && next == EN) {
                    types[i] = EN
                } else if (t == CS && previous == AN && next == AN) {
                    types[i] = AN
                }
            }

            // W5
            var i = 0
            while (i < count) {
                if (types[i] == ET) {
                    val limit = runLimit(i) { it == ET }
                    var t = if (i == 0) sos else types[i - 1]
                    if (t != EN) {
                        t = if (limit == count) eos else types[limit]
                    }
                    if (t == EN) {
                        types.fill(EN, i, limit)
                    }
                    i = limit
                } else {
                    i++
                }
            }

            // W6
            for (j in 0 until count) {
                val t = types[j]
                if (t == ES || t == ET || t == CS) types[j] = ON
            }

            // W7
            lastStrong = sos
            for (j in 0 until count) {
                when (types[j]) {
                    L, R -> lastStrong = types[j]
                    EN -> if (lastStrong == L) types[j] = L
                }
            }
        }

        /** Returns the strong direction of a resolved type for N0, EN and AN are treated as R */
        private fun strongDirection(t: Byte): Byte = when (t) {
            L -> L
            R, EN, AN -> R
            else -> ON
        }

        /** BD16 and N0 */
        private fun resolvePairedBrackets() {
            // BD16, locate bracket pairs
            val openerClosing = IntArray(MAX_PAIRING_DEPTH)
            val openerPosition = IntArray(MAX_PAIRING_DEPTH)
            var openerCount = 0
            val pairs = mutableListOf<Long>()

            loop@ for (i in 0 until count) {
                if (types[i] != ON) continue
                val codePoint = codePoints[indexes[i]]
                val closing = UnicodeData.closingBracketFor(codePoint)
                if (closing != -1) {
                    if (openerCount == MAX_PAIRING_DEPTH) {
                        break@loop
                    }
                    openerClosing[openerCount] = closing
                    openerPosition[openerCount] = i
                    openerCount++
                } else {
                    val normalized = UnicodeData.closingBracket(codePoint)
                    if (normalized != -1) {
                        for (s in openerCount - 1 downTo 0) {
                            if (openerClosing[s] == normalized) {
                                pairs.add((openerPosition[s].toLong() shl 32) or i.toLong())
                                openerCount = s
                                break
                            }
                        }
                    }
                }
            }
            if (pairs.isEmpty()) return
            pairs.sort()

            // N0
            for (pair in pairs) {
                val open = (pair ushr 32).toInt()
                val close = (pair and 0xffff_ffffL).toInt()

                var foundEmbedding = false
                var foundOpposite = false
                for (i in open + 1 until close) {
                    val d = strongDirection(types[i])
                    if (d == embeddingDirection) {
                        foundEmbedding = true
                        break
                    } else if (d != ON) {
                        foundOpposite = true
                    }
                }

                val direction = if (foundEmbedding) {
                    embeddingDirection
                } else if (foundOpposite) {
                    var preceding = sos
                    for (i in open - 1 downTo 0) {
                        val d = strongDirection(types[i])
                        if (d != ON) {
                            preceding = d
                            break
                        }
                    }
                    if (preceding != embeddingDirection) preceding else embeddingDirection
                } else {
                    continue
                }
                setBracketType(open, direction)
                setBracketType(close, direction)
            }
        }

        /** Sets the type of a bracket and of the original NSMs following it */
        private fun setBracketType(position: Int, direction: Byte) {
            types[position] = direction
            for (i in position + 1 until count) {
                if (initialTypes[indexes[i]] == NSM) {
                    types[i] = direction
                } else {
                    break
                }
            }
        }

        private fun isNeutralOrIsolate(t: Byte) =
            t == B || t == S || t == WS || t == ON || isIsolateInitiator(t) || t == PDI

        /** N1, N2 */
        private fun resolveNeutralTypes() {
            var i = 0
            while (i < count) {
                if (isNeutralOrIsolate(types[i])) {
                    val limit = runLimit(i, ::isNeutralOrIsolate)
                    val leading = if (i == 0) sos else when (val t = types[i - 1]) {
                        AN, EN -> R
                        else -> t
                    }
                    val trailing = if (limit == count) eos else when (val t = types[limit]) {
                        AN, EN -> R
                        else -> t
                    }
                    val resolved = if (leading == trailing) leading else embeddingDirection
                    types.fill(resolved, i, limit)
                    i = limit
                } else {
                    i++
                }
            }
        }

        /** I1, I2 */
        private fun resolveImplicitLevels() {
            val even = level and 1 == 0
            for (i in 0 until count) {
                val t = types[i]
                val resolved = if (even) {
                    when (t) {
                        R -> level + 1
                        AN, EN -> level + 2
                        else -> level
                    }
                } else {
                    when (t) {
                        L, EN, AN -> level + 1
                        else -> level
                    }
                }
                levels[indexes[i]] = resolved.toByte()
                resultTypes[indexes[i]] = t
            }
        }
    }
}
