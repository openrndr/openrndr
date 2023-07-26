import org.amshove.kluent.`should be equal to`
import org.openrndr.math.Vector2
import org.openrndr.shape.Triangle
import io.kotest.core.spec.style.DescribeSpec

object TestTriangle : DescribeSpec({
    describe("a triangle") {
        val t = Triangle(Vector2(0.0, 0.0), Vector2(0.0, 100.0), Vector2(100.0, 100.0))
        it("contains points") {
            (Vector2(30.0, 30.0) in t) `should be equal to` true
            (Vector2(1.0, 90.0) in t) `should be equal to` true
        }

        it("does not contain other points") {
            (Vector2(31.1, 30.0) in t) `should be equal to` false
            (Vector2(40.0, 30.0) in t) `should be equal to` false
            (Vector2(200.0, 200.0) in t) `should be equal to` false
        }
    }
})