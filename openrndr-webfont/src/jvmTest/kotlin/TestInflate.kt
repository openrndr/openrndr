import org.openrndr.webfont.inflate
import org.openrndr.webfont.zlibDecompress
import java.util.zip.Deflater
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals

/**
 * The zlib decoder of WOFF decodes what java.util.zip compresses: stored, fixed and dynamic blocks
 */
class TestInflate {
    private fun compress(data: ByteArray, level: Int, nowrap: Boolean = false): ByteArray {
        val deflater = Deflater(level, nowrap)
        deflater.setInput(data)
        deflater.finish()
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(4096)
        while (!deflater.finished()) out.write(buffer, 0, deflater.deflate(buffer))
        deflater.end()
        return out.toByteArray()
    }

    private val samples = listOf(
        ByteArray(0),
        "a".toByteArray(),
        "the quick brown fox jumps over the lazy dog ".repeat(200).toByteArray(),
        Random(1).nextBytes(70000),
        ByteArray(100000) { (it % 7).toByte() }
    )

    @Test
    fun decodesZlibStreams() {
        for (data in samples) for (level in listOf(0, 1, 6, 9)) {
            val compressed = compress(data, level)
            assertContentEquals(data, zlibDecompress(compressed, 0, compressed.size, data.size), "level $level, ${data.size} bytes")
        }
    }

    @Test
    fun decodesRawDeflateData() {
        for (data in samples) {
            val compressed = compress(data, 9, nowrap = true)
            assertContentEquals(data, inflate(compressed, 0, compressed.size, 0))
        }
    }
}
