package org.openrndr.draw

import org.openrndr.color.ColorRGBa
import org.openrndr.math.Matrix44
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4

class ShadeStyle {
    var vertexPreamble: String? = null
    var fragmentPreamble: String? = null
    var vertexTransform: String? = null
    var fragmentTransform: String? = null

    var parameterValues = mutableMapOf<String, Any>()
    var parameters = mutableMapOf<String, String>()
    var outputs = mutableMapOf<String, Int>()
    var attributes = mutableListOf<VertexBuffer>()

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

    fun parameter(name: String, value: Cubemap): ShadeStyle {
        parameterValues.put(name, value)
        parameters.put(name, "Cubemap")
        return this
    }

    fun parameter(name: String, value: Int): ShadeStyle {
        parameterValues.put(name, value)
        parameters.put(name, "int")
        return this
    }

    fun parameter(name: String, value: Matrix44): ShadeStyle {
        parameterValues.put(name, value)
        parameters.put(name, "Matrix44")
        return this
    }

    fun parameter(name: String, value: Float): ShadeStyle {
        parameterValues.put(name, value)
        parameters.put(name, "float")
        return this
    }

    fun parameter(name: String, value: Double): ShadeStyle {
        parameterValues.put(name, value.toFloat())
        parameters.put(name, "float")
        return this
    }

    fun parameter(name: String, value: Vector2): ShadeStyle {
        parameterValues.put(name, value)
        parameters.put(name, "Vector2")
        return this
    }

    fun parameter(name: String, value: Vector3): ShadeStyle {
        parameterValues.put(name, value)
        parameters.put(name, "Vector3")
        return this
    }

    fun parameter(name: String, value: Vector4): ShadeStyle {
        parameterValues.put(name, value)
        parameters.put(name, "Vector4")
        return this
    }

    fun parameter(name: String, value: ColorRGBa): ShadeStyle {
        parameterValues.put(name, value)
        parameters.put(name, "ColorRGBa")
        return this
    }

    fun parameter(name: String, value: ColorBuffer): ShadeStyle {
        parameterValues.put(name, value)
        parameters.put(name, "ColorBuffer")
        return this
    }

    fun parameter(name: String, value: BufferTexture): ShadeStyle {
        parameterValues.put(name, value)
        parameters.put(name, "BufferTexture")
        return this
    }

    fun output(name: String, slot: Int): ShadeStyle {
        outputs.put(name, slot)
        return this
    }

    fun attributes(attributesBuffer:VertexBuffer) {
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

