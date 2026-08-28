import org.openrndr.application

fun main() {
    application {
        program {
            window.drop.listen {
                println(it)
            }
        }
    }
}