import org.junit.jupiter.api.assertThrows
import org.openrndr.draw.ComputeShader
import org.openrndr.draw.ImageAccess
import org.openrndr.draw.ShaderType
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.imageBinding
import org.openrndr.internal.Driver

import kotlin.test.Test

class TestImageBindingsGL3 : AbstractApplicationTestFixture() {
    @Test
    fun testRejectSRGB() {
        val cc = ComputeShader.fromCode("""
            ${Driver.instance.shaderConfiguration(ShaderType.COMPUTE)}
            layout(local_size_x = 1, local_size_y = 1, local_size_z = 1) in;
            uniform layout(binding=0, rgba8ui) writeonly highp uimage2D image;
            void main() {
            
            }
        """.trimIndent(), "test")
        val cb = colorBuffer(256, 256)
        assertThrows<IllegalArgumentException> {
            cc.image("image", 0, cb.imageBinding(0, ImageAccess.WRITE))
        }
    }
}