import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should be`
import org.openrndr.animatable.Animatable
import org.openrndr.math.Vector2
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TestAnimatable : Spek({

    describe("an animatable") {
        val a = object : Animatable() {
            var x = 0.0
            var v = Vector2(0.0, 2.0)
            var n = 0.0
        }
        a.updateAnimation(0L)
        a.createAtTimeInNs `should be equal to` 0L
        a.cancel()
        a.apply {
            ::x.animate(2.0, 1000)
            ::v.animate(Vector2(3.0, 2.0), 2000)
        }
        a.updateAnimation(0L)
        a.hasAnimations() `should be` true
        a.apply {
            ::x.hasAnimations `should be` true
            ::v.hasAnimations `should be` true
            ::n.hasAnimations `should be` false
            ::x.durationInMs `should be equal to` 1000L
            ::v.durationInMs `should be equal to` 2000L
            ::n.durationInMs `should be equal to` 0L
        }
        a.x `should be equal to` 0.0
        a.v `should be equal to` Vector2(0.0, 2.0)
    }

    describe("an animatable 2") {
        val a = object : Animatable() {
            var x = 0.0
            var v = Vector2(0.0, 2.0)
            var n = 0.0
        }
        a.updateAnimation(0L)
        a.apply {
            ::x.animate(1.0, 1000)
            ::v.animate(Vector2.ONE, 2000)
        }
        it("can be cancelled") {
            a.apply {
                ::x.cancel()
                ::x.hasAnimations `should be` false
                ::v.hasAnimations `should be` true
            }
        }
    }

    describe("a sequence of animations") {
        val a = object : Animatable() {
            var x = 0.0
            var v = Vector2(0.0, 2.0)
            var n = 0.0
        }
        a.updateAnimation(0L)
        it("should have the right duration") {
            a.apply {
                ::x.animate(1.0, 1000)
                ::x.complete()
                ::v.animate(Vector2.ONE, 2000)
                ::v.durationInMs `should be equal to` 3000
            }
        }
    }

    describe("a sequence of animation groups") {
        val a = object : Animatable() {
            var x = 0.0
            var v = Vector2(0.0, 2.0)
            var u0 = Unit
            var u1 = Unit
        }
        a.updateAnimation(0L)
        it("should have the right duration") {
            a.apply {
                ::u0.animationGroup {
                    ::x.animate(1.0, 1000)
                }
                ::u0.complete()
                ::u1.animationGroup {
                    ::x.animate(0.0, 1000)
                }

                ::u1.durationInMs `should be equal to` 2000
            }
        }
    }

    describe("creating new animations inside complete") {
        val a = object : Animatable() {
            var x = 0.0
            var v = Vector2(0.0, 2.0)
            var u = Unit
        }
        a.updateAnimation(0L)
        it("should have the right duration") {
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
                ::u.durationInMs `should be equal to` 1000
            }
        }
    }




    describe("an animatable with groups") {
        val a = object : Animatable() {
            var x = 0.0
            var v = Vector2(0.0, 2.0)
            var n = 0.0
            var u = Unit
        }
        a.updateAnimation(0L)

        it("can properly report hasAnimations") {
            a.apply {
                ::u.animationGroup {
                    ::x.animate(1.0, 1000)
                    ::v.animate(Vector2.ONE, 2000)
                }
                ::u.hasAnimations `should be` true
                ::x.hasAnimations `should be` true
                ::v.hasAnimations `should be` true
            }
        }

        it("can be properly cancelled") {
            a.apply {
                ::u.cancel()
                ::u.hasAnimations `should be` false
                ::x.hasAnimations `should be` false
                ::v.hasAnimations `should be` false
            }
        }

        it("can properly report duration") {
            a.apply {
                ::u.animationGroup {
                    ::x.animate(1.0, 1000)
                    ::v.animate(Vector2.ONE, 2000)
                }
                ::u.durationInMs `should be equal to` 2000
                ::x.durationInMs `should be equal to` 1000
                ::v.durationInMs `should be equal to` 2000
                ::u.cancel()
                hasAnimations() `should be equal to` false
            }
        }

        it("triggers the complete event") {
            var completed = false
            var xCompleted = false

            a.apply {
                ::u.animationGroup {
                    ::x.animate(1.0, 1000).completed.listen {
                        xCompleted = true
                    }
                    ::v.animate(Vector2.ONE, 2000)
                }.completed.listen {
                    completed = true
                }
            }
            a.updateAnimation(2000 * 1000)
            completed `should be` true
            xCompleted `should be` true
        }

        it("triggers the cancel event") {
            var cancelled = false
            var xCancelled = false
            a.updateAnimation(0L)

            a.apply {
                ::u.animationGroup {
                    ::x.animate(1.0, 1000).cancelled.listen {
                        xCancelled = true
                    }
                    ::v.animate(Vector2.ONE, 2000)
                }.cancelled.listen {
                    cancelled = true
                }
            }
            a.updateAnimation(500 * 1000)
            a.apply {
                ::u.cancel()
            }
            cancelled `should be` true
            xCancelled `should be` true
        }

    }
})