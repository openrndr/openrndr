import org.intellij.lang.annotations.Language
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*

/**
 * This demo creates an array of textures (layers)
 * and populates them using a compute shader when the program starts.
 *
 * Inside the animation loop it draws one of the layers.
 */
fun main() {
    application {
        val textureSize = 256
        val numLayers = 60

        configure {
            width = textureSize
            height = textureSize
        }

        program {
            val arrayTex = arrayTexture(
                textureSize,
                textureSize,
                numLayers,
                ColorFormat.RGBa,
                ColorType.FLOAT32
            )

            /**
             * A compute shader that populates all layers with
             * transparent pixels on the left side,
             * a pink wave on the right side of the window.
             * The split between both areas is at a horizontal position that
             * depends on the layer number.
             */
            @Language("GLSL")
            val glsl = """
                #version 450
                layout(local_size_x=8, local_size_y=8) in;
                layout(rgba32f) uniform writeonly image2DArray writeTex;
                uniform int numLayers;
                void main() {
                    vec4 pink = vec4(1.0, 0.753, 0.796, 1.0);
                    for (int layer=0; layer<numLayers; layer++) {
                        ivec3 coords = ivec3(gl_GlobalInvocationID.x, 
                                             gl_GlobalInvocationID.y, 
                                             layer);
                        float alpha = sin(coords.y * 0.1 + layer) * 0.5 + 0.5;
                        alpha *= step(layer, coords.x);
                        imageStore(writeTex, coords, pink * alpha);
                    }
                }
                """.trimIndent()
            val cs = ComputeShader.fromCode(glsl, "arrayTextureComp")

            // Populate layers once using a compute shader
            cs.uniform("numLayers", numLayers)
            cs.image(
                "writeTex", 0,
                arrayTex.imageBinding(0, ImageAccess.WRITE)
            )
            cs.execute(textureSize, textureSize, 1)

            extend {
                drawer.clear(ColorRGBa.WHITE)
                // Draw a layer. The layer ID equals the mouse x position.
                val currentLayer = mouse.position.x.toInt() % numLayers
                drawer.image(arrayTex, currentLayer)

                // Draw a helper line
                drawer.lineSegment(
                    currentLayer * 1.0,
                    0.0,
                    currentLayer * 1.0,
                    height * 1.0
                )
            }
        }
    }
}