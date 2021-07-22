import org.junit.jupiter.api.Test
import org.openrndr.kartifex.utils.Scalars

class TestNormalizationFactor    {

    @Test
    fun test0() {
        println(Scalars.normalizationFactor(0.0, 0.0, 0.0))
        println(Scalars.normalizationFactor(0.01, 0.10, 0.1))

    }
}