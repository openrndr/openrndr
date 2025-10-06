package org.openrndr.draw

/**
 * A collection of granules, or template functions for writing stylable shaders in GLSL. ShadeStyleGLSL
 * is used in OPENRNDR's shader generators but is exposed to the user such that they too can write shader generators.
 */
actual class ShadeStyleGLSL {
    actual companion object {
        /**
         * This granule is used inside the main() function of fragment shaders to set up
         * constants that are part of the shade style language.
         * It sets up:
         * - c_instance
         * - c_element
         * - c_screenPosition
         * - c_contourPosition
         * - c_boundsPosition
         * - c_boundsSize
         */
        actual fun fragmentMainConstants(
            instance: String,
            element: String,
            screenPosition: String,
            screenSize: String,
            contourPosition: String,
            boundsPosition: String,
            boundsSize: String
        ) = """
        |    // -- fragmentConstants
        |    #define c_instance ($instance)
        |    #define c_element ($element)
        |    #define c_screenPosition ($screenPosition)
        |    #define c_screenSize ($screenSize)
        |    #define c_contourPosition ($contourPosition)
        |    #define c_boundsPosition ($boundsPosition)
        |    #define c_boundsSize ($boundsSize)""".trimMargin()


        /**
         * This granule is used inside the main() function of vertex shaders to set up
         * constants that are part of the shade style language.
         */
        actual fun vertexMainConstants(
            instance: String,
            element: String
        ) = """
        |#define c_instance $instance
        |int c_element = $element;""".trimMargin()

        /**
         * This granule is used in the preamble of a fragment shader. It sets up the declarations of
         * in-varyings holding transformations of position and normal.
         */
        actual val transformVaryingIn = """
            // <transform-varying-in> (ShadeStyleGLSL.kt)
            in vec3 v_worldNormal;
            in vec3 v_viewNormal;
            in vec3 v_worldPosition;
            in vec3 v_viewPosition;
            in vec4 v_clipPosition;
            in mat4 v_modelNormalMatrix;
            // </transform-varying-in>""".trimIndent()

        /**
         * This granule is used in the preamble of a vertex shader. It sets up the declarations of
         * out-varyings holding transformations of position and normal.
         */
        actual val transformVaryingOut = """
            // <transform-varying-out> (ShadeStyleGLSL.kt)
            out vec3 v_worldNormal;
            out vec3 v_viewNormal;
            out vec3 v_worldPosition;
            out vec3 v_viewPosition;
            out vec4 v_clipPosition;
            
            out mat4 v_modelNormalMatrix;
            // </transform-varying-out>""".trimIndent()

        /**
         * This granule is used in the main function of a vertex shader. It sets up declarations
         * of transformable variables in the shade style language. It is used right before [ShadeStructure.vertexTransform]
         * is inserted into the shader template.
         */
        actual val preVertexTransform = """
            // <pre-Transform> (ShadeStyleGLSL.kt)
            mat4 x_modelMatrix = u_modelMatrix;
            mat4 x_viewMatrix = u_viewMatrix;
            mat4 x_modelNormalMatrix = u_modelNormalMatrix;
            mat4 x_viewNormalMatrix = u_viewNormalMatrix;
            mat4 x_projectionMatrix = u_projectionMatrix;
            // </pre-transform>""".trimIndent()

        /**
         * This granule is used in the main function of a vertex shader. It assigns values
         * to out-varyings declared in [transformVaryingOut]. It is used right after [ShadeStructure.vertexTransform]
         * is inserted into the shader template.
         */
        actual val postVertexTransform = """
            // <post-transform> (ShadeStyleGLSL.kt)
            v_worldNormal = (x_modelNormalMatrix * vec4(x_normal,0.0)).xyz;
            v_viewNormal = (x_viewNormalMatrix * vec4(v_worldNormal,0.0)).xyz;
            v_worldPosition = (x_modelMatrix * vec4(x_position, 1.0)).xyz;
            v_viewPosition = (x_viewMatrix * vec4(v_worldPosition, 1.0)).xyz;
            v_clipPosition = x_projectionMatrix * vec4(v_viewPosition, 1.0);
            v_modelNormalMatrix = x_modelNormalMatrix;
            // </post-transform>""".trimIndent()

        /**
         * This granule is to set up definitions for primitive types.
         * @param type type of the primitive, users would pass in "d_custom"
         */
        actual fun primitiveTypes(type: String) = """
            // <primitive-types> (ShadeStyleGLSL.kt)
            #define d_vertex_buffer 0
            #define d_image 1
            #define d_circle 2
            #define d_rectangle 3
            #define d_font_image_map 4
            #define d_expansion 5
            #define d_fast_line 6
            #define d_mesh_line 7
            #define d_point 8
            #define d_custom 9
            #define d_primitive $type
            // </primitive-types>
            """.trimIndent()


        /**
         * This granule is used to set up [Drawer] uniform declarations. It declares uniforms for
         * transformations and [DrawStyle]. This is used in fragment and vertex shaders.
         */
        actual fun drawerUniforms(contextBlock: Boolean, styleBlock: Boolean) = """
            |// <drawer-uniforms($contextBlock, $styleBlock)> (ShadeStyleGLSL.kt)
            ${contextBlock.trueOrEmpty {
            """
                |    uniform highp mat4 u_modelNormalMatrix;
                |    uniform highp mat4 u_modelMatrix;
                |    uniform highp mat4 u_viewNormalMatrix;
                |    uniform highp mat4 u_viewMatrix;
                |    uniform highp mat4 u_projectionMatrix;
                |    uniform highp float u_contentScale;
                |    uniform highp float u_modelViewScalingFactor;
                |    uniform highp vec2 u_viewDimensions;
                |"""
            }
        }
            ${styleBlock.trueOrEmpty {
            """
                |    uniform highp vec4 u_fill;
                |    uniform highp vec4 u_stroke;
                |    uniform highp float u_strokeWeight;
                |    uniform highp float u_colorMatrix[25];"""
            }
        }
            |// </drawer-uniforms>
            """.trimMargin()
    }
}

private fun Boolean.trueOrEmpty(f: () -> String): String {
    return if (this) f() else ""
}
