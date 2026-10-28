import com.sun.net.httpserver.HttpServer
import org.openrndr.draw.font.internal.fontBytesFromFileOrUrl
import java.net.InetSocketAddress
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class TestFontBytesCache {

    @Test
    fun testDownloadsOnceAndReadsFromDisk() {
        val payload = ByteArray(1024) { it.toByte() }
        var requests = 0
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/font.ttf") { exchange ->
            requests++
            exchange.sendResponseHeaders(200, payload.size.toLong())
            exchange.responseBody.use { it.write(payload) }
        }
        server.start()
        val cacheDirectory = Files.createTempDirectory("fontcache").toFile()
        try {
            val url = "http://127.0.0.1:${server.address.port}/font.ttf"

            assertContentEquals(payload, fontBytesFromFileOrUrl(url, cacheDirectory))
            assertEquals(1, requests)
            assertEquals(1, cacheDirectory.listFiles()!!.count { it.extension == "ttf" })

            assertContentEquals(payload, fontBytesFromFileOrUrl(url, cacheDirectory))
            assertEquals(1, requests)
        } finally {
            server.stop(0)
            cacheDirectory.deleteRecursively()
        }
    }
}
