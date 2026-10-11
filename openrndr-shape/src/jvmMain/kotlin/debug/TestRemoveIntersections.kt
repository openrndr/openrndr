import debug.loadBitExact
import org.openrndr.shape.Shape
import org.openrndr.shape.removeSelfIntersections
import java.io.File

fun main() {
    println(File(".").absolutePath)
    val shape = Shape.loadBitExact(File("../slivers.shape"))

    val c = shape.contours[0]
    val k = c.removeSelfIntersections()
    require(k.contours.size == 1) {
        println("${k.contours.size} contours")
    }
}