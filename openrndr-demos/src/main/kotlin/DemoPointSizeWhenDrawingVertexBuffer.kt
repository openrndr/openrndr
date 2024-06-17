import org.openrndr.application
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.shadeStyle
import org.openrndr.draw.vertexBuffer
import org.openrndr.draw.vertexFormat
import org.openrndr.math.Vector3
import kotlin.math.sin

fun main() {
    application {
        program {
            val points = vertexBuffer(
                vertexFormat {
                    position(3)
                },
                vertexCount = 1
            )
            points.put {
                write(Vector3(width / 2.0, height / 2.0, 0.0))
            }
            val style = shadeStyle {
                vertexTransform = """
                    x_pointSize = p_pointSize;
                """.trimIndent()
                fragmentTransform = """
                    // TODO in the future it should be rather c_boundsPosition instead of gl_PointCoord
                    vec2 pointPosition = (gl_PointCoord - .5) * 2.0;
                    float distance = length(pointPosition);
                    float luma = smoothstep(1.0, .7, distance);
                    luma *= va_pointSize / 100.0;
                    x_fill.rgb = vec3(luma);
                """.trimIndent()
            }
            extend {
                style.parameter("pointSize", (sin(seconds) * .5 + .5) * 99.0 + 1.0)
                drawer.shadeStyle = style
                drawer.vertexBuffer(points, DrawPrimitive.POINTS)
            }
        }
    }
}
