package org.openrndr.internal.glcommon

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.math.*

interface StyleManagerDispatchUniform {
    var textureIndex: Int

    fun <T> dispatchParameters(style: StyleParameters, shader: T, textureBindings: TextureBindings) where T: ShaderUniforms {
        textureIndex = style.textureBaseIndex
        for ((key, value) in style.parameterValues) {
            val type = style.parameterTypes[key] ?: error("parameter type not found for $key")
            setUniform(shader, textureBindings,"p_$key", type, key, value)
        }
    }

    fun <T> setUniform(shader: T, textureBindings: TextureBindings, targetName: String, type: String, name: String, value: Any) where T : ShaderUniforms {

        println("${value is Int} ${value is Double}")
        when (value) {
            is Boolean -> shader.uniform(targetName, value)
            is Int -> {
                when (type) {
                    "int" ->
                    shader.uniform(targetName, value as Int)
                    "float" -> shader.uniform(targetName, value as Double)
                }

            }
            is Float -> shader.uniform(targetName, value)
            is Double -> shader.uniform(targetName, value)
            is Matrix44 -> shader.uniform(targetName, value)
            is Matrix33 -> shader.uniform(targetName, value)
            is Vector4 -> shader.uniform(targetName, value)
            is Vector3 -> shader.uniform(targetName, value)
            is Vector2 -> shader.uniform(targetName, value)
            is IntVector2 -> shader.uniform(targetName, value)
            is IntVector3 -> shader.uniform(targetName, value)
            is IntVector4 -> shader.uniform(targetName, value)
            is ColorRGBa -> shader.uniform(targetName, value)
            is ColorBuffer -> {
                textureBindings[textureIndex] = value
                shader.uniform(targetName, textureIndex)
                textureIndex++
            }

            is DepthBuffer -> {
                textureBindings[textureIndex] = value
                shader.uniform(targetName, textureIndex)
                textureIndex++
            }

            is BufferTexture -> {
                textureBindings[textureIndex] = value
                shader.uniform(targetName, textureIndex)
                textureIndex++
            }

            is Cubemap -> {
                textureBindings[textureIndex] = value
                shader.uniform(targetName, textureIndex)
                textureIndex++
            }

            is ArrayTexture -> {
                textureBindings[textureIndex] = value
                shader.uniform(targetName, textureIndex)
                textureIndex++
            }

            is ArrayCubemap -> {
                textureBindings[textureIndex] = value
                shader.uniform(targetName, textureIndex)
                textureIndex++
            }

            is VolumeTexture -> {
                textureBindings[textureIndex] = value
                shader.uniform(targetName, textureIndex)
                textureIndex++
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
                        }.toTypedArray())
                    }

                    is Struct<*> -> {
                        for (i in 0 until value.size) {
                            setUniform(shader, textureBindings,"$targetName[$i]", "struct","", value[i]!!)
                        }
                    }
                }
            }

            is CastableToVector4 -> {
                shader.uniform(targetName, value.toVector4())
            }

            is Struct<*> -> {
                for (f in value.values.keys) {
                    val type = value.types[f]!!
                    setUniform(shader, textureBindings,"$targetName.$f", type,"", value.values.getValue(f))
                }
            }

            else -> {
                throw RuntimeException("unsupported value type ${value::class}")
            }
        }
    }
}