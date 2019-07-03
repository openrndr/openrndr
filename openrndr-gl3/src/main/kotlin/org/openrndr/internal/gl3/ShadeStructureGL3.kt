package org.openrndr.internal.gl3

import org.openrndr.draw.*

fun array(item: VertexElement): String = if (item.arraySize == 1) "" else "[${item.arraySize}]"

fun structureFromShadeTyle(shadeStyle: ShadeStyle?, vertexFormats: List<VertexFormat>, instanceAttributeFormats: List<VertexFormat>): ShadeStructure {
    return ShadeStructure().apply {
        if (shadeStyle != null) {
            vertexTransform = shadeStyle.vertexTransform
            fragmentTransform = shadeStyle.fragmentTransform
            vertexPreamble = shadeStyle.vertexPreamble
            fragmentPreamble = shadeStyle.fragmentPreamble
            outputs = shadeStyle.outputs.map { "layout(location = ${it.value}) out vec4 o_${it.key};\n" }.joinToString("")
            uniforms = shadeStyle.parameters.map { "uniform ${mapType(it.value)} p_${it.key};\n" }.joinToString("")
        }
        varyingOut = vertexFormats.flatMap { it.items }.joinToString("") { "out ${it.type.glslType} va_${it.attribute}${array(it)};\n" } +
                instanceAttributeFormats.flatMap { it.items }.joinToString("") { "out ${it.type.glslType} vi_${it.attribute}${array(it)};\n" }
        varyingIn = vertexFormats.flatMap { it.items }.joinToString("") { "in ${it.type.glslType} va_${it.attribute}${array(it)};\n" } +
                instanceAttributeFormats.flatMap { it.items }.joinToString("") { "in ${it.type.glslType} vi_${it.attribute}${array(it)};\n" }
        varyingBridge = vertexFormats.flatMap { it.items }.joinToString("") { "    va_${it.attribute} = a_${it.attribute};\n" } +
                instanceAttributeFormats.flatMap { it.items }.joinToString("") { "vi_${it.attribute} = i_${it.attribute};\n" }
        attributes = vertexFormats.flatMap { it.items }.joinToString("") { "in ${it.type.glslType} a_${it.attribute}${array(it)};\n" } +
                instanceAttributeFormats.flatMap { it.items }.joinToString("") { "in ${it.type.glslType} i_${it.attribute}${array(it)};\n" }

        suppressDefaultOutput = shadeStyle?.suppressDefaultOutput ?: false
    }
}

private fun mapType(type: String): String {
    return when (type) {
        "Int", "int" -> "int"
        "Matrix33" -> "mat3"
        "Matrix44" -> "mat4"
        "float" -> "float"
        "Vector2" -> "vec2"
        "Vector3" -> "vec3"
        "Vector4" -> "vec4"
        "ColorRGBa" -> "vec4"
        "BufferTexture" -> "samplerBuffer"
        "ColorBuffer" -> "sampler2D"
        "DepthBuffer" -> "sampler2D"
        "Cubemap" -> "samplerCube"
        "ArrayTexture" -> "sampler2DArray"
        else -> throw RuntimeException("unsupported type $type")
    }
}

private val VertexElementType.glslType: String
    get() {
        return when (this) {
            VertexElementType.FLOAT32 -> "float"
            VertexElementType.VECTOR2_FLOAT32 -> "vec2"
            VertexElementType.VECTOR3_FLOAT32 -> "vec3"
            VertexElementType.VECTOR4_FLOAT32 -> "vec4"
            VertexElementType.MATRIX22_FLOAT32 -> "mat2"
            VertexElementType.MATRIX33_FLOAT32 -> "mat3"
            VertexElementType.MATRIX44_FLOAT32 -> "mat4"
        }
    }
