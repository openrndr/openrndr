//import org.openrndr.math.Vector1
//import org.openrndr.shape.Segment1D
//import org.openrndr.shape.internal.BezierCubicSamplerT
//import kotlin.test.Test
//
//class Segment1DTest {
//
//    @Test
//    fun shouldHaveSub() {
//        val s1 = Segment1D(0.0, 10.0, 20.0, 30.0)
//        val a = s1.sub(0.0, 0.5)
//        val b = s1.sub(0.5, 1.0)
//        println(a)
//        println(b)
//    }
//
//    @Test
//    fun shouldHaveAdaptivePositions() {
//        val s1 = Segment1D(0.0, 10.0, 20.0, 30.0)
//        val a = s1.sub(0.0, 0.5)
//        val b = s1.sub(0.5, 1.0)
//        println(a)
//        println(b)
//        val bc = BezierCubicSamplerT<Vector1>()
//        bc.distanceTolerance = 25.0
//        val samples = bc.sample(Vector1(s1.start), Vector1(s1.control[0]), Vector1(s1.control[1]), Vector1(s1.end))
//        println(samples)
//
//
//    }
//}