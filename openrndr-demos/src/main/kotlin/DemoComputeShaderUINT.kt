import org.openrndr.application
import org.openrndr.draw.*
import org.openrndr.draw.font.BufferAccess

fun main() = application {
    program {
        val cb = colorBuffer(16, 16, format = ColorFormat.R, type = ColorType.UINT32_INT)
        val cs = computeStyle { computeTransform = "imageAtomicAdd(p_c, ivec2(0), 1);" }
        cs.image("c", cb.imageBinding(0, BufferAccess.READ_WRITE))
        cs.execute(32, 32)
    }
}
