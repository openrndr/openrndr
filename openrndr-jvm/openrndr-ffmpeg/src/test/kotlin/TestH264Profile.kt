import org.openrndr.ffmpeg.H264Profile
import kotlin.test.Test
import kotlin.test.assertEquals

class TestH264Profile {
    private fun Array<String>.valueOf(option: String) = this[indexOf(option) + 1]

    @Test
    fun `arguments can be requested more than once`() {
        val profile = H264Profile()
        val first = profile.arguments()
        val second = profile.arguments()
        assertEquals(first.toList(), second.toList())
        assertEquals("vflip,colorspace=bt709:iall=bt601-6-625:fast=1", second.valueOf("-vf"))
        assertEquals(listOf("vflip"), profile.filters)
    }

    @Test
    fun `full chroma is encoded with libx264`() {
        val profile = H264Profile().apply {
            videoCodec = CODEC_H264_VIDEOTOOLBOX
            fullChroma = true
        }
        val arguments = profile.arguments()
        assertEquals("libx264", arguments.valueOf("-vcodec"))
        assertEquals("yuv444p", arguments.valueOf("-pix_fmt"))
    }
}
