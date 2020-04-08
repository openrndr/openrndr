import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should throw`
import org.amshove.kluent.invoking
import org.openrndr.shape.contour
import org.openrndr.shape.contours
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.lang.IllegalArgumentException

object TestContourBuilder : Spek({

    describe("single segment contours") {
        it("support a single line segment") {
            val c = contour {
                moveTo(0.0, 0.0)
                lineTo(100.0, 100.0)
            }
            c.closed `should be equal to` false
            c.segments.size `should be equal to` 1
        }

        it("support a single quadratic curve segment") {
            val c = contour {
                moveTo(0.0, 0.0)
                curveTo(20.0, 100.0, 100.0, 100.0)
            }
            c.closed `should be equal to` false
            c.segments.size `should be equal to` 1
        }

        it("support a single cubic curve segment") {
            val c = contour {
                moveTo(0.0, 0.0)
                curveTo(100.0, 20.0, 20.0, 100.0, 100.0, 100.0)
            }
            c.closed `should be equal to` false
            c.segments.size `should be equal to` 1
        }

        it("support a single arc segment ") {
            val c = contour {
                moveTo(0.0, 0.0)
                arcTo(100.0, 100.0, 40.0, false, false, 200.0, 200.0)
            }
            c.closed `should be equal to` false
        }

        it("support a single quadratic continueTo segment ") {
            val c = contour {
                moveTo(0.0, 0.0)
                continueTo(100.0, 100.0)
            }
            c.closed `should be equal to` false
            c.segments.size `should be equal to` 1
        }

        it("support a single cubic continueTo segment ") {
            val c = contour {
                moveTo(0.0, 0.0)
                continueTo(20.0, 30.0, 100.0, 100.0)
            }
            c.closed `should be equal to` false
            c.segments.size `should be equal to` 1
        }

        it("supports a simple closed shape") {
            val c = contour {
                moveTo(0.0, 0.0)
                lineTo(100.0, 100.0)
                lineTo(200.0, 100.0)
                close()
            }
            c.closed `should be equal to` true
            c.segments.size `should be equal to` 3
        }
    }

    describe("faulty contour") {
        it("detects multiple moveTo commands") {
            invoking {
                contour {
                    moveTo(0.0, 0.0)
                    lineTo(100.0, 100.0)
                    moveTo(200.0, 200.0)
                }
            } `should throw` IllegalArgumentException::class
        }

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
                    curveTo(50.0, 50.0, 100.0, 100.0)

                }
            } `should throw` IllegalArgumentException::class
        }

        it("detects cubic curveTo before moveTo") {
            invoking {
                contour {
                    curveTo(20.0, 20.0, 50.0, 50.0, 100.0, 100.0)

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
    describe("multiple contours made with contours {}") {
        it("supports multiple open contours") {
            val c = contours {
                moveTo(0.0, 0.0)
                lineTo(200.0, 200.0)
                moveTo(300.0, 300.0)
                lineTo(400.0, 400.0)
            }
            c.size `should be equal to` 2
            c.all { !it.closed } `should be equal to` true
            c.all { it.segments.size == 1 } `should be equal to` true
        }

        it("supports a single open contour") {
            val c = contours {
                moveTo(0.0, 0.0)
                lineTo(100.0, 100.0)
            }
            c.size `should be equal to` 1
            c.first().closed `should be equal to` false
            c.first().segments.size `should be equal to` 1
        }

        it("supports an open contour followed by a closed contour") {
            val c = contours {
                moveTo(0.0, 0.0)
                lineTo(100.0, 100.0)
                moveTo(200.0, 200.0)
                lineTo(300.0,200.0)
                lineTo(200.0,300.0)
                close()
            }
            c.size `should be equal to` 2
            c[0].closed `should be equal to` false
            c[1].closed `should be equal to` true
            c[0].segments.size `should be equal to` 1
            c[1].segments.size `should be equal to` 3
        }

        it("supports a closed contour followed by an open contour") {
            val c = contours {
                moveTo(200.0, 200.0)
                lineTo(300.0,200.0)
                lineTo(200.0,300.0)
                close()
                moveTo(0.0, 0.0)
                lineTo(100.0, 100.0)
            }
            c.size `should be equal to` 2
            c[0].closed `should be equal to` true
            c[1].closed `should be equal to` false
            c[0].segments.size `should be equal to` 3
            c[1].segments.size `should be equal to` 1
        }

        it("supports multiple open contours") {
            val c = contours {
                moveTo(200.0, 200.0)
                lineTo(300.0,200.0)
                lineTo(200.0,300.0)
                close()
                moveTo(200.0, 200.0)
                lineTo(300.0,200.0)
                lineTo(200.0,300.0)
                close()
            }
            c.size `should be equal to` 2
            c[0].closed `should be equal to` true
            c[1].closed `should be equal to` true
            c[0].segments.size `should be equal to` 3
            c[1].segments.size `should be equal to` 3
        }
    }
})

