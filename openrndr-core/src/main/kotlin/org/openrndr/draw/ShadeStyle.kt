package org.openrndr.draw

import org.openrndr.color.ColorRGBa
import org.openrndr.math.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

open class ShadeStyle {
    var vertexPreamble: String? = null
    var fragmentPreamble: String? = null
    var vertexTransform: String? = null
    var fragmentTransform: String? = null

    var parameterValues = mutableMapOf<String, Any>()
    var parameters = mutableMapOf<String, String>()
    var outputs = mutableMapOf<String, Int>()
    var attributes = mutableListOf<VertexBuffer>()

    var suppressDefaultOutput = false

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
        parameters[name] = "Cubemap"
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


    fun parameter(name: String, value: BufferTexture) {
        parameterValues[name] = value
        parameters[name] = "BufferTexture"
    }

    fun output(name: String, slot: Int) {
        outputs[name] = slot
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

    inner class Parameter<R : Any>: ReadWriteProperty<ShadeStyle, R> {

        override fun getValue(thisRef: ShadeStyle, property: KProperty<*>): R {
            @Suppress("UNCHECKED_CAST")
            return parameterValues[property.name] as R
        }

        override fun setValue(thisRef: ShadeStyle, property: KProperty<*>, value: R) {
            parameterValues[property.name] = value
            parameters[property.name] = when (value) {
                is Int -> "int"
                is Double -> "float"
                is Float -> "float"
                is Vector2 -> "Vector2"
                is Vector3 -> "Vector3"
                is Vector4 -> "Vector4"
                is Matrix33 -> "Matrix33"
                is Matrix44 -> "Matrix44"
                is ColorRGBa -> "ColorRGBa"
                is BufferTexture -> "BufferTexture"
                is DepthBuffer -> "DepthBuffer"
                is ArrayTexture -> when (value.type.colorSampling) {
                    ColorSampling.UNSIGNED_INTEGER -> "ArrayTexture_UINT"
                    ColorSampling.SIGNED_INTEGER -> "ArrayTexture_SINT"
                    else -> "ArrayTexture"
                }
                is ColorBuffer -> when (value.type.colorSampling) {
                    ColorSampling.UNSIGNED_INTEGER -> "ColorBuffer_UINT"
                    ColorSampling.SIGNED_INTEGER -> "ColorBuffer_SINT"
                    else -> "ColorBuffer"
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

