import io.kotest.core.spec.style.DescribeSpec
import org.openrndr.application
import org.openrndr.shape.Circle
import org.openrndr.shape.LineSegment

class TestVertexBufferGL3 : DescribeSpec({
    describe("drawing a single contour") {
        it("should not throw exceptions") {
            application {
                program {
                    extend {
                        drawer.contour(Circle(width / 2.0, height / 2.0, 100.0).contour)
                        application.exit()
                    }
                }
            }
        }
    }

    describe("drawing org.openrndr.shape.contours with a single element") {
        it("should not throw exceptions") {
            application {
                program {
                    extend {
                        drawer.contours(listOf(Circle(width / 2.0, height / 2.0, 100.0).contour))
                        application.exit()
                    }
                }
            }
        }
    }

    describe("drawing org.openrndr.shape.contours with the same closed contour twice") {
        it("should not throw exceptions") {
            application {
                program {
                    extend {
                        drawer.contours(
                                listOf(
                                    Circle(width / 2.0, height / 2.0, 100.0).contour,
                                        Circle(width / 2.0, height / 2.0, 100.0).contour))
                        application.exit()
                    }
                }
            }
        }
    }

    describe("drawing short open org.openrndr.shape.contours using org.openrndr.shape.contours()") {
        it("should not throw exceptions") {
            application {
                program {
                    extend {
                        drawer.contours(
                                listOf(
                                    LineSegment(0.0, 0.0, 100.0, 100.0).contour,
                                        LineSegment(100.0, 100.0, 0.0, 0.0).contour))
                        application.exit()
                    }
                }
            }
        }
    }
})

