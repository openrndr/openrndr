import org.openrndr.ffmpeg.EncoderInput
import org.openrndr.ffmpeg.HardwareEncoder
import org.openrndr.ffmpeg.findHardwareEncoder
import org.openrndr.platform.Platform
import org.openrndr.platform.PlatformType
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TestHardwareEncoder {
    private fun List<String>.valueOf(option: String) = this[indexOf(option) + 1]

    @Test
    fun `software encoders are not hardware encoders`() {
        assertNull(HardwareEncoder.forCodec("libx264"))
        assertNull(HardwareEncoder.forCodec("libx265"))
        assertFailsWith<IllegalArgumentException> { HardwareEncoder("libx264") }
    }

    @Test
    fun `only VAAPI encoders keep their device`() {
        assertEquals(HardwareEncoder("hevc_vaapi", "/dev/dri/renderD129"), HardwareEncoder.forCodec("hevc_vaapi", "/dev/dri/renderD129"))
        assertEquals(HardwareEncoder("h264_nvenc"), HardwareEncoder.forCodec("h264_nvenc", "/dev/dri/renderD129"))
    }

    @Test
    fun `only NVENC encodes full chroma`() {
        assertTrue(HardwareEncoder("hevc_nvenc").encodesFullChroma)
        listOf("h264_vaapi", "h264_qsv", "h264_amf", "h264_mf", "h264_videotoolbox").forEach {
            assertFalse(HardwareEncoder(it).encodesFullChroma, it)
        }
    }

    @Test
    fun `frames reach VAAPI encoders by uploading them`() {
        assertEquals(EncoderInput(null, listOf("format=nv12", "hwupload")), HardwareEncoder("h264_vaapi").input())
        assertEquals(EncoderInput(null, listOf("format=p010le", "hwupload")), HardwareEncoder("hevc_vaapi").input("p010le"))
    }

    @Test
    fun `Quick Sync, AMF and Media Foundation take 8 bit frames as NV12`() {
        listOf("h264_qsv", "h264_amf", "h264_mf").forEach {
            assertEquals(EncoderInput("nv12", emptyList()), HardwareEncoder(it).input("yuv420p"), it)
        }
        assertEquals(EncoderInput("p010le", emptyList()), HardwareEncoder("hevc_qsv").input("p010le"))
        assertEquals(EncoderInput("yuv444p", emptyList()), HardwareEncoder("h264_nvenc").input("yuv444p"))
    }

    @Test
    fun `quality is mapped to the rate control of the encoder`() {
        assertEquals("23", HardwareEncoder("hevc_nvenc").arguments(60).valueOf("-cq"))
        assertEquals("/dev/dri/renderD128", HardwareEncoder("h264_vaapi").arguments(60).valueOf("-vaapi_device"))
        assertEquals("23", HardwareEncoder("h264_qsv").arguments(60).valueOf("-global_quality"))
        assertEquals("60", HardwareEncoder("h264_mf").arguments(60).valueOf("-quality"))
        assertEquals("60", HardwareEncoder("hevc_videotoolbox").arguments(60).valueOf("-q:v"))
        assertEquals(listOf("-preset", "p5", "-tune", "hq"), HardwareEncoder("h264_nvenc").arguments(null))
    }

    @Test
    fun `only the H264 encoder of AMF has a B-frame quantizer`() {
        assertEquals("23", HardwareEncoder("h264_amf").arguments(60).valueOf("-qp_b"))
        val hevc = HardwareEncoder("hevc_amf").arguments(60)
        assertEquals("23", hevc.valueOf("-qp_p"))
        assertFalse("-qp_b" in hevc)
    }

    /**
     * An ffmpeg that logs its arguments and encodes only with [codec]
     */
    private fun fakeFfmpeg(codec: String): Pair<File, File> {
        val log = File.createTempFile("ffmpeg-calls", ".txt").apply { deleteOnExit() }
        val script = File.createTempFile("fake-ffmpeg", ".sh").apply {
            writeText("#!/bin/sh\necho \"\$*\" >> '${log.path}'\ncase \"\$*\" in *\"-c:v $codec \"*) exit 0;; esac\nexit 1\n")
            setExecutable(true)
            deleteOnExit()
        }
        return script to log
    }

    @Test
    fun `encoders are tried in order with the pixel format and the result is cached`() {
        if (Platform.type != PlatformType.GENERIC) return
        val (ffmpeg, log) = fakeFfmpeg("hevc_nvenc")
        assertEquals(HardwareEncoder("hevc_nvenc"), findHardwareEncoder("hevc", "p010le", ffmpeg.path))
        assertEquals(HardwareEncoder("hevc_nvenc"), findHardwareEncoder("hevc", "p010le", ffmpeg.path))
        val calls = log.readLines()
        assertEquals(1, calls.size)
        assertTrue("-pix_fmt p010le" in calls[0])
        assertTrue("-preset p5 -tune hq" in calls[0])
    }

    @Test
    fun `no encoder is found when none works`() {
        if (Platform.type != PlatformType.GENERIC) return
        val (ffmpeg, log) = fakeFfmpeg("none")
        assertNull(findHardwareEncoder("h264", ffmpeg = ffmpeg.path))
        val calls = log.readLines()
        assertTrue(calls.first().contains("-c:v h264_nvenc"))
        assertTrue(calls.drop(1).all { "-c:v h264_vaapi" in it && "hwupload" in it && "-vaapi_device /dev/dri/renderD" in it })
    }
}
