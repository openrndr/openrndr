package org.openrndr.draw

import org.openrndr.color.ColorRGBa
import org.openrndr.math.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

data class ShadeStyleOutput(val attachment: Int, val format: ColorFormat = ColorFormat.RGBa, val type: ColorType = ColorType.FLOAT32)

class ObservableHashmap<K, V>(inline val onChange: () -> Unit) : HashMap<K, V>() {
    override fun put(key: K, value: V): V? {
        if (key in this) {
            if (get(key) != value) {
                onChange()
            }
        }
        return super.put(key, value)
    }

    override fun remove(key: K): V? {
        onChange()
        return super.remove(key)
    }
}

open class ShadeStyle {
    var dirty = true

    var vertexPreamble: String? = null
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

    var fragmentTransform: String? = null
        set(value) {
            dirty = true
            field = value
        }


    var parameterValues = mutableMapOf<String, Any>()
    var parameters = ObservableHashmap<String, String> { dirty = true }
    var outputs = ObservableHashmap<String, ShadeStyleOutput> { dirty = true }
    var attributes = mutableListOf<VertexBuffer>()

    var suppressDefaultOutput = false
        set(value) {
            dirty = true
            field = value
        }

    constructor()

    constructor(other: ShadeStyle) {
        this.fragmentPreamble = other.fragmentPreamble
        this.vertexPreamble = other.vertexPreamble
        this.vertexTransform = other.vertexTransform
        this.fragmentTransform = other.fragmentTransform

        this.parameterValues.putAll(other.parameterValues)
        this.parameters.putAll(other.parameters)
        this.outputs.putAll(other.outputs)
    }

    fun parameter(name: String, value: Cubemap) {
        parameterValues[name] = value
        parameters[name] = when (value.type.colorSampling) {
            ColorSampling.UNSIGNED_INTEGER -> "Cubemap_UINT"
            ColorSampling.SIGNED_INTEGER -> "Cubemap_SINT"
            else -> "Cubemap"
        }
    }

    fun parameter(name: String, value: Boolean) {
        parameterValues[name] = value
        parameters[name] = "boolean"
    }

    fun parameter(name: String, value: Int) {
        parameterValues[name] = value
        parameters[name] = "int"
    }

    fun parameter(name: String, value: Matrix33) {
        parameterValues[name] = value
        parameters[name] = "Matrix33"
    }

    fun parameter(name: String, value: Matrix44) {
        parameterValues[name] = value
        parameters[name] = "Matrix44"
    }

    fun parameter(name: String, value: Array<Matrix44>) {
        parameterValues[name] = value
        parameters[name] = "Matrix44,${value.size}"
    }

    fun parameter(name: String, value: Array<Vector2>) {
        parameterValues[name] = value
        parameters[name] = "Vector2,${value.size}"
    }

    fun parameter(name: String, value: Array<Vector3>) {
        parameterValues[name] = value
        parameters[name] = "Vector3,${value.size}"
    }

    fun parameter(name: String, value: Array<Vector4>) {
        parameterValues[name] = value
        parameters[name] = "Vector4,${value.size}"
    }

    fun parameter(name: String, value: Float) {
        parameterValues[name] = value
        parameters[name] = "float"
    }

    fun parameter(name: String, value: Double) {
        parameterValues[name] = value.toFloat()
        parameters[name] = "float"
    }

    fun parameter(name: String, value: Vector2) {
        parameterValues[name] = value
        parameters[name] = "Vector2"
    }

    fun parameter(name: String, value: Vector3) {
        parameterValues[name] = value
        parameters[name] = "Vector3"
    }

    fun parameter(name: String, value: Vector4) {
        parameterValues[name] = value
        parameters[name] = "Vector4"
    }

    fun parameter(name: String, value: IntVector2) {
        parameterValues[name] = value
        parameters[name] = "IntVector2"
    }

    fun parameter(name: String, value: IntVector3) {
        parameterValues[name] = value
        parameters[name] = "IntVector3"
    }

    fun parameter(name: String, value: IntVector4) {
        parameterValues[name] = value
        parameters[name] = "IntVector4"
    }

    fun parameter(name: String, value: ColorRGBa) {
        parameterValues[name] = value
        parameters[name] = "ColorRGBa"
    }

    fun parameter(name: String, value: ColorBuffer) {
        parameterValues[name] = value
        parameters[name] = when (value.type.colorSampling) {
            ColorSampling.UNSIGNED_INTEGER -> "ColorBuffer_UINT"
            ColorSampling.SIGNED_INTEGER -> "ColorBuffer_SINT"
            else -> "ColorBuffer"
        }
    }

    fun parameter(name: String, value: DepthBuffer) {
        parameterValues[name] = value
        parameters[name] = "DepthBuffer"
    }

    fun parameter(name: String, value: ArrayTexture) {
        parameterValues[name] = value
        parameters[name] = when (value.type.colorSampling) {
            ColorSampling.UNSIGNED_INTEGER -> "ArrayTexture_UINT"
            ColorSampling.SIGNED_INTEGER -> "ArrayTexture_SINT"
            else -> "ArrayTexture"
        }
    }

    fun parameter(name: String, value: ArrayCubemap) {
        parameterValues[name] = value
        parameters[name] = when (value.type.colorSampling) {
            ColorSampling.UNSIGNED_INTEGER -> "ArrayCubemap_UINT"
            ColorSampling.SIGNED_INTEGER -> "ArrayCubemap_SINT"
            else -> "ArrayCubemap"
        }
    }

    fun parameter(name: String, value: BufferTexture) {
        parameterValues[name] = value
        parameters[name] = when (value.type.colorSampling) {
            ColorSampling.UNSIGNED_INTEGER -> "BufferTexture_UINT"
            ColorSampling.SIGNED_INTEGER -> "BufferTexture_SINT"
            else -> "BufferTexture"
        }
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

        s.vertexPreamble = (if (vertexPreamble == null) "" else vertexPreamble) + "\n" + if (other.vertexPreamble == null) "" else other.vertexPreamble
        s.fragmentPreamble = (if (fragmentPreamble == null) "" else fragmentPreamble) + "\n" + if (other.fragmentPreamble == null) "" else other.fragmentPreamble

        s.parameters.apply {
            putAll(parameters)
            putAll(other.parameters)
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
            parameters[property.name] = when (value) {
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
                            is IntVector2 -> {
                                "IntVector2, ${value.size}"
                            }
                            is IntVector3 -> {
                                "IntVector3, ${value.size}"
                            }
                            is IntVector4 -> {
                                "IntVector4, ${value.size}"
                            }
                            else -> error("unsupported array type ${(value.first()!!)::class}")
                        }
                    } else {
                        error("empty array")
                    }
                }
                else -> error("unsupported type ${value::class}")
            }
        }
    }
}

fun shadeStyle(builder: ShadeStyle.() -> Unit): ShadeStyle = ShadeStyle().apply(builder)

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