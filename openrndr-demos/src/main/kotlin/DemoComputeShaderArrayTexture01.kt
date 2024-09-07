import org.openrndr.application
import org.openrndr.draw.*
import org.openrndr.internal.Driver

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
            val glsl = """
                ${Driver.instance.shaderConfiguration(ShaderType.COMPUTE)}
                
                layout(local_size_x=8, local_size_y=8) in;
                layout(rgba32f) uniform writeonly highp image2DArray writeTex;
                uniform int numLayers;
                void main() {
                    for (int layerId=0; layerId<numLayers; layerId++) {
                        // The coordinates of the pixel we want to update
                        ivec3 coords = ivec3(gl_GlobalInvocationID.x, 
                                             gl_GlobalInvocationID.y, 
                                             layerId);
                        // The color for that pixel
                        float bri = sin(float(layerId) * float(coords.x) * 0.01) *
                                    cos(float(layerId) * float(coords.y) * 0.03) * 0.5 + 0.5;
                        
                        // Update the pixel
                        imageStore(writeTex, coords, vec4(bri));
                    }
                }
                """.trimIndent()

            val cs = ComputeShader.fromCode(glsl, "arrayTextureComp")

            // Send a uniform to the shader
            cs.uniform("numLayers", numLayers)

            // Bind the array texture we want to write into
            cs.image(
                "writeTex", 0,
                arrayTex.imageBinding(0, ImageAccess.WRITE)
            )

            // Run the compute shader to update the target array texture
            cs.execute(textureSize, textureSize, 1)

            extend {
                // Draw a layer on every animation frame.
                // The layer ID depends on frameCount.
                drawer.image(arrayTex, (frameCount + 10) % numLayers)
            }
        }
    }
}