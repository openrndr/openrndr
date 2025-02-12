import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.volumeTexture
import org.openrndr.internal.Driver
import org.openrndr.internal.gl3.DriverTypeGL
import org.openrndr.internal.gl3.glType
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test

class TestVolumeTextureGL3 : AbstractApplicationTestFixture() {

    @Test
    fun testVolumeTextureMostCommon() {
        val vt = volumeTexture(64, 64, 64)
        val bb = ByteBuffer.allocateDirect(vt.width * vt.height * vt.depth * 4)
        bb.order(ByteOrder.nativeOrder())
        vt.write(bb)
        bb.rewind()
        if (Driver.glType == DriverTypeGL.GL) {
            vt.read(bb)
        }
        vt.close()
    }

    @Test
    fun testVolumeTextureRGBaFloat16() {
        val vt = volumeTexture(64, 64, 64, type = ColorType.FLOAT16)
        val bb = ByteBuffer.allocateDirect(vt.width * vt.height * vt.depth * 8)
        bb.order(ByteOrder.nativeOrder())
        vt.write(bb)
        bb.rewind()
        if (Driver.glType == DriverTypeGL.GL) {
            vt.read(bb)
        }
        vt.close()
    }

    @Test
    fun testVolumeTextureRGBaFloat32() {
        val vt = volumeTexture(64, 64, 64, type = ColorType.FLOAT32)
        val bb = ByteBuffer.allocateDirect(vt.width * vt.height * vt.depth * 16)
        bb.order(ByteOrder.nativeOrder())
        vt.write(bb)
        bb.rewind()
        if (Driver.glType == DriverTypeGL.GL) {
            vt.read(bb)
        }
        vt.close()
    }

    @Test
    fun testVolumeTextureRFloat32() {
        val vt = volumeTexture(64, 64, 64, format = ColorFormat.R, type = ColorType.FLOAT32)
        val bb = ByteBuffer.allocateDirect(vt.width * vt.height * vt.depth * 4)
        bb.order(ByteOrder.nativeOrder())
        vt.write(bb)
        bb.rewind()
        if (Driver.glType == DriverTypeGL.GL) {
            vt.read(bb)
        }
        vt.close()
    }

    @Test
    fun testVolumeTextureRGFloat32() {
        val vt = volumeTexture(64, 64, 64, format = ColorFormat.RG, type = ColorType.FLOAT32)
        val bb = ByteBuffer.allocateDirect(vt.width * vt.height * vt.depth * 8)
        bb.order(ByteOrder.nativeOrder())
        vt.write(bb)
        bb.rewind()
        if (Driver.glType == DriverTypeGL.GL) {
            vt.read(bb)
        }
        vt.close()
    }
}
