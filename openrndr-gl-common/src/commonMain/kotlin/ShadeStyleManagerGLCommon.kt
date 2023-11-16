package org.openrndr.internal.glcommon

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.internal.Driver
import org.openrndr.math.*


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

        fun String.prependConfig(): String = """${Driver.instance.shaderConfiguration()}
$this"""
        if (style == null) {
            return run {
                if (defaultShader == null) {
                    val structure = structureFromShadeStyle(style, vertexFormats, outputInstanceFormats)
                    defaultShader = Shader.createFromCode(
                        vsCode = vsGenerator(structure).prependConfig(),
                        tcsCode = tcsGenerator?.invoke(structure)?.prependConfig(),
                        tesCode = tesGenerator?.invoke(structure)?.prependConfig(),
                        gsCode = gsGenerator?.invoke(structure)?.prependConfig(),
                        fsCode = fsGenerator(structure).prependConfig(),
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
                            vsCode = vsGenerator(structure).prependConfig(),
                            tcsCode = tcsGenerator?.invoke(structure)?.prependConfig(),
                            tesCode = tesGenerator?.invoke(structure)?.prependConfig(),
                            gsCode = gsGenerator?.invoke(structure)?.prependConfig(),
                            fsCode = fsGenerator(structure).prependConfig(),
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