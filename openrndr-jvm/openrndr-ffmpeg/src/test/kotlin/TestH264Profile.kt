import org.openrndr.ffmpeg.H264Profile
import org.openrndr.ffmpeg.VideoRotation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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

    @Test
    fun `rotation is applied after flipping`() {
        val profile = H264Profile().apply { rotation = VideoRotation.CLOCKWISE_90 }
        assertEquals("vflip,transpose=clock,colorspace=bt709:iall=bt601-6-625:fast=1", profile.arguments().valueOf("-vf"))
    }

    @Test
    fun `NVENC encodes with a constant quantizer and full chroma`() {
        val profile = H264Profile().apply {
            videoCodec = CODEC_H264_NVENC
            fullChroma = true
        }
        val arguments = profile.arguments()
        assertEquals("h264_nvenc", arguments.valueOf("-vcodec"))
        assertEquals("yuv444p", arguments.valueOf("-pix_fmt"))
        assertEquals("23", arguments.valueOf("-cq"))
        assertEquals("0", arguments.valueOf("-b:v"))
        assertFalse("-crf" in arguments)
    }

    @Test
    fun `VAAPI encodes frames uploaded as NV12`() {
        val profile = H264Profile().apply {
            videoCodec = CODEC_H264_VAAPI
            vaapiDevice = "/dev/dri/renderD129"
            quality = 100
        }
        val arguments = profile.arguments()
        assertEquals("h264_vaapi", arguments.valueOf("-vcodec"))
        assertEquals("/dev/dri/renderD129", arguments.valueOf("-vaapi_device"))
        assertTrue(arguments.valueOf("-vf").endsWith(",format=nv12,hwupload"))
        assertFalse("-pix_fmt" in arguments)
        assertEquals("4", arguments.valueOf("-qp"))
    }

    @Test
    fun `Quick Sync encodes NV12 with intelligent constant quality`() {
        val profile = H264Profile().apply { videoCodec = CODEC_H264_QSV }
        val arguments = profile.arguments()
        assertEquals("h264_qsv", arguments.valueOf("-vcodec"))
        assertEquals("nv12", arguments.valueOf("-pix_fmt"))
        assertEquals("23", arguments.valueOf("-global_quality"))
        assertFalse("-crf" in arguments)
    }

    @Test
    fun `AMF encodes NV12 with a constant quantizer`() {
        val profile = H264Profile().apply { videoCodec = CODEC_H264_AMF }
        val arguments = profile.arguments()
        assertEquals("nv12", arguments.valueOf("-pix_fmt"))
        assertEquals("cqp", arguments.valueOf("-rc"))
        assertEquals(listOf("23", "23", "23"), listOf("-qp_i", "-qp_p", "-qp_b").map { arguments.valueOf(it) })
    }

    @Test
    fun `Media Foundation encodes in hardware`() {
        val profile = H264Profile().apply { videoCodec = CODEC_H264_MF }
        val arguments = profile.arguments()
        assertEquals("nv12", arguments.valueOf("-pix_fmt"))
        assertEquals("1", arguments.valueOf("-hw_encoding"))
        assertEquals("60", arguments.valueOf("-quality"))
    }

    @Test
    fun `Quick Sync full chroma is encoded with libx264`() {
        val profile = H264Profile().apply {
            videoCodec = CODEC_H264_QSV
            fullChroma = true
        }
        val arguments = profile.arguments()
        assertEquals("libx264", arguments.valueOf("-vcodec"))
        assertEquals("yuv444p", arguments.valueOf("-pix_fmt"))
        assertFalse("-global_quality" in arguments)
    }

    @Test
    fun `VAAPI full chroma is encoded with libx264`() {
        val profile = H264Profile().apply {
            videoCodec = CODEC_H264_VAAPI
            fullChroma = true
        }
        val arguments = profile.arguments()
        assertEquals("libx264", arguments.valueOf("-vcodec"))
        assertEquals("yuv444p", arguments.valueOf("-pix_fmt"))
        assertFalse("hwupload" in arguments.valueOf("-vf"))
        assertFalse("-vaapi_device" in arguments)
    }
}
