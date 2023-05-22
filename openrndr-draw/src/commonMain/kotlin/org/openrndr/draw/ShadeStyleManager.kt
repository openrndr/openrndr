package org.openrndr.draw

import org.openrndr.internal.Driver

data class ShadeStructure(
    var structDefinitions: String? = null,
    var uniforms: String? = null,
    var attributes: String? = null,
    var buffers: String? = null,
    var vertexTransform: String? = null,
    var geometryTransform: String? = null,
    var fragmentTransform: String? = null,
    var vertexPreamble: String? = null,
    var geometryPreamble: String? = null,
    var fragmentPreamble: String? = null,
    var outputs: String? = null,
    var varyingOut: String? = null,
    var varyingIn: String? = null,
    var varyingBridge: String? = null,
    var suppressDefaultOutput: Boolean = false
)

abstract class ShadeStyleManager(val name: String) {
    companion object {
        fun fromGenerators(
            name: String,
            vsGenerator: (ShadeStructure) -> String,
            tscGenerator: ((ShadeStructure) -> String)? = null,
            tseGenerator: ((ShadeStructure) -> String)? = null,
            gsGenerator: ((ShadeStructure) -> String)? = null,
            fsGenerator: (ShadeStructure) -> String
        ): ShadeStyleManager {
            return Driver.instance.createShadeStyleManager(
                name,
                vsGenerator = vsGenerator,
                tcsGenerator = tscGenerator,
                tesGenerator = tseGenerator,
                gsGenerator = gsGenerator,
                fsGenerator = fsGenerator
            )
        }
    }

    abstract fun shader(
        style: ShadeStyle?,
        vertexFormats: List<VertexFormat>,
        instanceFormats: List<VertexFormat> = emptyList()
    ): Shader

    fun shader(
        style: ShadeStyle?,
        format: VertexFormat
    ): Shader = shader(style, listOf(format), emptyList())
}