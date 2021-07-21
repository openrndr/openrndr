import io.kotest.matchers.ints.shouldBeExactly
import org.openrndr.kartifex.Vec2
import kotlin.test.Test

class CommonTestVec2 {
    @Test
    fun testHash() {
        Vec2(0.0, 0.0).hashCode().shouldBeExactly(961)
    }
}