package org.openrndr.draw

/**
 * A collection of granules, or template functions for writing stylable shaders in GLSL. ShadeStyleGLSL
 * is used in OPENRNDR's shader generators but is exposed to the user such that they too can write shader generators.
 */
expect class ShadeStyleGLSL {
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
            screenSize: String = "u_viewDimensions",
            contourPosition: String = "0.0",
            boundsPosition: String = "vec3(0.0)",
            boundsSize: String = "vec3(0.0)"
        ): String

        /**
         * This granule is used inside the main() function of vertex shaders to set up
         * constants that are part of the shade style language.
         */
        fun vertexMainConstants(
            instance: String = "gl_InstanceID",
            element: String = "0"): String

        /**
         * This granule is used in the preamble of a fragment shader. It sets up the declarations of
         * in-varyings holding transformations of position and normal.
         */
        val transformVaryingIn: String

        /**
         * This granule is used in the preamble of a vertex shader. It sets up the declarations of
         * out-varyings holding transformations of position and normal.
         */
        val transformVaryingOut: String

        /**
         * This granule is used in the main function of a vertex shader. It sets up declarations
         * of transformable variables in the shade style language. It is used right before [ShadeStructure.vertexTransform]
         * is inserted into the shader template.
         */
        val preVertexTransform: String

        /**
         * This granule is used in the main function of a vertex shader. It assigns values
         * to out-varyings declared in [transformVaryingOut]. It is used right after [ShadeStructure.vertexTransform]
         * is inserted into the shader template.
         */
        val postVertexTransform: String

        /**
         * This granule is to set up definitions for primitive types.
         * @param type type of the primitive, users would pass in "d_custom"
         */
        fun primitiveTypes(type: String): String

        /**
         * This granule is used to set up [Drawer] uniform declarations. It declares uniforms for
         * transformations and [DrawStyle]. This is used in fragment and vertex shaders.
         */
        fun drawerUniforms(contextBlock: Boolean = true, styleBlock: Boolean = true): String


    }
}