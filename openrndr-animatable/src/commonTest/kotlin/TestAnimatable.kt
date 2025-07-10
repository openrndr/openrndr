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
        assertEquals(0L, a.createAtTimeInNs)
        a.cancel()
        a.apply {
            ::x.animate(2.0, 1000)
            ::v.animate(Vector2(3.0, 2.0), 2000)
        }
        a.updateAnimation(0L)
        assertTrue(a.hasAnimations())
        a.apply {
            assertTrue(::x.hasAnimations)
            assertTrue(::v.hasAnimations)
            assertFalse(::n.hasAnimations)
            assertEquals(1000L, ::x.durationInMs)
            assertEquals(2000L, ::v.durationInMs)
            assertEquals(0L, ::n.durationInMs)
        }
        assertEquals(0.0, a.x)
        assertEquals(Vector2(0.0, 2.0), a.v)
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
            assertFalse(::x.hasAnimations)
            assertTrue(::v.hasAnimations)
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
            assertEquals(3000, ::v.durationInMs)
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

            assertEquals(2000, ::u1.durationInMs)
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
            assertEquals(1000, ::u.durationInMs)
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
            assertTrue(::u.hasAnimations)
            assertTrue(::x.hasAnimations)
            assertTrue(::v.hasAnimations)
        }

        a.apply {
            ::u.cancel()
            assertFalse(::u.hasAnimations)
            assertFalse(::x.hasAnimations)
            assertFalse(::v.hasAnimations)
        }


        a.apply {
            ::u.animationGroup {
                ::x.animate(1.0, 1000)
                ::v.animate(Vector2.ONE, 2000)
            }
            assertEquals(2000, ::u.durationInMs)
            assertEquals(1000, ::x.durationInMs)
            assertEquals(2000, ::v.durationInMs)
            ::u.cancel()
            assertFalse(hasAnimations())
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
            assertTrue(completed)
            assertTrue(xCompleted)
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
            assertTrue(cancelled)
            assertTrue(xCancelled)
        }
    }
}