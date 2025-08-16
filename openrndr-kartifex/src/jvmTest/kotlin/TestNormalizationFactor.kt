import org.openrndr.kartifex.utils.*
import kotlin.test.Test

class TestNormalizationFactor    {

    @Test
    fun test0() {
        println(normalizationFactor(0.0, 0.0, 0.0))
        println(normalizationFactor(0.01, 0.10, 0.1))

    }
}