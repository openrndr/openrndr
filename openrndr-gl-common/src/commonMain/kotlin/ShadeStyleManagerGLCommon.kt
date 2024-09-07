package org.openrndr.internal.glcommon

import org.openrndr.draw.*
import org.openrndr.internal.Driver


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

    private var defaultShader: Shader? = null
    private val shaders = mutableMapOf<ShadeStructure, Shader>()

    override fun shader(
        style: ShadeStyle?,
        vertexFormats: List<VertexFormat>,
        instanceFormats: List<VertexFormat>
    ): Shader {
        val outputInstanceFormats = instanceFormats + (style?.attributes
            ?: emptyList()).map { it.vertexFormat }

        fun String.prependConfig(type: ShaderType): String = """${Driver.instance.shaderConfiguration(type)}
$this"""
        if (style == null) {
            return run {
                if (defaultShader == null) {
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
                    //(defaultShader as ShaderGL3).userShader = false
                }
                defaultShader!!
            }
        } else {
            return run {

                val structure = structureFromShadeStyle(style, vertexFormats, outputInstanceFormats)
                val shader = shaders.getOrPut(structure) {
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
                dispatchBufferBindings(style, shader)
                dispatchParameters(style, shader)
                dispatchImageBindings(style, shader)
                shader
            }
        }
    }
}