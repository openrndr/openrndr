import org.openrndr.application
import org.openrndr.draw.shadeStyle

/**
 * Demonstration of ShaderError.glsl creation (in cwd) whenever shader compilation fails.
 */
fun main() {
    application {
        program {
          drawer.shadeStyle = shadeStyle {
                fragmentTransform = """
                    break stuff
                """.trimIndent()
            }
            drawer.circle(width/2.0, height/2.0, 100.0)
        }
    }
}