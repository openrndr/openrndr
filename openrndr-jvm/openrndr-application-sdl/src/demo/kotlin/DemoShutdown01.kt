import org.openrndr.application
import org.openrndr.window

fun main() {
    application {

        program {
            ended.listen {
                println("hello this is the end")
            }

            window {
                program.ended.listen {
                    println("hello this is the end of the window 1")
                }
            }

            window {
                program.ended.listen {
                    println("hello this is the end of the window 2")
                }
            }
        }
    }
}