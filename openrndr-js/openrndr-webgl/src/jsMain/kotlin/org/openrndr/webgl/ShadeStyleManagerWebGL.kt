package org.openrndr.webgl

import mu.KotlinLogging
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.math.*

private val logger = KotlinLogging.logger {}

class ShadeStyleManagerWebGL(
    name: String,
    val vsGenerator: (ShadeStructure) -> String,
    val fsGenerator: (ShadeStructure) -> String,
    val session: Session?
) : ShadeStyleManager(name) {

    private var defaultShader: Shader? = null
    private val shaders = mutableMapOf<ShadeStructure, Shader>()

    override fun shader(
        style: ShadeStyle?,
        vertexFormats: List<VertexFormat>,
        instanceFormats: List<VertexFormat>
    ): Shader {
        val outputInstanceFormats = instanceFormats + (style?.attributes
            ?: emptyList<VertexBuffer>()).map { it.vertexFormat }

        if (style == null) {
            return run {
                if (defaultShader == null) {
                    logger.debug { "creating default shader" }
                    val structure = structureFromShadeStyle(style, vertexFormats, outputInstanceFormats)
                    defaultShader = Shader.createFromCode(
                        vsCode = vsGenerator(structure),
                        tcsCode = null,
                        tesCode = null,
                        gsCode = null,
                        fsCode = fsGenerator(structure),
                        name = "shade-style-default:$name",
                        session = Session.root
                    )
                    (defaultShader as ShaderWebGL).userShader = false
                }
                defaultShader!!
            }
        } else {
            return run {
                val structure = structureFromShadeStyle(style, vertexFormats, outputInstanceFormats)
                val shader = shaders.getOrPut(structure) {
                    try {
                        Shader.createFromCode(
                            vsCode = vsGenerator(structure),
                            tcsCode = null,
                            tesCode = null,
                            gsCode = null,
                            fsCode = fsGenerator(structure),
                            name = "shade-style-custom:$name-${structure.hashCode()}",
                            session = Session.root
                        )
                    } catch (e: Throwable) {
                        throw e
                    }
                }
                (shader as ShaderWebGL).userShader = false

                shader.begin()
                var textureIndex = 2
                var imageIndex = 0
                var bufferIndex = 2


                run {
                    for (it in style.parameterValues.entries) {
                        when (val value = it.value) {
                            is Float -> {
                                shader.uniform("p_${it.key}", value)
                            }
                            is Double -> {
                                shader.uniform("p_${it.key}", value.toFloat())
                            }
                            is Boolean -> shader.uniform("p_${it.key}", value)
                            is Int -> shader.uniform("p_${it.key}", value)

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
                            is VolumeTexture -> {
                                value.bind(textureIndex)
                                shader.uniform("p_${it.key}", textureIndex)
                                textureIndex++
                            }
                            is ImageBinding -> {
                                shader.image("p_${it.key}", imageIndex, value)
                                imageIndex++
                            }
                            is DoubleArray -> {
                                shader.uniform("p_${it.key}", value.map { it.toFloat() }.toFloatArray())
                            }
                            is IntArray -> {
                                shader.uniform("p_${it.key}", value)
                            }
                            is Array<*> -> {
                                require(value.isNotEmpty())
                                when (value.firstOrNull()) {
                                    is Matrix44 -> {
                                        @Suppress("UNCHECKED_CAST")
                                        shader.uniform("p_${it.key}", value as Array<Matrix44>)
                                    }
                                    is Double -> {
                                        @Suppress("UNCHECKED_CAST")
                                        shader.uniform("p_${it.key}", value as Array<Double>)
                                    }
                                    is ColorRGBa -> {
                                        @Suppress("UNCHECKED_CAST")
                                        shader.uniform("p_${it.key}", value as Array<ColorRGBa>)
                                    }
                                    is Vector4 -> {
                                        @Suppress("UNCHECKED_CAST")
                                        shader.uniform("p_${it.key}", value as Array<Vector4>)
                                    }
                                    is Vector3 -> {
                                        @Suppress("UNCHECKED_CAST")
                                        shader.uniform("p_${it.key}", value as Array<Vector3>)
                                    }
                                    is Vector2 -> {
                                        @Suppress("UNCHECKED_CAST")
                                        shader.uniform("p_${it.key}", value as Array<Vector2>)
                                    }
                                    is CastableToVector4 -> {
                                        @Suppress("UNCHECKED_CAST")
                                        shader.uniform("p_${it.key}", (value as Array<CastableToVector4>).map {
                                            it.toVector4()
                                        }.toTypedArray() )
                                    }
                                }
                            }
                            is CastableToVector4 -> {
                                shader.uniform("p_${it.key}", value.toVector4())
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