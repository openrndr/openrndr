package org.openrndr.draw

import org.intellij.lang.annotations.Language

/**
 * A collection of granules, or template functions for writing stylable shaders in GLSL. ShadeStyleGLSL
 * is used in OPENRNDR's shader generators but is exposed to the user such that they too can write shader generators.
 */
class ShadeStyleGLSL {
    companion object {
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
        fun fragmentMainConstants(
            instance: String = "v_instance",
            element: String = "0",
            screenPosition: String = "gl_FragCoord.xy / u_contentScale",
            contourPosition: String = "0",
            boundsPosition: String = "vec3(0.0)",
            boundsSize: String = "vec3(0.0)"
        ) = """
        |    // -- fragmentConstants
        |    int c_instance = $instance;
        |    int c_element = $element;
        |    vec2 c_screenPosition = $screenPosition;
        |    float c_contourPosition = $contourPosition;
        |    vec3 c_boundsPosition = $boundsPosition;
        |    vec3 c_boundsSize = $boundsSize;""".trimMargin()


        /**
         * This granule is used inside the main() function of vertex shaders to set up
         * constants that are part of the shade style language.
        */
        fun vertexMainConstants(
            instance: String = "gl_InstanceID",
            element: String = "0"
        ) = """
        |    int c_instance = $instance;
        |    int c_element = $element;""".trimMargin()

        /**
         * This granule is used in the preamble of a fragment shader. It sets up the declarations of
         * in-varyings holding transformations of position and normal.
         */
        val transformVaryingIn = """
            // <transform-varying-in> (ShadeStyleGLSL.kt)
            in vec3 v_worldNormal;
            in vec3 v_viewNormal;
            in vec3 v_worldPosition;
            in vec3 v_viewPosition;
            in vec4 v_clipPosition;
            flat in mat4 v_modelNormalMatrix;
            // </transform-varying-in>""".trimIndent()

        /**
         * This granule is used in the preamble of a vertex shader. It sets up the declarations of
         * out-varyings holding transformations of position and normal.
         */
        val transformVaryingOut = """
            // <transform-varying-out> (ShadeStyleGLSL.kt)
            out vec3 v_worldNormal;
            out vec3 v_viewNormal;
            out vec3 v_worldPosition;
            out vec3 v_viewPosition;
            out vec4 v_clipPosition;
            
            flat out mat4 v_modelNormalMatrix;
            // </transform-varying-out>""".trimIndent()

        /**
         * This granule is used in the main function of a vertex shader. It sets up declarations
         * of transformable variables in the shade style language. It is used right before [ShadeStructure.vertexTransform]
         * is inserted into the shader template.
         */
        val preVertexTransform = """
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
        val postVertexTransform = """
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
        fun primitiveTypes(type: String) = """
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
        fun drawerUniforms(contextBlock: Boolean = true, styleBlock: Boolean = true) = """
            |// <drawer-uniforms($contextBlock, $styleBlock)> (ShadeStyleGLSL.kt)
            ${contextBlock.trueOrEmpty {
                """
                |layout(shared) uniform ContextBlock {
                |    uniform mat4 u_modelNormalMatrix;
                |    uniform mat4 u_modelMatrix;
                |    uniform mat4 u_viewNormalMatrix;
                |    uniform mat4 u_viewMatrix;
                |    uniform mat4 u_projectionMatrix;
                |    uniform float u_contentScale;
                |    uniform vec2 u_viewDimensions;
                |};"""
                }
            }
            ${styleBlock.trueOrEmpty {
                """
                |layout(shared) uniform StyleBlock {
                |    uniform vec4 u_fill;
                |    uniform vec4 u_stroke;
                |    uniform float u_strokeWeight;
                |    uniform float[25] u_colorMatrix;
                |};"""
                }
            }
            |// </drawer-uniforms>
            """.trimMargin()
        }
}

private fun Boolean.trueOrEmpty(@Language("GLSL") f: () -> String): String {
    return if (this) f() else ""
}
