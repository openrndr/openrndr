import org.openrndr.ffmpeg.VideoDeviceMode
import org.openrndr.ffmpeg.parseDshowModes
import org.openrndr.ffmpeg.parseV4l2Modes
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * On Windows and Linux the modes are read from FFmpeg's log output, the samples follow the formats of FFmpeg 8's
 * dshow.c and v4l2.c
 */
class TestDeviceModes {
    @Test
    fun `dshow modes`() {
        val lines = """
            DirectShow video device options (from video devices)
             Pin "Capture" (alternative pin name "0")
              pixel_format=yuyv422  min s=640x480 fps=5 max s=640x480 fps=30
              pixel_format=yuyv422  min s=1280x720 fps=5 max s=1280x720 fps=10 (tv, bt470bg/bt709/unknown, left)
              vcodec=mjpeg  min s=1920x1080 fps=5 max s=1920x1080 fps=30
              vcodec=mjpeg  min s=1920x1080 fps=5 max s=1920x1080 fps=30
              unknown compression type 0x3231564E  min s=640x480 fps=5 max s=640x480 fps=30
              pixel_format=bgr24  min s=320x240 fps=7.5 max s=1280x960 fps=29.97(left)
        """.trimIndent().lines()
        assertEquals(
            listOf(
                VideoDeviceMode(640, 480, 5.0, 30.0, pixelFormat = "yuyv422"),
                VideoDeviceMode(1280, 720, 5.0, 10.0, pixelFormat = "yuyv422"),
                VideoDeviceMode(1920, 1080, 5.0, 30.0, codec = "mjpeg"),
                VideoDeviceMode(320, 240, 7.5, 29.97, pixelFormat = "bgr24"),
                VideoDeviceMode(1280, 960, 7.5, 29.97, pixelFormat = "bgr24")
            ),
            parseDshowModes(lines)
        )
    }

    @Test
    fun `v4l2 modes`() {
        val lines = """
            Raw       :     yuyv422 :           YUYV 4:2:2 : 640x480 1280x720
            Compressed:       mjpeg :          Motion-JPEG : 640x480 1920x1080
            Raw       :     nv12 :               Y/UV 4:2:0 : Emulated : 320x240
            Raw       :     yuv420p :      Planar YUV 4:2:0 : {8-4096, 8}x{8-2160, 8}
        """.trimIndent().lines()
        assertEquals(
            listOf(
                VideoDeviceMode(640, 480, pixelFormat = "yuyv422"),
                VideoDeviceMode(1280, 720, pixelFormat = "yuyv422"),
                VideoDeviceMode(640, 480, codec = "mjpeg"),
                VideoDeviceMode(1920, 1080, codec = "mjpeg"),
                VideoDeviceMode(320, 240, pixelFormat = "nv12")
            ),
            parseV4l2Modes(lines)
        )
    }
}
