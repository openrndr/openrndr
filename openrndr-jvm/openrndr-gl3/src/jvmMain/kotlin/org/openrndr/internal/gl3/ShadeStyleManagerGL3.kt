package org.openrndr.internal.gl3

import mu.KotlinLogging
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.math.*
import org.openrndr.measure

private val logger = KotlinLogging.logger {}

class ShadeStyleManagerGL3(
    name: String,
    val vsGenerator: (ShadeStructure) -> String,
    val tcsGenerator: ((ShadeStructure) -> String)?,
    val tesGenerator: ((ShadeStructure) -> String)?,
    val gsGenerator: ((ShadeStructure) -> String)?,
    val fsGenerator: (ShadeStructure) -> String
) : ShadeStyleManager(name) {

    private var defaultShader: Shader? = null
    private val shaders = mutableMapOf<ShadeStructure, Shader>()

    override fun shader(
        style: ShadeStyle?,
        vertexFormats: List<VertexFormat>,
        instanceFormats: List<VertexFormat>
    ): Shader {
        val outputInstanceFormats = instanceFormats + (style?.attributes
            ?: emptyList()).map { it.vertexFormat }

        if (style == null) {
            return measure("default-shader") {
                if (defaultShader == null) {
                    logger.debug { "creating default shader" }
                    val structure = structureFromShadeStyle(style, vertexFormats, outputInstanceFormats)
                    defaultShader = Shader.createFromCode(
                        vsCode = vsGenerator(structure),
                        tcsCode = tcsGenerator?.invoke(structure),
                        tesCode = tesGenerator?.invoke(structure),
                        gsCode = gsGenerator?.invoke(structure),
                        fsCode = fsGenerator(structure),
                        name = "shade-style-default:$name",
                        session = Session.root
                    )
                    (defaultShader as ShaderGL3).userShader = false
                }
                defaultShader!!
            }
        } else {
            return measure("custom-shader") {
                val structure = structureFromShadeStyle(style, vertexFormats, outputInstanceFormats)
                val shader = shaders.getOrPut(structure) {
                    try {
                        Shader.createFromCode(
                            vsCode = vsGenerator(structure),
                            tcsCode = tcsGenerator?.invoke(structure),
                            tesCode = tesGenerator?.invoke(structure),
                            gsCode = gsGenerator?.invoke(structure),
                            fsCode = fsGenerator(structure),
                            name = "shade-style-custom:$name-${structure.hashCode()}",
                            session = Session.root
                        )
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
                var imageIndex = 0
                var bufferIndex = 2
                measure("bind-ssbos") {
                    for (it in style.bufferValues.entries) {
                        when (val value = it.value) {
                            is ShaderStorageBuffer -> {
                                value.bind(bufferIndex)
                                bufferIndex++
                            }
                            else -> error("unsupported buffer type $value")
                        }
                    }
                }

                fun setUniform(targetName:String, name:String, value:Any) {
                    when (value) {
                        is Boolean -> shader.uniform(targetName, value)
                        is Int -> shader.uniform(targetName, value)
                        is Float -> shader.uniform(targetName, value)
                        is Double -> shader.uniform(targetName, value)
                        is Matrix44 -> shader.uniform(targetName, value)
                        is Matrix33 -> shader.uniform(targetName, value)
                        is Vector4 -> shader.uniform(targetName, value)
                        is Vector3 -> shader.uniform(targetName, value)
                        is Vector2 -> shader.uniform(targetName, value)
                        is ColorRGBa -> shader.uniform(targetName, value)
                        is ColorBuffer -> {
                            value.bind(textureIndex)
                            shader.uniform(targetName, textureIndex)
                            textureIndex++
                        }
                        is DepthBuffer -> {
                            value.bind(textureIndex)
                            shader.uniform(targetName, textureIndex)
                            textureIndex++
                        }
                        is BufferTexture -> {
                            value.bind(textureIndex)
                            shader.uniform(targetName, textureIndex)
                            textureIndex++
                        }
                        is Cubemap -> {
                            value.bind(textureIndex)
                            shader.uniform(targetName, textureIndex)
                            textureIndex++
                        }
                        is ArrayTexture -> {
                            value.bind(textureIndex)
                            shader.uniform(targetName, textureIndex)
                            textureIndex++
                        }
                        is ArrayCubemap -> {
                            value.bind(textureIndex)
                            shader.uniform(targetName, textureIndex)
                            textureIndex++
                        }
                        is VolumeTexture -> {
                            value.bind(textureIndex)
                            shader.uniform(targetName, textureIndex)
                            textureIndex++
                        }
                        is ImageBinding -> {
                            shader.image(targetName, imageIndex, value)
                            imageIndex++
                        }
                        is DoubleArray -> {
                            shader.uniform(targetName, value.map { it.toFloat() }.toFloatArray())
                        }
                        is IntArray -> {
                            shader.uniform(targetName, value)
                        }
                        is Array<*> -> {
                            require(value.isNotEmpty())
                            when (value.firstOrNull()) {
                                is Matrix44 -> {
                                    @Suppress("UNCHECKED_CAST")
                                    shader.uniform(targetName, value as Array<Matrix44>)
                                }
                                is Double -> {
                                    @Suppress("UNCHECKED_CAST")
                                    shader.uniform(targetName, value as Array<Double>)
                                }
                                is ColorRGBa -> {
                                    @Suppress("UNCHECKED_CAST")
                                    shader.uniform(targetName, value as Array<ColorRGBa>)
                                }
                                is Vector4 -> {
                                    @Suppress("UNCHECKED_CAST")
                                    shader.uniform(targetName, value as Array<Vector4>)
                                }
                                is Vector3 -> {
                                    @Suppress("UNCHECKED_CAST")
                                    shader.uniform(targetName, value as Array<Vector3>)
                                }
                                is Vector2 -> {
                                    @Suppress("UNCHECKED_CAST")
                                    shader.uniform(targetName, value as Array<Vector2>)
                                }
                                is CastableToVector4 -> {
                                    @Suppress("UNCHECKED_CAST")
                                    shader.uniform(targetName, (value as Array<CastableToVector4>).map {
                                        it.toVector4()
                                    }.toTypedArray() )
                                }
                                is Struct<*> -> {
                                    for (i in 0 until value.size) {
                                        setUniform("$targetName[$i]", "", value[i]!!)
                                    }
                                }
                            }
                        }
                        is CastableToVector4 -> {
                            shader.uniform(targetName, value.toVector4())
                        }
                        is Struct<*> -> {
                            for (f in value.values.keys) {
                                setUniform("$targetName.$f", "", value.values.getValue(f))
                            }
                        }
                        else -> {
                            throw RuntimeException("unsupported value type ${value::class}")
                        }
                    }
                }

                measure("set-uniforms") {
                    for (it in style.parameterValues.entries) {
                        setUniform("p_${it.key}", it.key, it.value)
                    }
                }
                shader
            }
        }
    }
}