package org.openrndr.internal.glcommon

import io.github.oshai.kotlinlogging.KotlinLogging
import org.openrndr.draw.*
import org.openrndr.internal.Driver

private val logger = KotlinLogging.logger {  }
class ShadeStyleManagerGLCommon(
    name: String,
    val vsGenerator: (ShadeStructure) -> String,
    val tcsGenerator: ((ShadeStructure) -> String)?,
    val tesGenerator: ((ShadeStructure) -> String)?,
    val gsGenerator: ((ShadeStructure) -> String)?,
    val fsGenerator: (ShadeStructure) -> String
) : ShadeStyleManager(name), StyleManagerDispatchUniform, StyleManagerDispatchImageBindings,
    StyleManagerDispatchBufferBindings {

    override var imageIndex: Int = 0
    override var textureIndex: Int = 0
    override val uniformNames = UniformNames()

    private var defaultShader: Shader? = null
    private val shaders = mutableMapOf<ShadeStructure, Shader>()

    /** the structure and the shader of the last draw, styles are often drawn many times in a row */
    private var lastStructure: ShadeStructure? = null
    private var lastShader: Shader? = null

    /** the instance formats with the formats of the attributes of [style], [instanceFormats] when it has none */
    private fun outputInstanceFormats(style: ShadeStyle?, instanceFormats: List<VertexFormat>): List<VertexFormat> {
        val attributes = style?.attributes
        if (attributes.isNullOrEmpty()) return instanceFormats
        return instanceFormats + attributes.map { it.vertexFormat }
    }

    override fun shader(
        shadeStyle: ShadeStyle?,
        vertexFormats: List<VertexFormat>,
        instanceFormats: List<VertexFormat>
    ): Shader {
        // dynamic parameters are read through generated resolver functions
        val style = shadeStyle?.resolveDynamicParameters()

        fun String.prependConfig(type: ShaderType): String = """${Driver.instance.shaderConfiguration(type)}
$this"""
        if (style == null) {
            return run {
                if (defaultShader == null) {
                    val outputInstanceFormats = outputInstanceFormats(null, instanceFormats)
                    logger.debug { "Creating default shader $name" }
                    val structure = structureFromShadeStyle(style, vertexFormats, outputInstanceFormats)
                    defaultShader = Shader.createFromCode(
                        vsCode = vsGenerator(structure).prependConfig(ShaderType.VERTEX),
                        tcsCode = tcsGenerator?.invoke(structure)?.prependConfig(ShaderType.TESSELLATION_CONTROL),
                        tesCode = tesGenerator?.invoke(structure)?.prependConfig(ShaderType.TESSELLATION_EVALUATION),
                        gsCode = gsGenerator?.invoke(structure)?.prependConfig(ShaderType.GEOMETRY),
                        fsCode = fsGenerator(structure).prependConfig(ShaderType.FRAGMENT),
                        name = "shade-style-default:$name",
                        session = Session.root
                    )
                    logger.debug { "$name: $defaultShader" }
                }
                defaultShader ?: error("Failed to create default shader '$name'")
            }
        } else {
            return run {
                val outputInstanceFormats = outputInstanceFormats(style, instanceFormats)
                val structure = structureFromShadeStyle(style, vertexFormats, outputInstanceFormats)
                val cached = if (structure === lastStructure) lastShader else null
                val shader = cached ?: shaders.getOrPut(structure) {
                    try {
                        Shader.createFromCode(
                            vsCode = vsGenerator(structure).prependConfig(ShaderType.VERTEX),
                            tcsCode = tcsGenerator?.invoke(structure)?.prependConfig(ShaderType.TESSELLATION_CONTROL),
                            tesCode = tesGenerator?.invoke(structure)?.prependConfig(ShaderType.TESSELLATION_EVALUATION),
                            gsCode = gsGenerator?.invoke(structure)?.prependConfig(ShaderType.GEOMETRY),
                            fsCode = fsGenerator(structure).prependConfig(ShaderType.FRAGMENT),
                            name = "shade-style-custom:$name-${structure.hashCode()}",
                            session = Session.root
                        )
                    } catch (e: Throwable) {
                        if (ignoreShadeStyleErrors) {
                            shader(null, vertexFormats, outputInstanceFormats)
                        } else {
                            throw e
                        }
                    }
                }
                lastStructure = structure
                lastShader = shader
                shader.begin()
                dispatchBufferBindings(style, shader)
                dispatchParameters(style, shader, shader.textureBindings)
                dispatchImageBindings(style, shader)
                shader
            }
        }
    }
}