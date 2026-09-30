import org.openrndr.application
import org.openrndr.draw.Session

fun main() = application {
    program {
        val stats = Session.statistics.toString()
            .substringAfter('(').dropLast(1)
            .replace("=", ": ")
            .split(", ")

        println()
        println(stats)
        println()

        extend {
//            writer {
//                box = drawer.bounds.offsetEdges(-50.0)
//                text(stats)
//            }
        }
    }
}