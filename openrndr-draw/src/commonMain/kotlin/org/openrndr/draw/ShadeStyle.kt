package org.openrndr.draw

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.font.BufferAccess
import org.openrndr.draw.font.BufferFlag
import org.openrndr.math.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

inline fun <reified F> shadeStyleTypeOrNull(): String? {
    return when (F::class) {
        Boolean::class -> "boolean"
        Int::class -> "int"
        Float::class -> "float"
        Double::class -> "float"
        Vector2::class -> "Vector2"
        IntVector2::class -> "IntVector2"
        Vector3::class -> "Vector3"
        IntVector3::class -> "IntVector3"
        Vector4::class -> "Vector4"
        IntVector4::class -> "IntVector4"
        Matrix33::class -> "Matrix33"
        Matrix44::class -> "Matrix44"
        ColorRGBa::class -> "ColorRGBa"
        DepthBuffer::class -> "DepthBuffer"
        else -> "struct ${F::class.simpleName}"
    }
}

inline fun <reified F> shadeStyleType(): String {
    return shadeStyleTypeOrNull<F>() ?: error("shade style type not supported: ${F::class.simpleName}")
}

private fun shadeStyleTypeToGLSLOrNull(type: String): String? {
    return when (type) {
        shadeStyleType<Boolean>() -> "bool"
        shadeStyleType<Int>() -> "int"
        shadeStyleType<Matrix33>() -> "mat3"
        shadeStyleType<Matrix44>() -> "mat4"
        shadeStyleType<Float>(), shadeStyleType<Double>() -> "float"
        shadeStyleType<Vector2>() -> "vec2"
        shadeStyleType<Vector3>() -> "vec3"
        shadeStyleType<Vector4>(), shadeStyleType<ColorRGBa>() -> "vec4"
        "BufferTexture" -> "samplerBuffer"
        "BufferTexture_UINT" -> "usamplerBuffer"
        "BufferTexture_SINT" -> "isamplerBuffer"
        "ColorBuffer" -> "sampler2D"
        "ColorBuffer_UINT" -> "usampler2D"
        "ColorBuffer_SINT" -> "isampler2D"
        "DepthBuffer" -> "sampler2D"
        "Cubemap" -> "samplerCube"
        "Cubemap_UINT" -> "usamplerCube"
        "Cubemap_SINT" -> "isamplerCube"
        "ArrayCubemap" -> "samplerCubeArray"
        "ArrayCubemap_UINT" -> "usamplerCubeArray"
        "ArrayCubemap_SINT" -> "isamplerCubeArray"
        "ArrayTexture" -> "sampler2DArray"
        "ArrayTexture_UINT" -> "usampler2DArray"
        "ArrayTexture_SINT" -> "isampler2DArray"
        "VolumeTexture" -> "sampler3D"
        "VolumeTexture_UINT" -> "usampler3D"
        "VolumeTexture_SINT" -> "isampler3D"
        else -> if (type.startsWith("struct")) {
            type.drop(7)
        } else {
            null
        }
    }
}

fun shadeStyleTypeToGLSL(type: String): String {
    return shadeStyleTypeToGLSLOrNull(type) ?: error("unsupported type $type")
}

data class ShadeStyleOutput(
    val attachment: Int,
    val format: ColorFormat = ColorFormat.RGBa,
    val type: ColorType = ColorType.FLOAT32
)


class ObservableHashmap<K, V>(val b: MutableMap<K, V>, inline val onChange: () -> Unit) : MutableMap<K, V> by b {
    override fun put(key: K, value: V): V? {
        if (key in this) {
            if (get(key) != value) {
                onChange()
            }
        }
        return b.put(key, value)
    }

    override fun remove(key: K): V? {
        onChange()
        return b.remove(key)
    }
}



@Suppress("unused")
open class ShadeStyle : StyleParameters, StyleBufferBindings, StyleImageBindings {
    var dirty = true

    override var textureBaseIndex = 0

    var vertexPreamble: String? = null
        set(value) {
            dirty = true
            field = value
        }

    var geometryPreamble: String? = null
        set(value) {
            dirty = true
            field = value
        }

    var fragmentPreamble: String? = null
        set(value) {
            dirty = true
            field = value
        }

    var vertexTransform: String? = null
        set(value) {
            dirty = true
            field = value
        }

    var geometryTransform: String? = null
        set(value) {
            dirty = true
            field = value
        }

    var fragmentTransform: String? = null
        set(value) {
            dirty = true
            field = value
        }

    override var bufferValues = mutableMapOf<String, Any>()
    override val buffers = mutableMapOf<String, String>()
    override val bufferTypes = mutableMapOf<String, String>()
    override val bufferFlags = mutableMapOf<String, Set<BufferFlag>>()
    override val bufferAccess = mutableMapOf<String, BufferAccess>()

    override val imageTypes: MutableMap<String, String> = mutableMapOf()
    override val imageValues: MutableMap<String, Array<out ImageBinding>> = mutableMapOf()
    override val imageAccess: MutableMap<String, ImageAccess> = mutableMapOf()
    override val imageFlags: MutableMap<String, Set<ImageFlag>> = mutableMapOf()
    override val imageArrayLength: MutableMap<String, Int> = mutableMapOf()


    var outputs = ObservableHashmap(HashMap<String, ShadeStyleOutput>()) { dirty = true }
    var attributes = mutableListOf<VertexBuffer>()

    var suppressDefaultOutput = false
        set(value) {
            dirty = true
            field = value
        }

    constructor()

    constructor(other: ShadeStyle) {
        this.fragmentPreamble = other.fragmentPreamble
        this.geometryPreamble = other.geometryPreamble
        this.vertexPreamble = other.vertexPreamble

        this.fragmentTransform = other.fragmentTransform
        this.geometryTransform = other.geometryTransform
        this.vertexTransform = other.vertexTransform

        this.parameterTypes.putAll(other.parameterTypes)
        this.outputs.putAll(other.outputs)
    }

    fun output(name: String, output: ShadeStyleOutput) {
        outputs[name] = output
    }

    fun attributes(attributesBuffer: VertexBuffer) {
        attributes.add(attributesBuffer)
    }


    operator fun plus(other: ShadeStyle): ShadeStyle {
        val s = ShadeStyle()
        s.vertexTransform = concat(vertexTransform, other.vertexTransform)
        s.fragmentTransform = concat(fragmentTransform, other.fragmentTransform)

        s.vertexPreamble =
            (if (vertexPreamble == null) "" else vertexPreamble) + "\n" + if (other.vertexPreamble == null) "" else other.vertexPreamble
        s.fragmentPreamble =
            (if (fragmentPreamble == null) "" else fragmentPreamble) + "\n" + if (other.fragmentPreamble == null) "" else other.fragmentPreamble

        s.parameterTypes.apply {
            putAll(parameterTypes)
            putAll(other.parameterTypes)
        }

        s.parameterValues.apply {
            putAll(parameterValues)
            putAll(other.parameterValues)
        }

        s.outputs.apply {
            putAll(outputs)
            putAll(other.outputs)
        }

        s.attributes.apply {
            addAll(attributes)
            addAll(other.attributes)
        }

        return s
    }

    inner class Parameter<R : Any> : ReadWriteProperty<ShadeStyle, R> {

        override fun getValue(thisRef: ShadeStyle, property: KProperty<*>): R {
            @Suppress("UNCHECKED_CAST")
            return parameterValues[property.name] as R
        }

        override fun setValue(thisRef: ShadeStyle, property: KProperty<*>, value: R) {
            parameterValues[property.name] = value
            parameterTypes[property.name] = when (value) {
                is Boolean -> "boolean"
                is Int -> "int"
                is Double -> "float"
                is Float -> "float"
                is IntArray -> "int, ${value.size}"
                is FloatArray -> "float, ${value.size}"
                is Vector2 -> "Vector2"
                is Vector3 -> "Vector3"
                is Vector4 -> "Vector4"
                is IntVector2 -> "IntVector2"
                is IntVector3 -> "IntVector3"
                is IntVector4 -> "IntVector4"
                is Matrix33 -> "Matrix33"
                is Matrix44 -> "Matrix44"
                is ColorRGBa -> "ColorRGBa"
                is BufferTexture -> when (value.type.colorSampling) {
                    ColorSampling.UNSIGNED_INTEGER -> "BufferTexture_UINT"
                    ColorSampling.SIGNED_INTEGER -> "BufferTexture_SINT"
                    else -> "BufferTexture"
                }

                is DepthBuffer -> "DepthBuffer"
                is ArrayTexture -> when (value.type.colorSampling) {
                    ColorSampling.UNSIGNED_INTEGER -> "ArrayTexture_UINT"
                    ColorSampling.SIGNED_INTEGER -> "ArrayTexture_SINT"
                    else -> "ArrayTexture"
                }

                is ArrayCubemap -> when (value.type.colorSampling) {
                    ColorSampling.UNSIGNED_INTEGER -> "ArrayCubemap_UINT"
                    ColorSampling.SIGNED_INTEGER -> "ArrayCubemap_SINT"
                    else -> "ArrayCubemap"
                }

                is Cubemap -> when (value.type.colorSampling) {
                    ColorSampling.UNSIGNED_INTEGER -> "Cubemap_UINT"
                    ColorSampling.SIGNED_INTEGER -> "Cubemap_SINT"
                    else -> "Cubemap"
                }

                is ColorBuffer -> when (value.type.colorSampling) {
                    ColorSampling.UNSIGNED_INTEGER -> "ColorBuffer_UINT"
                    ColorSampling.SIGNED_INTEGER -> "ColorBuffer_SINT"
                    else -> "ColorBuffer"
                }

                is Array<*> -> {
                    if (value.isNotEmpty()) {
                        when (value.first()) {
                            is Matrix44 -> {
                                "Matrix44, ${value.size}"
                            }

                            is Vector2 -> {
                                "Vector2, ${value.size}"
                            }

                            is Vector3 -> {
                                "Vector3, ${value.size}"
                            }

                            is Vector4 -> {
                                "Vector4, ${value.size}"
                            }

                            is ColorRGBa -> {
                                "ColorRGBa, ${value.size}"
                            }

                            is Double -> {
                                "float, ${value.size}"
                            }

                            is IntVector2 -> {
                                "IntVector2, ${value.size}"
                            }

                            is IntVector3 -> {
                                "IntVector3, ${value.size}"
                            }

                            is IntVector4 -> {
                                "IntVector4, ${value.size}"
                            }

                            is CastableToVector4 -> {
                                "Vector4, ${value.size}"
                            }

                            else -> error("unsupported array type ${(value.first()!!)::class}")
                        }
                    } else {
                        error("empty array")
                    }
                }

                is CastableToVector4 -> {
                    "Vector4"
                }

                else -> error("unsupported type ${value::class}")
            }
        }
    }

    override var parameterValues: MutableMap<String, Any> = mutableMapOf()
    override var parameterTypes: ObservableHashmap<String, String> = ObservableHashmap(mutableMapOf()) { dirty = true }
}

@OptIn(ExperimentalContracts::class)
fun shadeStyle(builder: ShadeStyle.() -> Unit): ShadeStyle {
    contract {
        callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
    }
    return ShadeStyle().apply(builder)
}

private fun concat(left: String?, right: String?): String? {
    return if (left == null && right == null) {
        null
    } else if (left == null && right != null) {
        right
    } else if (left != null && right == null) {
        left
    } else if (left != null && right != null) {
        "${isolate(left)}\n${isolate(right)}"
    } else {
        throw RuntimeException("should never happen")
    }
}

private fun isolate(code: String): String =
    if (code.startsWith("{") && code.endsWith("}")) {
        code
    } else {
        "{$code}"
    }