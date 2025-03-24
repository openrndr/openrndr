import org.openrndr.application
import org.openrndr.draw.*

/**
 * Create an array of textures (layers)
 * and populate them once using a compute shader.
 *
 * Inside the animation loop draw one of the layers.
 *
 * Useful when you want to have a compute shader that can read from or
 * write into multiple textures.
 *
 * One can update the texture array inside or outside the animation loop.
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
             * A compute shader that populates a texture array with
             * pixels of different brightnesses.
             * The pattern in each layer depends on the layer id.
             */
            val cs = computeStyle {
                computeTransform = """
                    for (int layerId=0; layerId<p_numLayers; layerId++) {
                        // The coordinates of the pixel we want to update
                        ivec3 coords = ivec3(gl_GlobalInvocationID.x, 
                                             gl_GlobalInvocationID.y, 
                                             layerId);
                        // The color for that pixel
                        float bri = sin(float(layerId) * float(coords.x) * 0.01) *
                                    cos(float(layerId) * float(coords.y) * 0.03) * 0.5 + 0.5;
                        
                        // Update the pixel
                        imageStore(p_writeTex, coords, vec4(bri));
                    }                    
                """.trimIndent()
            }

            // Send a uniform to the shader
            cs.parameter("numLayers", numLayers)

            // Bind the array texture we want to write into
            cs.image(
                "writeTex",
                arrayTex.imageBinding(0, ImageAccess.WRITE)
            )

            // Run the compute shader once to update the target array texture
            cs.execute(textureSize, textureSize, 1)

            extend {
                // Draw a layer on every animation frame.
                // The layer ID depends on frameCount.
                drawer.image(arrayTex, (frameCount + 10) % numLayers)
            }
        }
    }
}