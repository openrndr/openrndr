package org.openrndr.internal.gl3

import mu.KotlinLogging
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.math.*
import org.openrndr.measure

private val logger = KotlinLogging.logger {}

class ShadeStyleManagerGL3(name: String,
                           val vertexShaderGenerator: (ShadeStructure) -> String,
                           val fragmentShaderGenerator: (ShadeStructure) -> String) : ShadeStyleManager(name) {

    private var defaultShader: Shader? = null
    private val shaders = mutableMapOf<ShadeStructure, Shader>()

    override fun shader(style: ShadeStyle?, vertexFormats: List<VertexFormat>, instanceFormats: List<VertexFormat>): Shader {
        val outputInstanceFormats = instanceFormats + (style?.attributes
                ?: emptyList<VertexBuffer>()).map { it.vertexFormat }

        if (style == null) {
            return measure("ShadeStyleManagerGl3.shader-default-shader") {
                if (defaultShader == null) {
                    logger.debug { "creating default shader" }
                    val structure = structureFromShadeStyle(style, vertexFormats, outputInstanceFormats)
                    defaultShader = Shader.createFromCode(vertexShaderGenerator(structure), fragmentShaderGenerator(structure), "shade-style-default:$name", Session.root)
                    (defaultShader as ShaderGL3).userShader = false
                }
                defaultShader!!
            }
        } else {
            return measure("ShadeStyleManagerGL3.shader-custom-shader") {
                val structure = structureFromShadeStyle(style, vertexFormats, outputInstanceFormats)
                val shader = shaders.getOrPut(structure) {
                    try {
                        Shader.createFromCode(vertexShaderGenerator(structure), fragmentShaderGenerator(structure), "shade-style-custom:$name-${structure.hashCode()}", Session.root)
                    } catch (e: Throwable) {
                        if (System.getProperties().containsKey("org.openrndr.ignoreShadeStyleErrors")) {
                            shader(null, vertexFormats, outputInstanceFormats)
                        } else {
                            throw e
                        }
                    }
                }
                (shader as ShaderGL3).userShader = false

                shader.begin()
                var textureIndex = 2
                measure("shadestyle-parameters-to-uniforms") {
                    for (it in style.parameterValues.entries) {
                        when (val value = it.value) {
                            is Int -> shader.uniform("p_${it.key}", value)
                            is Float -> shader.uniform("p_${it.key}", value)
                            is Double -> shader.uniform("p_${it.key}", value)
                            is Matrix44 -> shader.uniform("p_${it.key}", value)
                            is Matrix33 -> shader.uniform("p_${it.key}", value)
                            is Vector4 -> shader.uniform("p_${it.key}", value)
                            is Vector3 -> shader.uniform("p_${it.key}", value)
                            is Vector2 -> shader.uniform("p_${it.key}", value)
                            is ColorRGBa -> shader.uniform("p_${it.key}", value)
                            is ColorBuffer -> {
                                value.bind(textureIndex)
                                shader.uniform("p_${it.key}", textureIndex)
                                textureIndex++
                            }
                            is DepthBuffer -> {
                                value.bind(textureIndex)
                                shader.uniform("p_${it.key}", textureIndex)
                                textureIndex++
                            }
                            is BufferTexture -> {
                                value.bind(textureIndex)
                                shader.uniform("p_${it.key}", textureIndex)
                                textureIndex++
                            }
                            is Cubemap -> {
                                value.bind(textureIndex)
                                shader.uniform("p_${it.key}", textureIndex)
                                textureIndex++
                            }
                            is ArrayTexture -> {
                                value.bind(textureIndex)
                                shader.uniform("p_${it.key}", textureIndex)
                                textureIndex++
                            }
                            is ArrayCubemap -> {
                                value.bind(textureIndex)
                                shader.uniform("p_${it.key}", textureIndex)
                                textureIndex++
                            }
                            is Array<*> -> {
                                require(value.isNotEmpty())
                                when (value.first()!!) {
                                    is Matrix44 -> {
                                        @Suppress("UNCHECKED_CAST")
                                        shader.uniform("p_${it.key}", value as Array<Matrix44>)
                                    }
                                }
                            }
                            else -> {
                                throw RuntimeException("unsupported value type ${value::class}")
                            }
                        }
                    }
                }
                shader
            }
        }
    }
}