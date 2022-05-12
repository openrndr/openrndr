import org.openrndr.application
import org.openrndr.draw.colorBuffer
import org.openrndr.filter.color.delinearize
import org.openrndr.filter.color.hybridLogGamma
import org.openrndr.filter.color.linearize

/**
 * Applies multiple filters reading from a colorBuffer and writing into
 * a different colorBuffer to make sure they don't throw any errors.
 */
fun main() {
    application {
        program {
            val cb0 = colorBuffer(256, 256)
            val cb1 = colorBuffer(256, 256)
            extend {
                linearize.apply(cb0, cb1)
                delinearize.apply(cb0, cb1)
                hybridLogGamma.apply(cb0, cb1)
            }
        }
    }
}