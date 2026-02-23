import org.junit.jupiter.api.assertThrows
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.ComputeShader
import org.openrndr.draw.ImageAccess
import org.openrndr.draw.ShaderType
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.imageBinding
import org.openrndr.internal.Driver

import kotlin.test.Test
import kotlin.test.assertEquals

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

    @Test
    fun testImageBinding() {
        val source = colorBuffer(256, 256, format = ColorFormat.RGBa, type = ColorType.FLOAT32)
        source.fill(ColorRGBa.RED)
        val target = colorBuffer(256, 256, format = ColorFormat.RGBa, type = ColorType.FLOAT32)
        target.fill(ColorRGBa.BLUE)
        val cc = ComputeShader.fromCode("""
            ${Driver.instance.shaderConfiguration(ShaderType.COMPUTE)}
            layout(local_size_x = 1, local_size_y = 1, local_size_z = 1) in;
            uniform layout(binding=0, rgba32f) readonly highp image2D source;
            uniform layout(binding=1, rgba32f) writeonly highp image2D target;
            void main() {
                for (int y = 0; y < 256; ++y) {
                    for (int x = 0; x < 256; ++x) {
                        vec4 c =  imageLoad(source, ivec2(x, y));
                        imageStore(target, ivec2(x, y), c);                                
                    }
                }
            }
        """.trimIndent(), "test")
        cc.image("source", 0, source.imageBinding(0, ImageAccess.WRITE))
        cc.image("target", 1, target.imageBinding(0, ImageAccess.WRITE))
        cc.execute(1, 1, 1)

        val s = target.shadow
        s.download()
        for (y in 0 until 256) {
            for (x in 0 until 256) {
                assertEquals(ColorRGBa.RED.toLinear(), s[x, y])
            }
        }
    }
}