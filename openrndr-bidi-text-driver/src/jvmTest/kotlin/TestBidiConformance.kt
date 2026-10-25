import org.openrndr.biditextdriver.BidiTextImpl
import org.openrndr.biditextdriver.ParagraphResolver
import org.openrndr.biditextdriver.analyzeText
import org.openrndr.biditextdriver.isRemovedByX9
import org.openrndr.draw.font.internal.BidiClass
import org.openrndr.draw.font.internal.BidiParagraph
import org.openrndr.draw.font.internal.Script
import java.util.zip.GZIPInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Runs the conformance tests from the Unicode Character Database against the implementation.
 */
class TestBidiConformance {

    private fun lines(resource: String): Sequence<String>? {
        val stream = javaClass.getResourceAsStream(resource) ?: return null
        return GZIPInputStream(stream).bufferedReader().readLines().asSequence()
    }

    private fun levelsToString(levels: List<Int?>) = levels.joinToString(" ") { it?.toString() ?: "x" }

    @Test
    fun bidiCharacterTest() {
        val lines = lines("/BidiCharacterTest.txt.gz") ?: error("BidiCharacterTest.txt.gz not found")
        var cases = 0
        val failures = mutableListOf<String>()
        for (line in lines) {
            if (line.isBlank() || line.startsWith("#")) continue
            val fields = line.split(";")
            val codePoints = fields[0].trim().split(" ").map { it.toInt(16) }
            val requestedLevel = when (fields[1].trim()) {
                "0" -> 0
                "1" -> 1
                else -> -1
            }
            val expectedParagraphLevel = fields[2].trim().toInt()
            val expectedLevels = fields[3].trim()
            val expectedOrder = fields[4].trim()

            val text = buildString { codePoints.forEach { appendCodePoint(it) } }
            val bidi = analyzeText(text, requestedLevel)

            // map UTF-16 indices to code point indices
            val utf16ToCodePoint = IntArray(text.length)
            var cp = 0
            var u = 0
            while (u < text.length) {
                val size = Character.charCount(text.codePointAt(u))
                repeat(size) { utf16ToCodePoint[u + it] = cp }
                u += size
                cp++
            }
            val codePointStarts = IntArray(codePoints.size)
            for (i in text.indices.reversed()) codePointStarts[utf16ToCodePoint[i]] = i

            val removed = codePoints.map { isRemovedByX9(bidiClassOrdinal(it)) }
            val lineLevels = bidi.lineLevels(0, text.length)
            val levels = codePoints.indices.map { if (removed[it]) null else lineLevels[codePointStarts[it]].toInt() }
            val order = bidi.visualOrder(0, text.length)
                .filter { !text[it].isLowSurrogate() }
                .map { utf16ToCodePoint[it] }
                .filter { !removed[it] }
                .joinToString(" ")

            cases++
            val paragraphLevel = bidi.paragraphs.single().level
            if (paragraphLevel != expectedParagraphLevel || levelsToString(levels) != expectedLevels || order != expectedOrder) {
                failures += "$line\n  got: $paragraphLevel;${levelsToString(levels)};$order"
            }
        }
        assertTrue(cases > 90_000, "expected more test cases, found $cases")
        assertEquals(0, failures.size, "${failures.size} of $cases cases failed:\n" + failures.take(20).joinToString("\n"))
    }

    @Test
    fun bidiTest() {
        val lines = lines("/BidiTest.txt.gz") ?: error("BidiTest.txt.gz not found")
        var expectedLevels = ""
        var expectedOrder = ""
        var cases = 0
        val failures = mutableListOf<String>()
        for (line in lines) {
            if (line.isBlank() || line.startsWith("#")) continue
            if (line.startsWith("@Levels:")) {
                expectedLevels = line.substringAfter(":").trim().split(Regex("\\s+")).joinToString(" ")
                continue
            }
            if (line.startsWith("@Reorder:")) {
                expectedOrder = line.substringAfter(":").trim().split(Regex("\\s+")).joinToString(" ")
                continue
            }
            if (line.startsWith("@")) continue

            val (input, bitset) = line.split(";")
            val types = input.trim().split(Regex("\\s+")).map { BidiClass.valueOf(it).ordinal.toByte() }.toByteArray()
            val bits = bitset.trim().toInt(16)
            for ((bit, requestedLevel) in listOf(1 to -1, 2 to 0, 4 to 1)) {
                if (bits and bit == 0) continue
                cases++
                val resolver = ParagraphResolver(IntArray(types.size), types, requestedLevel)
                val n = types.size
                val bidi = BidiTextImpl(
                    "a".repeat(n),
                    listOf(BidiParagraph(0, n, resolver.paragraphLevel)),
                    resolver.levels,
                    types,
                    Array(n) { Script.COMMON }
                )
                val lineLevels = bidi.lineLevels(0, n)
                val levels = (0 until n).map { if (isRemovedByX9(types[it])) null else lineLevels[it].toInt() }
                val order = bidi.visualOrder(0, n).filter { !isRemovedByX9(types[it]) }.joinToString(" ")
                if (levelsToString(levels) != expectedLevels || order != expectedOrder) {
                    failures += "$line (level $requestedLevel)\n  expected: $expectedLevels;$expectedOrder\n  got: ${levelsToString(levels)};$order"
                }
            }
        }
        assertTrue(cases > 400_000, "expected more test cases, found $cases")
        assertEquals(0, failures.size, "${failures.size} of $cases cases failed:\n" + failures.take(20).joinToString("\n"))
    }

    private fun bidiClassOrdinal(codePoint: Int): Byte =
        org.openrndr.biditextdriver.BidiTextDriverUax9().bidiClass(codePoint).ordinal.toByte()
}
