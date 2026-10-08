import org.openrndr.math.Vector2
import org.openrndr.shape.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class OpenRemoveSelfIntersectionsTest {

    @Test
    fun simpleOpenPathIsUnchanged() {
        val c = contour {
            moveTo(0.0, 0.0)
            lineTo(100.0, 0.0)
            lineTo(100.0, 100.0)
        }
        val result = c.removeSelfIntersections(FillRule.NON_ZERO)
        assertEquals(1, result.contours.size)
        assertEquals(200.0, result.contours[0].length, 1E-9)
    }

    @Test
    fun figureEightOpenPathExcisesLoopButKeepsOriginalEndpoints() {
        // open path shaped like a figure-8: crosses itself once, between distinct segments.
        // Removing the self-intersection should excise the crossing loop and reconnect head to
        // tail, preserving the path's original start and end point -- not just return the first
        // of several disjoint pieces (that was the bug: see cross-session report).
        val c = contour {
            moveTo(0.0, 0.0)
            lineTo(100.0, 100.0)
            lineTo(100.0, 0.0)
            lineTo(0.0, 100.0)
        }
        val result = c.removeSelfIntersections(FillRule.NON_ZERO)
        assertEquals(1, result.contours.size)
        val cc = result.contours[0]
        assertFalse(cc.isSelfIntersecting)
        assertEquals(c.position(0.0), cc.position(0.0))
        assertEquals(c.position(1.0), cc.position(1.0))
    }

    @Test
    fun loopingSingleCubicSegmentExcisesLoopButKeepsOriginalEndpoints() {
        // a classic self-intersecting cubic BΓ©zier loop, entirely within one segment -- the
        // case intersections(c, c) can never detect, since it skips a === b.
        val loop = Segment2D(
            Vector2(0.0, 0.0),
            listOf(Vector2(200.0, 100.0), Vector2(-100.0, 100.0)),
            Vector2(100.0, 0.0)
        )
        val c = ShapeContour(listOf(loop), closed = false)
        val result = c.removeSelfIntersections(FillRule.NON_ZERO)
        assertEquals(1, result.contours.size)
        val cc = result.contours[0]
        assertFalse(cc.isSelfIntersecting)
        assertEquals(c.position(0.0), cc.position(0.0))
        assertEquals(c.position(1.0), cc.position(1.0))
    }

    @Test
    fun trailingLoopNearTheEndStillReachesTheRealEndpoint() {
        // regression guard for the reported bug: a loop positioned well past the midpoint of
        // the path, with a substantial amount of simple geometry both before AND after it.
        val c = contour {
            moveTo(0.0, 0.0)
            lineTo(0.0, 500.0)
            lineTo(100.0, 600.0)
            lineTo(100.0, 500.0)
            lineTo(0.0, 600.0)
            lineTo(0.0, 700.0)
            lineTo(300.0, 700.0)
        }
        val result = c.removeSelfIntersections(FillRule.NON_ZERO)
        assertEquals(1, result.contours.size)
        val cc = result.contours[0]
        assertFalse(cc.isSelfIntersecting)
        assertEquals(c.position(0.0), cc.position(0.0))
        assertEquals(c.position(1.0), cc.position(1.0))
    }
}
