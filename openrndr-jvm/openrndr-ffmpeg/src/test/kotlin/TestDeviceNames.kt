import org.openrndr.ffmpeg.parseAVFoundationVideoDevices
import org.openrndr.ffmpeg.parseDshowVideoDevices
import org.openrndr.ffmpeg.sortVideoDeviceFiles
import kotlin.test.Test
import kotlin.test.assertEquals

class TestDeviceNames {
    @Test
    fun `dshow devices with media types`() {
        val lines = """
            "Integrated Camera" (video)
              Alternative name "@device_pnp_\\?\usb#vid_04f2&pid_b6d9&mi_00#6&2a3c4bd&0&0000#{65e8773d-8f56-11d0-a3b9-00a0c9223196}\global"
            "OBS Virtual Camera" (none)
              Alternative name "@device_sw_{860BB310-5D01-11D0-BD3B-00A0C911CE86}\{A3FCE0F5-3493-419F-958A-ABA1250EC20B}"
            "Capture Card" (audio, video)
              Alternative name "@device_pnp_\\?\usb#vid_1234"
            "Microphone (Realtek Audio)" (audio)
              Alternative name "@device_cm_{33D9A762-90C8-11D0-BD43-00A0C911CE86}\wave_{1}"
        """.trimIndent().lines()
        assertEquals(listOf("Integrated Camera", "Capture Card"), parseDshowVideoDevices(lines))
    }

    @Test
    fun `dshow devices in sections`() {
        val lines = """
            DirectShow video devices (some may be both video and audio devices)
             "Integrated Camera"
                Alternative name "@device_pnp_\\?\usb#vid_04f2"
            DirectShow audio devices
             "Microphone (Realtek Audio)"
                Alternative name "@device_cm_{33D9A762}\wave_{1}"
            Error opening input files: Immediate exit requested
        """.trimIndent().lines()
        assertEquals(listOf("Integrated Camera"), parseDshowVideoDevices(lines))
    }

    @Test
    fun `avfoundation devices`() {
        val lines = """
            Some other message
            AVFoundation video devices:
            [0] FaceTime HD Camera
            [1] iPhone Camera
            [2] Capture screen 0
            AVFoundation audio devices:
            [0] MacBook Pro Microphone
        """.trimIndent().lines()
        assertEquals(listOf("FaceTime HD Camera", "iPhone Camera", "Capture screen 0"), parseAVFoundationVideoDevices(lines))
        assertEquals(emptyList(), parseAVFoundationVideoDevices(emptyList()))
    }

    @Test
    fun `video device files`() {
        val names = listOf("video10", "null", "video2", "/dev/video0", "video", "videoX", "media0")
        assertEquals(listOf("/dev/video0", "/dev/video2", "/dev/video10"), sortVideoDeviceFiles(names))
    }
}
