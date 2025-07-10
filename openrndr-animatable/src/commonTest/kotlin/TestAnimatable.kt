import org.openrndr.animatable.Animatable
import org.openrndr.math.Vector2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestAnimatable {
    @Test
    fun `an animatable`() {
        val a = object : Animatable() {
            var x = 0.0
            var v = Vector2(0.0, 2.0)
            var n = 0.0
        }
        a.updateAnimation(0L)
        assertEquals(0L, a.createAtTimeInNs, "create time should be 0")
        a.cancel()
        a.apply {
            ::x.animate(2.0, 1000)
            ::v.animate(Vector2(3.0, 2.0), 2000)
        }
        a.updateAnimation(0L)
        assertTrue(a.hasAnimations(), "hasAnimations should be true")
        a.apply {
            assertTrue(::x.hasAnimations, "x has animation")
            assertTrue(::v.hasAnimations, "v has animation")
            assertFalse(::n.hasAnimations,"n has no animation")
            assertEquals(1000L, ::x.durationInMs, "x duration should be 1000")
            assertEquals(2000L, ::v.durationInMs, "v duration should be 2000")
            assertEquals(0L, ::n.durationInMs, "n duration should be 0")
        }
        assertEquals(0.0, a.x, "initial value of x is 0.0")
        assertEquals(Vector2(0.0, 2.0), a.v, "initial value of v is (0, 2)")
    }

    @Test
    fun `an animatable can be cancelled`() {
        val a = object : Animatable() {
            var x = 0.0
            var v = Vector2(0.0, 2.0)
        }
        a.updateAnimation(0L)
        a.apply {
            ::x.animate(1.0, 1000)
            ::v.animate(Vector2.ONE, 2000)
        }
        a.apply {
            ::x.cancel()
            assertFalse(::x.hasAnimations, "x has no animation")
            assertTrue(::v.hasAnimations, "v has animation")
        }
    }

    @Test
    fun `a sequence of animations should have the right duration`() {
        val a = object : Animatable() {
            var x = 0.0
            var v = Vector2(0.0, 2.0)
        }
        a.updateAnimation(0L)
        a.apply {
            ::x.animate(1.0, 1000)
            ::x.complete()
            ::v.animate(Vector2.ONE, 2000)
            assertEquals(3000, ::v.durationInMs, "v duration should be 3000")
        }
    }

    @Test
    fun `a sequence of animation groups should have the right duration`() {
        val a = object : Animatable() {
            var x = 0.0
            var u0 = Unit
            var u1 = Unit
        }
        a.updateAnimation(0L)
        a.apply {
            ::u0.animationGroup {
                ::x.animate(1.0, 1000)
            }
            ::u0.complete()
            ::u1.animationGroup {
                ::x.animate(0.0, 1000)
            }

            assertEquals(2000, ::u1.durationInMs, "u1 duration should be 2000")
        }
    }

    @Test
    fun `creating new animations inside complete should have the right duration`() {
        val a = object : Animatable() {
            var x = 0.0
            var u = Unit
        }
        a.updateAnimation(0L)
        a.apply {
            ::u.animationGroup {
                ::x.animate(1.0, 1000)
            }.completed.listen {
                ::u.animationGroup {
                    ::x.animate(0.0, 1000)
                }
            }
        }
        a.updateAnimation(1000 * 1000)
        a.apply {
            assertEquals(1000, ::u.durationInMs, "u duration should be 1000")
        }
    }

    @Test
    fun `an animatable with groups can properly report its properties`() {
        val a = object : Animatable() {
            var x = 0.0
            var v = Vector2(0.0, 2.0)
            var u = Unit
        }
        a.updateAnimation(0L)

        a.apply {
            ::u.animationGroup {
                ::x.animate(1.0, 1000)
                ::v.animate(Vector2.ONE, 2000)
            }
            assertTrue(::u.hasAnimations, "u has animation")
            assertTrue(::x.hasAnimations, "x has animation")
            assertTrue(::v.hasAnimations, "v has animation")
        }

        a.apply {
            ::u.cancel()
            assertFalse(::u.hasAnimations, "u has no animation")
            assertFalse(::x.hasAnimations, "x has no animation")
            assertFalse(::v.hasAnimations, "v has no animation" )
        }


        a.apply {
            ::u.animationGroup {
                ::x.animate(1.0, 1000)
                ::v.animate(Vector2.ONE, 2000)
            }
            assertEquals(2000, ::u.durationInMs, "u duration should be 2000")
            assertEquals(1000, ::x.durationInMs, "x duration should be 1000")
            assertEquals(2000, ::v.durationInMs, "v duration should be 2000")
            ::u.cancel()
            assertFalse(hasAnimations(), "a has no animations")
        }

        a.apply {
            var completed = false
            var xCompleted = false
            ::u.animationGroup {
                ::x.animate(1.0, 1000).completed.listen {
                    xCompleted = true
                }
                ::v.animate(Vector2.ONE, 2000)
            }.completed.listen {
                completed = true
            }
            updateAnimation(2000 * 1000)
            assertTrue(completed, "completed is true")
            assertTrue(xCompleted, "xCompleted is true")
        }


        a.apply {
            var cancelled = false
            var xCancelled = false
            updateAnimation(0L)

            ::u.animationGroup {
                ::x.animate(1.0, 1000).cancelled.listen {
                    xCancelled = true
                }
                ::v.animate(Vector2.ONE, 2000)
            }.cancelled.listen {
                cancelled = true
            }
            updateAnimation(500 * 1000)
            ::u.cancel()
            assertTrue(cancelled, "cancelled is true")
            assertTrue(xCancelled, "xCancelled is true")
        }
    }
}