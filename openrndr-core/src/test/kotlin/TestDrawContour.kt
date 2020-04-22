import org.openrndr.application
import org.openrndr.shape.Circle
import org.openrndr.shape.LineSegment
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TestVertexBufferGL3 : Spek({
    describe("drawing a single contour") {
        application {
            program {
                extend {
                    drawer.contour(Circle(width / 2.0, height / 2.0, 100.0).contour)
                    application.exit()
                }
            }
        }
    }

    describe("drawing contours with a single element") {
        application {
            program {
                extend {
                    drawer.contours(listOf(Circle(width / 2.0, height / 2.0, 100.0).contour))
                    application.exit()
                }
            }
        }
    }

    describe("drawing contours with the same contour twice") {
        application {
            program {
                extend {
                    drawer.contours(
                            listOf(Circle(width / 2.0, height / 2.0, 100.0).contour,
                                    Circle(width / 2.0, height / 2.0, 100.0).contour))
                    application.exit()
                }
            }
        }
    }

//    describe("drawing contours with the same contour twice") {
//        application {
//            program {
//                extend {
//                    drawer.contours(
//                            listOf(LineSegment(0.0, 0.0, 100.0, 100.0).contour,
//                                    LineSegment(100.0, 100.0, 0.0, 0.0).contour))
//                    application.exit()
//                }
//            }
//        }
//    }
})

