import org.openrndr.application

fun main() {
    application {
        program {
            extend {
                val start = System.currentTimeMillis()
                for (i in 0 until 10_000) {
                    drawer.rectangle(0.0, 0.0, 100.0, 100.0)
                }
                val end = System.currentTimeMillis()
                println("${end-start}ms")
                if (frameCount == 1000) {
                    application.exit()
                }
            }
        }
    }
}