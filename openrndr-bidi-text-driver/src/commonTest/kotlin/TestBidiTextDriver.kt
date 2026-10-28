import org.openrndr.biditextdriver.BidiTextDriverUax9
import org.openrndr.draw.font.internal.BidiClass
import org.openrndr.draw.font.internal.BidiRun
import org.openrndr.draw.font.internal.Direction
import org.openrndr.draw.font.internal.Script
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class TestBidiTextDriver {
    private val driver = BidiTextDriverUax9()

    @Test
    fun characterProperties() {
        assertEquals(BidiClass.L, driver.bidiClass('a'.code))
        assertEquals(BidiClass.R, driver.bidiClass(0x05D0))
        assertEquals(BidiClass.AL, driver.bidiClass(0x0627))
        assertEquals(BidiClass.AN, driver.bidiClass(0x0662))
        assertEquals(BidiClass.EN, driver.bidiClass('2'.code))
        assertEquals(BidiClass.BN, driver.bidiClass(0x200D))
        assertEquals(BidiClass.FSI, driver.bidiClass(0x2068))
        // unassigned code point in the Hebrew block defaults to R
        assertEquals(BidiClass.R, driver.bidiClass(0x05FF))

        assertEquals(Script.LATIN, driver.script('a'.code))
        assertEquals(Script.ARABIC, driver.script(0x0627))
        assertEquals(Script.COMMON, driver.script(' '.code))
        assertEquals(Script.INHERITED, driver.script(0x0301))

        assertEquals(')'.code, driver.mirroredCodePoint('('.code))
        assertEquals(0x00AB, driver.mirroredCodePoint(0x00BB))
        assertNull(driver.mirroredCodePoint('a'.code))
    }

    @Test
    fun emptyText() {
        val bidi = driver.analyze("", Direction.RIGHT_TO_LEFT)
        assertEquals(1, bidi.paragraphs.size)
        assertEquals(1, bidi.paragraphs[0].level)
        assertEquals(emptyList(), bidi.runs())
        assertContentEquals(IntArray(0), bidi.visualOrder(0, 0))
    }

    @Test
    fun paragraphDirectionFromText() {
        assertEquals(Direction.LEFT_TO_RIGHT, driver.analyze("abc").paragraphs[0].direction)
        assertEquals(Direction.RIGHT_TO_LEFT, driver.analyze("123 אב").paragraphs[0].direction)
        assertEquals(Direction.LEFT_TO_RIGHT, driver.analyze("123").paragraphs[0].direction)
        assertFailsWith<IllegalArgumentException> { driver.analyze("abc", Direction.TOP_TO_BOTTOM) }
    }

    @Test
    fun paragraphs() {
        val text = "abc\r\nאב def"
        val bidi = driver.analyze(text)
        assertEquals(3, bidi.paragraphs.size)
        assertEquals(0 until 5, bidi.paragraphs[0].start until bidi.paragraphs[0].end)
        assertEquals(1, bidi.paragraphs[1].level)
        assertEquals(5, bidi.paragraphs[1].start)
        assertEquals(8, bidi.paragraphs[1].end)
        assertEquals(0, bidi.paragraphs[2].level)
        assertEquals(bidi.paragraphs[1], bidi.paragraphAt(6))
        assertEquals(bidi.paragraphs[2], bidi.paragraphAt(text.length))
    }

    @Test
    fun arabicDigitsAreLeftToRight() {
        // "سنة ٢٠٢٦"
        val text = "سنة ٢٠٢٦"
        val bidi = driver.analyze(text)
        assertEquals(1, bidi.paragraphs[0].level)
        val runs = bidi.runs()
        assertEquals(
            listOf(
                BidiRun(0, 4, 1, Script.ARABIC),
                BidiRun(4, 8, 2, Script.ARABIC),
            ), runs
        )
        assertEquals(Direction.LEFT_TO_RIGHT, runs[1].direction)
        // the number is displayed left of the word, its digits left to right
        assertContentEquals(intArrayOf(4, 5, 6, 7, 3, 2, 1, 0), bidi.visualOrder(0, text.length))
    }

    @Test
    fun mixedScriptsAndLevels() {
        // "abc אבג def" in a left-to-right paragraph
        val text = "abc אבג def"
        val bidi = driver.analyze(text, Direction.LEFT_TO_RIGHT)
        assertEquals(
            listOf(
                BidiRun(0, 4, 0, Script.LATIN),
                BidiRun(4, 7, 1, Script.HEBREW),
                // the space takes the script of the preceding text
                BidiRun(7, 8, 0, Script.HEBREW),
                BidiRun(8, 11, 0, Script.LATIN),
            ), bidi.runs()
        )
        assertEquals(Script.LATIN, bidi.scriptAt(3))
        assertContentEquals(intArrayOf(0, 1, 2, 3, 6, 5, 4, 7, 8, 9, 10), bidi.visualOrder(0, text.length))
    }

    @Test
    fun visualRunsPerLine() {
        // two lines: "abc אבג " and "def גדה"
        val text = "abc אבג def גדה"
        val bidi = driver.analyze(text, Direction.RIGHT_TO_LEFT)

        val first = bidi.visualRuns(0, 8)
        // the space between "abc" and the Hebrew resolves to the paragraph direction, the trailing space is reset
        // to the paragraph level by L1
        assertEquals(listOf(4 until 8, 3 until 4, 0 until 3), first.map { it.start until it.end })
        assertEquals(listOf(1, 1, 2), first.map { it.level })
        assertEquals(listOf(Script.HEBREW, Script.LATIN, Script.LATIN), first.map { it.script })

        val second = bidi.visualRuns(8, text.length)
        assertEquals(listOf(12 until 15, 11 until 12, 8 until 11), second.map { it.start until it.end })
        assertEquals(listOf(1, 1, 2), second.map { it.level })
    }

    @Test
    fun lineLevelsResetTrailingWhitespace() {
        val text = "אב abc  "
        val bidi = driver.analyze(text, Direction.LEFT_TO_RIGHT)
        assertEquals(listOf<Byte>(1, 1, 0, 0, 0, 0, 0, 0), bidi.levels.toList())
        val rtl = driver.analyze(text, Direction.RIGHT_TO_LEFT)
        assertEquals(listOf<Byte>(1, 1, 1, 2, 2, 2, 1, 1), rtl.lineLevels(0, text.length).toList())
    }

    @Test
    fun bracketsTakeScriptAndDirection() {
        // "אב (גד) ef": the brackets enclose Hebrew so they resolve to R and take the Hebrew script
        val text = "אב (גד) ef"
        val bidi = driver.analyze(text, Direction.LEFT_TO_RIGHT)
        assertEquals(Script.HEBREW, bidi.scriptAt(3))
        assertEquals(Script.HEBREW, bidi.scriptAt(6))
        assertEquals(1, bidi.levels[3].toInt())
        assertEquals(1, bidi.levels[6].toInt())

        // a closing bracket takes the script of its opening bracket
        val mixed = driver.analyze("a(א)")
        assertEquals(Script.LATIN, mixed.scriptAt(3))
    }

    @Test
    fun leadingCommonTakesFollowingScript() {
        val bidi = driver.analyze("\"123 ال")
        assertEquals(Script.ARABIC, bidi.scriptAt(0))
        assertEquals(Script.ARABIC, bidi.scriptAt(1))
        val common = driver.analyze("123 !")
        assertEquals(Script.COMMON, common.scriptAt(0))
    }

    @Test
    fun surrogatePairsStayInLogicalOrder() {
        // Adlam (right-to-left, supplementary plane) followed by Latin
        val adlam = "𞤀𞤁"
        val text = "$adlam ab"
        val bidi = driver.analyze(text, Direction.LEFT_TO_RIGHT)
        assertEquals(Script.ADLAM, bidi.scriptAt(0))
        assertEquals(1, bidi.levels[0].toInt())
        assertEquals(1, bidi.levels[1].toInt())
        assertContentEquals(intArrayOf(2, 3, 0, 1, 4, 5, 6), bidi.visualOrder(0, text.length))
    }

    @Test
    fun explicitIsolates() {
        // RLI אב PDI inside Latin text
        val text = "a ⁧אב⁩ b"
        val bidi = driver.analyze(text)
        assertEquals(listOf<Byte>(0, 0, 0, 1, 1, 0, 0, 0), bidi.levels.toList())
    }
}
