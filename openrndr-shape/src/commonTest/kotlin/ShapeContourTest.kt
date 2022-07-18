import org.openrndr.math.Vector2
import org.openrndr.math.YPolarity
import org.openrndr.shape.Segment
import org.openrndr.shape.ShapeContour
import kotlin.test.*

internal fun assertEquals(expected: Vector2, actual: Vector2, absoluteTolerance: Double, message: String? = null) {
    assertEquals(expected.x, actual.x, absoluteTolerance, message)
    assertEquals(expected.y, actual.y, absoluteTolerance, message)
}

class ShapeContourTest {

    @Test
    fun shouldCalculateSimplePointAtLength() {
        val rectangleContour = ShapeContour(
            listOf(
                Segment(Vector2(7.0, 6.0), Vector2(507.0, 6.0)),
                Segment(Vector2(507.0, 6.0), Vector2(507.0, 506.0)),
                Segment(Vector2(507.0, 506.0), Vector2(7.0, 506.0)),
                Segment(Vector2(7.0, 506.0), Vector2(7.0, 6.0))
            ), true, YPolarity.CW_NEGATIVE_Y
        )
        assertEquals(Vector2(507.0, 16.0), rectangleContour.pointAtLength(510.0))

        val curveContour = ShapeContour(
            listOf(
                Segment(Vector2(110.0, 150.0), Vector2(25.0, 190.0), Vector2(210.0, 250.0), Vector2(210.0, 30.0))
            ), false, YPolarity.CW_NEGATIVE_Y
        )
        assertEquals(Vector2(105.53567504882812, 152.2501678466797), curveContour.pointAtLength(5.0), 2.0)
        assertEquals(Vector2(162.22564697265625, 170.3757781982422), curveContour.pointAtLength(120.0), 2.0)
        assertEquals(curveContour.segments.first().start, curveContour.pointAtLength(-500.0))
        assertEquals(curveContour.segments.first().end, curveContour.pointAtLength(500.0))
    }

    @Test
    fun shouldCalculateComplexPointAtLength() {
        val poorlyDrawnLine = ShapeContour(
            listOf(
                Segment(Vector2(11.41, 164.15), arrayOf(Vector2(11.41, 164.15)), Vector2(11.03, 159.44)),
                Segment(Vector2(11.03, 159.44), arrayOf(Vector2(10.65, 154.74)), Vector2(9.57, 148.84)),
                Segment(Vector2(9.57, 148.84), arrayOf(Vector2(8.48, 142.95)), Vector2(7.0, 136.78)),
                Segment(Vector2(7.0, 136.78), arrayOf(Vector2(5.52, 130.6)), Vector2(4.25, 124.7)),
                Segment(Vector2(4.25, 124.7), arrayOf(Vector2(2.98, 118.79)), Vector2(2.06, 113.27)),
                Segment(Vector2(2.06, 113.27), arrayOf(Vector2(1.14, 107.75)), Vector2(0.63, 102.66)),
                Segment(Vector2(0.63, 102.66), arrayOf(Vector2(0.12, 97.57)), Vector2(-0.15, 92.97)),
                Segment(Vector2(-0.15, 92.97), arrayOf(Vector2(-0.43, 88.37)), Vector2(-0.58, 83.36)),
                Segment(Vector2(-0.58, 83.36), arrayOf(Vector2(-0.73, 78.35)), Vector2(-0.36, 73.52)),
                Segment(Vector2(-0.36, 73.52), arrayOf(Vector2(-0.0, 68.69)), Vector2(0.65, 64.22)),
                Segment(Vector2(0.65, 64.22), arrayOf(Vector2(1.31, 59.76)), Vector2(3.05, 55.61)),
                Segment(Vector2(3.05, 55.61), arrayOf(Vector2(4.79, 51.47)), Vector2(7.35, 47.81)),
                Segment(Vector2(7.35, 47.81), arrayOf(Vector2(9.91, 44.15)), Vector2(12.9, 40.79)),
                Segment(Vector2(12.9, 40.79), arrayOf(Vector2(15.89, 37.44)), Vector2(19.32, 33.81)),
                Segment(Vector2(19.32, 33.81), arrayOf(Vector2(22.76, 30.18)), Vector2(27.36, 25.92)),
                Segment(Vector2(27.36, 25.92), arrayOf(Vector2(31.96, 21.66)), Vector2(37.91, 17.69)),
                Segment(Vector2(37.91, 17.69), arrayOf(Vector2(43.87, 13.73)), Vector2(50.78, 10.64)),
                Segment(Vector2(50.78, 10.64), arrayOf(Vector2(57.69, 7.54)), Vector2(65.58, 5.6)),
                Segment(Vector2(65.58, 5.6), arrayOf(Vector2(73.47, 3.66)), Vector2(81.19, 2.59)),
                Segment(Vector2(81.19, 2.59), arrayOf(Vector2(88.92, 1.53)), Vector2(95.64, 0.94)),
                Segment(Vector2(95.64, 0.94), arrayOf(Vector2(102.36, 0.35)), Vector2(107.91, 0.48)),
                Segment(Vector2(107.91, 0.48), arrayOf(Vector2(113.46, 0.6)), Vector2(116.95, 0.67)),
                Segment(Vector2(116.95, 0.67), arrayOf(Vector2(120.44, 0.74)), Vector2(123.2, 1.71)),
                Segment(Vector2(123.2, 1.71), arrayOf(Vector2(125.97, 2.67)), Vector2(128.65, 4.6)),
                Segment(Vector2(128.65, 4.6), arrayOf(Vector2(131.33, 6.53)), Vector2(133.46, 8.49)),
                Segment(Vector2(133.46, 8.49), arrayOf(Vector2(135.59, 10.45)), Vector2(138.23, 13.9)),
                Segment(Vector2(138.23, 13.9), arrayOf(Vector2(140.86, 17.34)), Vector2(140.94, 17.49)),
                Segment(Vector2(140.94, 17.49), arrayOf(Vector2(141.02, 17.65)), Vector2(141.05, 17.82)),
                Segment(Vector2(141.05, 17.82), arrayOf(Vector2(141.08, 17.99)), Vector2(141.05, 18.17)),
                Segment(Vector2(141.05, 18.17), arrayOf(Vector2(141.02, 18.34)), Vector2(140.94, 18.5)),
                Segment(Vector2(140.94, 18.5), arrayOf(Vector2(140.86, 18.65)), Vector2(140.73, 18.77)),
                Segment(Vector2(140.73, 18.77), arrayOf(Vector2(140.61, 18.89)), Vector2(140.45, 18.97)),
                Segment(Vector2(140.45, 18.97), arrayOf(Vector2(140.29, 19.04)), Vector2(140.11, 19.06)),
                Segment(Vector2(140.11, 19.06), arrayOf(Vector2(139.94, 19.08)), Vector2(139.77, 19.04)),
                Segment(Vector2(139.77, 19.04), arrayOf(Vector2(139.59, 19.0)), Vector2(139.44, 18.91)),
                Segment(Vector2(139.44, 18.91), arrayOf(Vector2(139.29, 18.82)), Vector2(139.18, 18.69)),
                Segment(Vector2(139.18, 18.69), arrayOf(Vector2(139.07, 18.56)), Vector2(139.0, 18.39)),
                Segment(Vector2(139.0, 18.39), arrayOf(Vector2(138.93, 18.23)), Vector2(138.93, 18.05)),
                Segment(Vector2(138.93, 18.05), arrayOf(Vector2(138.92, 17.88)), Vector2(138.96, 17.71)),
                Segment(Vector2(138.96, 17.71), arrayOf(Vector2(139.01, 17.54)), Vector2(139.11, 17.39)),
                Segment(Vector2(139.11, 17.39), arrayOf(Vector2(139.21, 17.25)), Vector2(139.35, 17.14)),
                Segment(Vector2(139.35, 17.14), arrayOf(Vector2(139.49, 17.04)), Vector2(139.65, 16.98)),
                Segment(Vector2(139.65, 16.98), arrayOf(Vector2(139.82, 16.92)), Vector2(139.99, 16.92)),
                Segment(Vector2(139.99, 16.92), arrayOf(Vector2(140.17, 16.92)), Vector2(140.34, 16.98)),
                Segment(Vector2(140.34, 16.98), arrayOf(Vector2(140.5, 17.04)), Vector2(140.64, 17.14)),
                Segment(Vector2(140.64, 17.14), arrayOf(Vector2(140.78, 17.25)), Vector2(140.88, 17.39)),
                Segment(Vector2(140.88, 17.39), arrayOf(Vector2(140.98, 17.54)), Vector2(141.03, 17.71)),
                Segment(Vector2(141.03, 17.71), arrayOf(Vector2(141.07, 17.88)), Vector2(141.07, 18.05)),
                Segment(Vector2(141.07, 18.05), arrayOf(Vector2(141.06, 18.23)), Vector2(140.99, 18.39)),
                Segment(Vector2(140.99, 18.39), arrayOf(Vector2(140.93, 18.55)), Vector2(140.81, 18.69)),
                Segment(Vector2(140.81, 18.69), arrayOf(Vector2(140.7, 18.82)), Vector2(140.55, 18.91)),
                Segment(Vector2(140.55, 18.91), arrayOf(Vector2(140.4, 19.0)), Vector2(140.23, 19.04)),
                Segment(Vector2(140.23, 19.04), arrayOf(Vector2(140.05, 19.08)), Vector2(139.88, 19.06)),
                Segment(Vector2(139.88, 19.06), arrayOf(Vector2(139.71, 19.04)), Vector2(139.55, 18.97)),
                Segment(Vector2(139.55, 18.97), arrayOf(Vector2(139.39, 18.89)), Vector2(139.26, 18.77)),
                Segment(Vector2(139.26, 18.77), arrayOf(Vector2(139.13, 18.65)), Vector2(139.13, 18.65)),
                Segment(Vector2(139.13, 18.65), arrayOf(Vector2(139.13, 18.65)), Vector2(136.62, 15.35)),
                Segment(Vector2(136.62, 15.35), arrayOf(Vector2(134.12, 12.05)), Vector2(132.09, 10.17)),
                Segment(Vector2(132.09, 10.17), arrayOf(Vector2(130.05, 8.28)), Vector2(127.63, 6.5)),
                Segment(Vector2(127.63, 6.5), arrayOf(Vector2(125.21, 4.71)), Vector2(122.79, 3.81)),
                Segment(Vector2(122.79, 3.81), arrayOf(Vector2(120.36, 2.91)), Vector2(116.89, 2.84)),
                Segment(Vector2(116.89, 2.84), arrayOf(Vector2(113.41, 2.77)), Vector2(107.98, 2.64)),
                Segment(Vector2(107.98, 2.64), arrayOf(Vector2(102.55, 2.52)), Vector2(95.88, 3.1)),
                Segment(Vector2(95.88, 3.1), arrayOf(Vector2(89.21, 3.68)), Vector2(81.6, 4.72)),
                Segment(Vector2(81.6, 4.72), arrayOf(Vector2(73.99, 5.77)), Vector2(66.28, 7.65)),
                Segment(Vector2(66.28, 7.65), arrayOf(Vector2(58.57, 9.53)), Vector2(51.82, 12.54)),
                Segment(Vector2(51.82, 12.54), arrayOf(Vector2(45.07, 15.55)), Vector2(39.25, 19.4)),
                Segment(Vector2(39.25, 19.4), arrayOf(Vector2(33.44, 23.26)), Vector2(28.89, 27.47)),
                Segment(Vector2(28.89, 27.47), arrayOf(Vector2(24.34, 31.68)), Vector2(20.93, 35.29)),
                Segment(Vector2(20.93, 35.29), arrayOf(Vector2(17.53, 38.89)), Vector2(14.62, 42.15)),
                Segment(Vector2(14.62, 42.15), arrayOf(Vector2(11.7, 45.41)), Vector2(9.26, 48.87)),
                Segment(Vector2(9.26, 48.87), arrayOf(Vector2(6.81, 52.33)), Vector2(5.15, 56.21)),
                Segment(Vector2(5.15, 56.21), arrayOf(Vector2(3.49, 60.09)), Vector2(2.85, 64.48)),
                Segment(Vector2(2.85, 64.48), arrayOf(Vector2(2.22, 68.86)), Vector2(1.86, 73.57)),
                Segment(Vector2(1.86, 73.57), arrayOf(Vector2(1.51, 78.28)), Vector2(1.67, 83.26)),
                Segment(Vector2(1.67, 83.26), arrayOf(Vector2(1.84, 88.23)), Vector2(2.13, 92.78)),
                Segment(Vector2(2.13, 92.78), arrayOf(Vector2(2.43, 97.34)), Vector2(2.95, 102.34)),
                Segment(Vector2(2.95, 102.34), arrayOf(Vector2(3.48, 107.35)), Vector2(4.43, 112.81)),
                Segment(Vector2(4.43, 112.81), arrayOf(Vector2(5.38, 118.26)), Vector2(6.7, 124.13)),
                Segment(Vector2(6.7, 124.13), arrayOf(Vector2(8.01, 129.99)), Vector2(9.58, 136.21)),
                Segment(Vector2(9.58, 136.21), arrayOf(Vector2(11.15, 142.44)), Vector2(12.35, 148.45)),
                Segment(Vector2(12.35, 148.45), arrayOf(Vector2(13.55, 154.46)), Vector2(14.07, 159.15)),
                Segment(Vector2(14.07, 159.15), arrayOf(Vector2(14.58, 163.84)), Vector2(14.58, 164.04)),
                Segment(Vector2(14.58, 164.04), arrayOf(Vector2(14.57, 164.23)), Vector2(14.52, 164.41)),
                Segment(Vector2(14.52, 164.41), arrayOf(Vector2(14.47, 164.6)), Vector2(14.38, 164.77)),
                Segment(Vector2(14.38, 164.77), arrayOf(Vector2(14.28, 164.93)), Vector2(14.15, 165.07)),
                Segment(Vector2(14.15, 165.07), arrayOf(Vector2(14.02, 165.21)), Vector2(13.86, 165.32)),
                Segment(Vector2(13.86, 165.32), arrayOf(Vector2(13.7, 165.42)), Vector2(13.52, 165.49)),
                Segment(Vector2(13.52, 165.49), arrayOf(Vector2(13.34, 165.55)), Vector2(13.14, 165.57)),
                Segment(Vector2(13.14, 165.57), arrayOf(Vector2(12.95, 165.59)), Vector2(12.76, 165.56)),
                Segment(Vector2(12.76, 165.56), arrayOf(Vector2(12.57, 165.53)), Vector2(12.4, 165.46)),
                Segment(Vector2(12.4, 165.46), arrayOf(Vector2(12.22, 165.39)), Vector2(12.06, 165.27)),
                Segment(Vector2(12.06, 165.27), arrayOf(Vector2(11.91, 165.16)), Vector2(11.78, 165.01)),
                Segment(Vector2(11.78, 165.01), arrayOf(Vector2(11.66, 164.87)), Vector2(11.58, 164.69)),
                Segment(Vector2(11.58, 164.69), arrayOf(Vector2(11.49, 164.52)), Vector2(11.45, 164.33)),
                Segment(Vector2(11.45, 164.33), arrayOf(), Vector2(11.41, 164.15))
            ), false, YPolarity.CW_NEGATIVE_Y
        )

        val point = poorlyDrawnLine.pointAtLength(100.0)
        // These are the coordinates which Firefox 103 reports for an equivalent SVG and they
        // seem to be consistent across browsers with a tolerance as low as 0.0001.
        assertEquals(0.5043081045150757, point.x, 0.05)
        assertEquals(65.25358581542969, point.y, 0.05)
    }
}