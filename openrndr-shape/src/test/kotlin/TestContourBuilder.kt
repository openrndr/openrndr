import org.amshove.kluent.`should throw`
import org.amshove.kluent.invoking
import org.openrndr.shape.contour
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.lang.IllegalArgumentException

object TestContourBuilder : Spek({
    describe("single segment  contours") {
        it ("support a single line segment") {
            val c = contour {
                moveTo(0.0, 0.0)
                lineTo(100.0, 100.0)
            }
        }

        it ("support a single quadratic curve segment") {
            val c = contour {
                moveTo(0.0, 0.0)
                curveTo(20.0, 100.0, 100.0, 100.0)
            }
        }

        it ("support a single cubic curve segment") {
            val c = contour {
                moveTo(0.0, 0.0)
                curveTo(100.0, 20.0, 20.0, 100.0, 100.0, 100.0)
            }
        }

        it ("support a single arc segment ") {
            val c = contour {
                moveTo(0.0, 0.0)
                arcTo(100.0, 100.0, 40.0, false, false, 200.0, 200.0)
            }
        }

        it ("support a single quadratic continueTo segment ") {
            val c = contour {
                moveTo(0.0, 0.0)
                continueTo(100.0, 100.0)
            }
        }

        it ("support a single cubic continueTo segment ") {
            val c = contour {
                moveTo(0.0, 0.0)
                continueTo(20.0, 30.0, 100.0, 100.0)
            }
        }


    }
    describe("faulty contour") {
        it("detects lineTo before moveTo") {
            invoking {
                contour {
                    lineTo(100.0, 100.0)

                }
            } `should throw` IllegalArgumentException::class
        }

        it("detects quadratic curveTo before moveTo") {
            invoking {
                contour {
                    curveTo(50.0, 50.0,100.0, 100.0)

                }
            } `should throw` IllegalArgumentException::class
        }

        it("detects cubic curveTo before moveTo") {
            invoking {
                contour {
                    curveTo(20.0, 20.0,50.0, 50.0,100.0, 100.0)

                }
            } `should throw` IllegalArgumentException::class
        }

        it("detects arcTo before moveTo") {
            invoking {
                contour {
                    arcTo(100.0, 100.0, 40.0, false, false, 200.0, 200.0)

                }
            } `should throw` IllegalArgumentException::class
        }

        it("detects quadratic continueTo before moveTo") {
            invoking {
                contour {
                    continueTo(200.0, 200.0)

                }
            } `should throw` IllegalArgumentException::class
        }

        it("detects cubic continueTo before moveTo") {
            invoking {
                contour {
                    continueTo(100.0, 300.0, 200.0, 200.0)

                }
            } `should throw` IllegalArgumentException::class
        }
    }
})

