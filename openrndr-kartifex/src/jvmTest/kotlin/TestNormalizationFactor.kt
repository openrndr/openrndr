import org.junit.jupiter.api.Test
import org.openrndr.kartifex.utils.*

class TestNormalizationFactor    {

    @Test
    fun test0() {
        println(normalizationFactor(0.0, 0.0, 0.0))
        println(normalizationFactor(0.01, 0.10, 0.1))

    }
}