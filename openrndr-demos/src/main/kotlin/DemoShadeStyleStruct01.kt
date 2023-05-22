import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Struct
import org.openrndr.draw.parameter
import org.openrndr.draw.shadeStyle
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.buildTransform

fun main() = application {
    program {
        class CustomStruct : Struct<CustomStruct>() {
            var color by field<ColorRGBa>()
            var colors by arrayField<ColorRGBa>(1)
        }
        class StructWithStructField: Struct<StructWithStructField>() {
            var structField by field<CustomStruct>()
            var structFields by arrayField<CustomStruct>(1)
        }

        class Light : Struct<Light>() {
            var color by field<ColorRGBa>()
        }


        extend {
            val cs = CustomStruct()
            cs.color = ColorRGBa.RED
            cs.colors = arrayOf(ColorRGBa.BLUE)

            val cwsf = StructWithStructField()
            cwsf.structField = cs
            cwsf.structFields = arrayOf(cs)


            val lights = arrayOf(Light(), Light())
            lights[0].color = ColorRGBa.PINK
            lights[1].color = ColorRGBa.CYAN

            drawer.stroke = ColorRGBa.GREEN.opacify(0.5)

            drawer.strokeWeight = 4.0
            drawer.model = buildTransform {
                translate(drawer.bounds.center)
                scale(1.0 + mouse.position.y / 100.0)
                rotate(Vector3.UNIT_Z, 10.0 * seconds)
                translate(-drawer.bounds.center)
            }
            drawer.shadeStyle = shadeStyle {
                fragmentTransform = """
                    //x_fill = p_cwsf.structFields[0].colors[0];
                    x_fill = p_lights[1].color;
                """.trimIndent()
                parameter("cwsf", cwsf)
                parameter("lights", lights)
            }
            drawer.circle(drawer.bounds.center - Vector2(100.0, 0.0), 100.0)
            drawer.circle(drawer.bounds.center + Vector2(100.0, 0.0), 100.0)
        }
    }
}