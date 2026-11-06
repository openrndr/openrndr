import org.openrndr.draw.font.internal.cacheKey
import org.openrndr.draw.font.internal.sha256Hex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class TestFontCacheKeys {
    @Test
    fun sha256MatchesTheStandardVectors() {
        // FIPS 180-4 examples, and a message that spans two blocks with the padding in the second
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", sha256Hex(ByteArray(0)))
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", sha256Hex("abc".encodeToByteArray()))
        assertEquals(
            "248d6a61d20638b8e5c026930c3e6039a33ce45964ff2167f6ecedd419db06c1",
            sha256Hex("abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq".encodeToByteArray())
        )
        assertEquals("cdc76e5c9914fb9281a1c7e284d73e67f1809a48a497200e046d39ccc7112cd0", sha256Hex(ByteArray(1_000_000) { 'a'.code.toByte() }))
    }

    @Test
    fun dataUrlsAreKeyedByTheirDigest() {
        val url = "data:font/woff2;base64," + "QUJD".repeat(10000)
        val key = cacheKey(url)
        assertEquals("data:sha-256:".length + 64, key.length)
        assertEquals(key, cacheKey(url))
        assertNotEquals(key, cacheKey(url + "QUJD"))
        assertEquals("fonts/a.ttf", cacheKey("fonts/a.ttf"))
    }
}
