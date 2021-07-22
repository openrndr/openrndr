import io.kotest.matchers.doubles.shouldBeExactly
import io.kotest.matchers.ints.shouldBeExactly
import org.openrndr.kartifex.Vec2
import kotlin.test.Test

class CommonTestVec2 {
    @Test
    fun testHash() {
        Vec2(0.0, 0.0).hashCode().shouldBeExactly(961)
    }
    @Test
    fun testPseudoNorm() {
        val v2 = Vec2(2100.0, 3220.0)
        val pn = v2.pseudoNorm()
        pn.x.shouldBeExactly(1.025390625)
        pn.y.shouldBeExactly(1.572265625)
    }
}