import org.openrndr.application
import org.openrndr.shape.LineSegment
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TestDrawLineSegments : Spek({
    describe("drawing multiple lineSegments using a List of LineSegment") {
        it("should not throw exceptions") {
            application {
                program {
                    val segments = List(10) {
                        LineSegment(
                            (it * 1001.0) % width,
                            (it * 1337.0) % height,
                            (it * 6502.0) % width,
                            (it * 4004.0) % height
                        )
                    }
                    extend {
                        drawer.lineSegments(segments)
                        application.exit()
                    }
                }
            }
        }
    }

    describe("drawing a single line segment") {
        it("should not throw exceptions") {
            application {
                program {
                    extend {
                        val ls = LineSegment(0.0, 0.0, 50.0, 50.0)
                        drawer.lineSegment(ls)
                        application.exit()
                    }
                }
            }
        }
    }

    describe("drawing one lineSegment using a List of LineSegment") {
        it("should not throw exceptions") {
            application {
                program {
                    val segments = List(1) {
                        LineSegment(
                            (it * 1001.0) % width,
                            (it * 1337.0) % height,
                            (it * 6502.0) % width,
                            (it * 4004.0) % height
                        )
                    }
                    extend {
                        drawer.lineSegments(segments)
                        application.exit()
                    }
                }
            }
        }
    }

})

