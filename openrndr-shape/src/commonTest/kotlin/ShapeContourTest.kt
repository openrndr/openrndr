import org.openrndr.math.Vector2
import org.openrndr.math.YPolarity
import org.openrndr.shape.Circle
import org.openrndr.shape.Segment2D
import org.openrndr.shape.ShapeContour
import kotlin.test.*

// TODO: Move this to a dedicated package
internal fun assertEquals(expected: Vector2, actual: Vector2, absoluteTolerance: Double, message: String? = null) {
    assertEquals(expected.x, actual.x, absoluteTolerance, message)
    assertEquals(expected.y, actual.y, absoluteTolerance, message)
}

class ShapeContourTest {


    @Test
    fun shouldCalculateAdaptivePositionsWithT() {
        val c = Circle(0.0, 0.0, 200.0).contour
        val points = c.adaptivePositionsWithT()
        assertEquals(0.0, points.first().second)
        for (i in points.indices) {
            val tp = c.position(points[i].second)
            assertTrue(tp.distanceTo(points[i].first) < 0.05)
        }
    }

    @Test
    fun shouldCalculateEquidistantPositionsWithT() {
        val c = Circle(0.0, 0.0, 200.0).contour
        val points = c.equidistantPositionsWithT(20)
        assertEquals(0.0, points.first().second)
        for (i in 0 until points.size) {
            val tp = c.position(points[i].second)
            assertTrue(tp.distanceTo(points[i].first) < 0.05)
        }

    }

    @Test
    fun shouldCalculateTValues() {
        val positionsWithT = Circle(0.0, 0.0, 200.0).contour.adaptivePositionsWithT()
        assertEquals(0.0, positionsWithT.first().second)
        assertEquals(1.0, positionsWithT.last().second)
    }

    @Test
    fun shouldCalculateSimplePointAtLength() {
        val rectangleContour = ShapeContour(
            listOf(
                Segment2D(Vector2(7.0, 6.0), Vector2(507.0, 6.0)),
                Segment2D(Vector2(507.0, 6.0), Vector2(507.0, 506.0)),
                Segment2D(Vector2(507.0, 506.0), Vector2(7.0, 506.0)),
                Segment2D(Vector2(7.0, 506.0), Vector2(7.0, 6.0))
            ), true, YPolarity.CW_NEGATIVE_Y
        )
        assertEquals(Vector2(507.0, 16.0), rectangleContour.pointAtLength(510.0, 0.0001))

        val curveContour = ShapeContour(
            listOf(
                Segment2D(Vector2(110.0, 150.0), Vector2(25.0, 190.0), Vector2(210.0, 250.0), Vector2(210.0, 30.0))
            ), false, YPolarity.CW_NEGATIVE_Y
        )
        assertEquals(Vector2(105.53567504882812, 152.2501678466797), curveContour.pointAtLength(5.0, 0.0001), 0.001)
        assertEquals(Vector2(162.22564697265625, 170.3757781982422), curveContour.pointAtLength(120.0, 0.0001), 0.001)
        assertEquals(curveContour.segments.first().start, curveContour.pointAtLength(-500.0, 0.0001))
        assertEquals(curveContour.segments.first().end, curveContour.pointAtLength(500.0, 0.0001))
    }

    @Test
    fun shouldCalculateComplexPointAtLength() {
        val poorlyDrawnLine = ShapeContour(
            listOf(
                Segment2D(Vector2(11.41, 164.15), listOf(Vector2(11.41, 164.15)), Vector2(11.03, 159.44)),
                Segment2D(Vector2(11.03, 159.44), listOf(Vector2(10.65, 154.74)), Vector2(9.57, 148.84)),
                Segment2D(Vector2(9.57, 148.84), listOf(Vector2(8.48, 142.95)), Vector2(7.0, 136.78)),
                Segment2D(Vector2(7.0, 136.78), listOf(Vector2(5.52, 130.6)), Vector2(4.25, 124.7)),
                Segment2D(Vector2(4.25, 124.7), listOf(Vector2(2.98, 118.79)), Vector2(2.06, 113.27)),
                Segment2D(Vector2(2.06, 113.27), listOf(Vector2(1.14, 107.75)), Vector2(0.63, 102.66)),
                Segment2D(Vector2(0.63, 102.66), listOf(Vector2(0.12, 97.57)), Vector2(-0.15, 92.97)),
                Segment2D(Vector2(-0.15, 92.97), listOf(Vector2(-0.43, 88.37)), Vector2(-0.58, 83.36)),
                Segment2D(Vector2(-0.58, 83.36), listOf(Vector2(-0.73, 78.35)), Vector2(-0.36, 73.52)),
                Segment2D(Vector2(-0.36, 73.52), listOf(Vector2(-0.0, 68.69)), Vector2(0.65, 64.22)),
                Segment2D(Vector2(0.65, 64.22), listOf(Vector2(1.31, 59.76)), Vector2(3.05, 55.61)),
                Segment2D(Vector2(3.05, 55.61), listOf(Vector2(4.79, 51.47)), Vector2(7.35, 47.81)),
                Segment2D(Vector2(7.35, 47.81), listOf(Vector2(9.91, 44.15)), Vector2(12.9, 40.79)),
                Segment2D(Vector2(12.9, 40.79), listOf(Vector2(15.89, 37.44)), Vector2(19.32, 33.81)),
                Segment2D(Vector2(19.32, 33.81), listOf(Vector2(22.76, 30.18)), Vector2(27.36, 25.92)),
                Segment2D(Vector2(27.36, 25.92), listOf(Vector2(31.96, 21.66)), Vector2(37.91, 17.69)),
                Segment2D(Vector2(37.91, 17.69), listOf(Vector2(43.87, 13.73)), Vector2(50.78, 10.64)),
                Segment2D(Vector2(50.78, 10.64), listOf(Vector2(57.69, 7.54)), Vector2(65.58, 5.6)),
                Segment2D(Vector2(65.58, 5.6), listOf(Vector2(73.47, 3.66)), Vector2(81.19, 2.59)),
                Segment2D(Vector2(81.19, 2.59), listOf(Vector2(88.92, 1.53)), Vector2(95.64, 0.94)),
                Segment2D(Vector2(95.64, 0.94), listOf(Vector2(102.36, 0.35)), Vector2(107.91, 0.48)),
                Segment2D(Vector2(107.91, 0.48), listOf(Vector2(113.46, 0.6)), Vector2(116.95, 0.67)),
                Segment2D(Vector2(116.95, 0.67), listOf(Vector2(120.44, 0.74)), Vector2(123.2, 1.71)),
                Segment2D(Vector2(123.2, 1.71), listOf(Vector2(125.97, 2.67)), Vector2(128.65, 4.6)),
                Segment2D(Vector2(128.65, 4.6), listOf(Vector2(131.33, 6.53)), Vector2(133.46, 8.49)),
                Segment2D(Vector2(133.46, 8.49), listOf(Vector2(135.59, 10.45)), Vector2(138.23, 13.9)),
                Segment2D(Vector2(138.23, 13.9), listOf(Vector2(140.86, 17.34)), Vector2(140.94, 17.49)),
                Segment2D(Vector2(140.94, 17.49), listOf(Vector2(141.02, 17.65)), Vector2(141.05, 17.82)),
                Segment2D(Vector2(141.05, 17.82), listOf(Vector2(141.08, 17.99)), Vector2(141.05, 18.17)),
                Segment2D(Vector2(141.05, 18.17), listOf(Vector2(141.02, 18.34)), Vector2(140.94, 18.5)),
                Segment2D(Vector2(140.94, 18.5), listOf(Vector2(140.86, 18.65)), Vector2(140.73, 18.77)),
                Segment2D(Vector2(140.73, 18.77), listOf(Vector2(140.61, 18.89)), Vector2(140.45, 18.97)),
                Segment2D(Vector2(140.45, 18.97), listOf(Vector2(140.29, 19.04)), Vector2(140.11, 19.06)),
                Segment2D(Vector2(140.11, 19.06), listOf(Vector2(139.94, 19.08)), Vector2(139.77, 19.04)),
                Segment2D(Vector2(139.77, 19.04), listOf(Vector2(139.59, 19.0)), Vector2(139.44, 18.91)),
                Segment2D(Vector2(139.44, 18.91), listOf(Vector2(139.29, 18.82)), Vector2(139.18, 18.69)),
                Segment2D(Vector2(139.18, 18.69), listOf(Vector2(139.07, 18.56)), Vector2(139.0, 18.39)),
                Segment2D(Vector2(139.0, 18.39), listOf(Vector2(138.93, 18.23)), Vector2(138.93, 18.05)),
                Segment2D(Vector2(138.93, 18.05), listOf(Vector2(138.92, 17.88)), Vector2(138.96, 17.71)),
                Segment2D(Vector2(138.96, 17.71), listOf(Vector2(139.01, 17.54)), Vector2(139.11, 17.39)),
                Segment2D(Vector2(139.11, 17.39), listOf(Vector2(139.21, 17.25)), Vector2(139.35, 17.14)),
                Segment2D(Vector2(139.35, 17.14), listOf(Vector2(139.49, 17.04)), Vector2(139.65, 16.98)),
                Segment2D(Vector2(139.65, 16.98), listOf(Vector2(139.82, 16.92)), Vector2(139.99, 16.92)),
                Segment2D(Vector2(139.99, 16.92), listOf(Vector2(140.17, 16.92)), Vector2(140.34, 16.98)),
                Segment2D(Vector2(140.34, 16.98), listOf(Vector2(140.5, 17.04)), Vector2(140.64, 17.14)),
                Segment2D(Vector2(140.64, 17.14), listOf(Vector2(140.78, 17.25)), Vector2(140.88, 17.39)),
                Segment2D(Vector2(140.88, 17.39), listOf(Vector2(140.98, 17.54)), Vector2(141.03, 17.71)),
                Segment2D(Vector2(141.03, 17.71), listOf(Vector2(141.07, 17.88)), Vector2(141.07, 18.05)),
                Segment2D(Vector2(141.07, 18.05), listOf(Vector2(141.06, 18.23)), Vector2(140.99, 18.39)),
                Segment2D(Vector2(140.99, 18.39), listOf(Vector2(140.93, 18.55)), Vector2(140.81, 18.69)),
                Segment2D(Vector2(140.81, 18.69), listOf(Vector2(140.7, 18.82)), Vector2(140.55, 18.91)),
                Segment2D(Vector2(140.55, 18.91), listOf(Vector2(140.4, 19.0)), Vector2(140.23, 19.04)),
                Segment2D(Vector2(140.23, 19.04), listOf(Vector2(140.05, 19.08)), Vector2(139.88, 19.06)),
                Segment2D(Vector2(139.88, 19.06), listOf(Vector2(139.71, 19.04)), Vector2(139.55, 18.97)),
                Segment2D(Vector2(139.55, 18.97), listOf(Vector2(139.39, 18.89)), Vector2(139.26, 18.77)),
                Segment2D(Vector2(139.26, 18.77), listOf(Vector2(139.13, 18.65)), Vector2(139.13, 18.65)),
                Segment2D(Vector2(139.13, 18.65), listOf(Vector2(139.13, 18.65)), Vector2(136.62, 15.35)),
                Segment2D(Vector2(136.62, 15.35), listOf(Vector2(134.12, 12.05)), Vector2(132.09, 10.17)),
                Segment2D(Vector2(132.09, 10.17), listOf(Vector2(130.05, 8.28)), Vector2(127.63, 6.5)),
                Segment2D(Vector2(127.63, 6.5), listOf(Vector2(125.21, 4.71)), Vector2(122.79, 3.81)),
                Segment2D(Vector2(122.79, 3.81), listOf(Vector2(120.36, 2.91)), Vector2(116.89, 2.84)),
                Segment2D(Vector2(116.89, 2.84), listOf(Vector2(113.41, 2.77)), Vector2(107.98, 2.64)),
                Segment2D(Vector2(107.98, 2.64), listOf(Vector2(102.55, 2.52)), Vector2(95.88, 3.1)),
                Segment2D(Vector2(95.88, 3.1), listOf(Vector2(89.21, 3.68)), Vector2(81.6, 4.72)),
                Segment2D(Vector2(81.6, 4.72), listOf(Vector2(73.99, 5.77)), Vector2(66.28, 7.65)),
                Segment2D(Vector2(66.28, 7.65), listOf(Vector2(58.57, 9.53)), Vector2(51.82, 12.54)),
                Segment2D(Vector2(51.82, 12.54), listOf(Vector2(45.07, 15.55)), Vector2(39.25, 19.4)),
                Segment2D(Vector2(39.25, 19.4), listOf(Vector2(33.44, 23.26)), Vector2(28.89, 27.47)),
                Segment2D(Vector2(28.89, 27.47), listOf(Vector2(24.34, 31.68)), Vector2(20.93, 35.29)),
                Segment2D(Vector2(20.93, 35.29), listOf(Vector2(17.53, 38.89)), Vector2(14.62, 42.15)),
                Segment2D(Vector2(14.62, 42.15), listOf(Vector2(11.7, 45.41)), Vector2(9.26, 48.87)),
                Segment2D(Vector2(9.26, 48.87), listOf(Vector2(6.81, 52.33)), Vector2(5.15, 56.21)),
                Segment2D(Vector2(5.15, 56.21), listOf(Vector2(3.49, 60.09)), Vector2(2.85, 64.48)),
                Segment2D(Vector2(2.85, 64.48), listOf(Vector2(2.22, 68.86)), Vector2(1.86, 73.57)),
                Segment2D(Vector2(1.86, 73.57), listOf(Vector2(1.51, 78.28)), Vector2(1.67, 83.26)),
                Segment2D(Vector2(1.67, 83.26), listOf(Vector2(1.84, 88.23)), Vector2(2.13, 92.78)),
                Segment2D(Vector2(2.13, 92.78), listOf(Vector2(2.43, 97.34)), Vector2(2.95, 102.34)),
                Segment2D(Vector2(2.95, 102.34), listOf(Vector2(3.48, 107.35)), Vector2(4.43, 112.81)),
                Segment2D(Vector2(4.43, 112.81), listOf(Vector2(5.38, 118.26)), Vector2(6.7, 124.13)),
                Segment2D(Vector2(6.7, 124.13), listOf(Vector2(8.01, 129.99)), Vector2(9.58, 136.21)),
                Segment2D(Vector2(9.58, 136.21), listOf(Vector2(11.15, 142.44)), Vector2(12.35, 148.45)),
                Segment2D(Vector2(12.35, 148.45), listOf(Vector2(13.55, 154.46)), Vector2(14.07, 159.15)),
                Segment2D(Vector2(14.07, 159.15), listOf(Vector2(14.58, 163.84)), Vector2(14.58, 164.04)),
                Segment2D(Vector2(14.58, 164.04), listOf(Vector2(14.57, 164.23)), Vector2(14.52, 164.41)),
                Segment2D(Vector2(14.52, 164.41), listOf(Vector2(14.47, 164.6)), Vector2(14.38, 164.77)),
                Segment2D(Vector2(14.38, 164.77), listOf(Vector2(14.28, 164.93)), Vector2(14.15, 165.07)),
                Segment2D(Vector2(14.15, 165.07), listOf(Vector2(14.02, 165.21)), Vector2(13.86, 165.32)),
                Segment2D(Vector2(13.86, 165.32), listOf(Vector2(13.7, 165.42)), Vector2(13.52, 165.49)),
                Segment2D(Vector2(13.52, 165.49), listOf(Vector2(13.34, 165.55)), Vector2(13.14, 165.57)),
                Segment2D(Vector2(13.14, 165.57), listOf(Vector2(12.95, 165.59)), Vector2(12.76, 165.56)),
                Segment2D(Vector2(12.76, 165.56), listOf(Vector2(12.57, 165.53)), Vector2(12.4, 165.46)),
                Segment2D(Vector2(12.4, 165.46), listOf(Vector2(12.22, 165.39)), Vector2(12.06, 165.27)),
                Segment2D(Vector2(12.06, 165.27), listOf(Vector2(11.91, 165.16)), Vector2(11.78, 165.01)),
                Segment2D(Vector2(11.78, 165.01), listOf(Vector2(11.66, 164.87)), Vector2(11.58, 164.69)),
                Segment2D(Vector2(11.58, 164.69), listOf(Vector2(11.49, 164.52)), Vector2(11.45, 164.33)),
                Segment2D(Vector2(11.45, 164.33), listOf(), Vector2(11.41, 164.15))
            ), false, YPolarity.CW_NEGATIVE_Y
        )

        val point = poorlyDrawnLine.pointAtLength(100.0, 0.0001)
        // These are the coordinates which Firefox 103 reports for an equivalent SVG and they
        // seem to be consistent across browsers with a tolerance as low as 0.0001.
        assertEquals(Vector2(0.5043081045150757, 65.25358581542969), point, 0.005)
    }
}