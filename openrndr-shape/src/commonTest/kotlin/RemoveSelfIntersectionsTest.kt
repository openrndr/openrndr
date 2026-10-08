import org.openrndr.math.Vector2
import org.openrndr.shape.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RemoveSelfIntersectionsTest {

    /**
     * A simple (non-self-intersecting) rounded 5-point star whose tip is a single curve
     * bulging out to its apex. The rounding brings that curve close enough to its neighbor
     * that the self-intersection sweep in `nonZeroSelfUnion()` finds a spurious near-touch
     * right at the pinch, rather than a true crossing. At that location the real "inside"
     * region is thinner than the fixed inside/outside probe distance used to classify each
     * arc, so both sides of the probe used to land outside, the arc got dropped, and that
     * broke the ring's connectivity enough that no cycle was found at all -- collapsing a
     * provably simple, non-empty shape to zero contours under FillRule.NON_ZERO (EVEN_ODD was
     * unaffected, since it never goes through this code path).
     */
    @Test
    fun testNonZeroSelfUnionOnSimpleCurveWithNearTangentTip() {
        val contour = ShapeContour(simpleStarWithNearTangentTip(), closed = true)

        // the curve genuinely never crosses itself
        assertEquals(0, intersections(contour, contour).size)

        val evenOdd = contour.shape.removeSelfIntersections(FillRule.EVEN_ODD)
        assertEquals(1, evenOdd.contours.size)

        val nonZero = contour.shape.removeSelfIntersections(FillRule.NON_ZERO)
        assertEquals(1, nonZero.contours.size)
        assertTrue(abs(nonZero.area - evenOdd.area) < 1.0)
    }

    private fun abs(x: Double) = if (x < 0.0) -x else x

    private fun bits(l: Long): Double = Double.fromBits(l)

    private fun simpleStarWithNearTangentTip(): List<Segment2D> = listOf(
        Segment2D(Vector2(bits(4645637279689005825L), bits(4643395060080332913L)), listOf(Vector2(bits(4645191058374512300L), bits(4643550166493020311L))), Vector2(bits(4645181433379437612L), bits(4644022478699336107L))),
        Segment2D(Vector2(bits(4645181433379437612L), bits(4644022478699336107L)), listOf(), Vector2(bits(4645162183389288237L), bits(4644967103111967698L))),
        Segment2D(Vector2(bits(4645162183389288237L), bits(4644967103111967698L)), listOf(Vector2(bits(4645152558394213549L), bits(4645439415318283494L))), Vector2(bits(4644867153460277207L), bits(4645062964146881578L))),
        Segment2D(Vector2(bits(4644867153460277207L), bits(4645062964146881578L)), listOf(), Vector2(bits(4644296343592404522L), bits(4644310061804077746L))),
        Segment2D(Vector2(bits(4644296343592404522L), bits(4644310061804077746L)), listOf(Vector2(bits(4644010938658468180L), bits(4643933610632675830L))), Vector2(bits(4643558768769876948L), bits(4644070409216793047L))),
        Segment2D(Vector2(bits(4643558768769876948L), bits(4644070409216793047L)), listOf(), Vector2(bits(4642097642166407590L), bits(4644344006385027481L))),
        Segment2D(Vector2(bits(4642097642166407590L), bits(4644344006385027481L)), listOf(Vector2(bits(4641193302389225126L), bits(4644480804969144698L))), Vector2(bits(4641732965118753020L), bits(4644093038937426204L))),
        Segment2D(Vector2(bits(4641732965118753020L), bits(4644093038937426204L)), listOf(), Vector2(bits(4642812290577808810L), bits(4643317506873989214L))),
        Segment2D(Vector2(bits(4642812290577808810L), bits(4643317506873989214L)), listOf(Vector2(bits(4643281584563159040L), bits(4642648265865560064L))), Vector2(bits(4642812290577808810L), bits(4641872733802123076L))),
        Segment2D(Vector2(bits(4642812290577808810L), bits(4641872733802123076L)), listOf(), Vector2(bits(4641732965118753020L), bits(4640321669675249098L))),
        Segment2D(Vector2(bits(4641732965118753020L), bits(4640321669675249098L)), listOf(Vector2(bits(4641193302389225126L), bits(4639546137611812110L))), Vector2(bits(4642097642166407590L), bits(4639819734780046544L))),
        Segment2D(Vector2(bits(4642097642166407590L), bits(4639819734780046544L)), listOf(), Vector2(bits(4643558768769876948L), bits(4640366929116515410L))),
        Segment2D(Vector2(bits(4643558768769876948L), bits(4640366929116515410L)), listOf(Vector2(bits(4644010938658468180L), bits(4640640526284749844L))), Vector2(bits(4644296343592404522L), bits(4639887623941946012L))),
        Segment2D(Vector2(bits(4644296343592404522L), bits(4639887623941946012L)), listOf(), Vector2(bits(4644867153460277207L), bits(4638056022321065813L))),
        Segment2D(Vector2(bits(4644867153460277207L), bits(4638056022321065813L)), listOf(Vector2(bits(4645152558394213549L), bits(4636550217635458148L))), Vector2(bits(4645162183389288237L), bits(4638439466460721332L))),
        Segment2D(Vector2(bits(4645162183389288237L), bits(4638439466460721332L)), listOf(), Vector2(bits(4645181433379437612L), bits(4640462790151429290L))),
        Segment2D(Vector2(bits(4645181433379437612L), bits(4640462790151429290L)), listOf(Vector2(bits(4645191058374512300L), bits(4641407414564060882L))), Vector2(bits(4645637279689005825L), bits(4641717627389435677L))),
        Segment2D(Vector2(bits(4645637279689005825L), bits(4641717627389435677L)), listOf(), Vector2(bits(4646529722317992875L), bits(4642338053040185268L))),
        Segment2D(Vector2(bits(4646529722317992875L), bits(4642338053040185268L)), listOf(Vector2(bits(4646975943632486400L), bits(4642648265865560063L))), Vector2(bits(4646529722317992875L), bits(4642958478690934859L))),
        Segment2D(Vector2(bits(4646529722317992875L), bits(4642958478690934859L)), listOf(), Vector2(bits(4645637279689005825L), bits(4643395060080332913L))),
    )
}
