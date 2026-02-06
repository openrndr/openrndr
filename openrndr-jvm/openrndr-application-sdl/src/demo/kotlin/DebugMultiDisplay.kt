import org.openrndr.application

fun main() {
    System.setProperty("org.openrndr.gl3.gl_type", "gles")
    application {
        configure {
            width = 720
            height = 720
            display = displays[0]
            windowResizable = true
        }
        program {
            extend {
//                println(RenderTarget.active.contentScale)
//                println("${RenderTarget.active.width}, ${RenderTarget.active.height}")
                drawer.circle(drawer.bounds.center, 100.0)
            }
        }
    }
}