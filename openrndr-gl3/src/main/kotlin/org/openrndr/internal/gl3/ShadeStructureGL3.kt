package org.openrndr.internal.gl3

import org.openrndr.draw.*

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
        varyingOut = vertexFormats.flatMap { it.items }.joinToString("") { "out ${it.glslType()} va_${it.attribute};\n" } +
                instanceAttributeFormats.flatMap { it.items }.joinToString("") { "out ${it.glslType()} vi_${it.attribute};\n" }
        varyingIn = vertexFormats.flatMap { it.items }.joinToString("") { "in ${it.glslType()} va_${it.attribute};\n" } +
                instanceAttributeFormats.flatMap { it.items }.joinToString("") { "in ${it.glslType()} vi_${it.attribute};\n" }
        varyingBridge = vertexFormats.flatMap { it.items }.joinToString("") { "va_${it.attribute} = a_${it.attribute};\n" } +
                instanceAttributeFormats.flatMap { it.items }.joinToString("") { "vi_${it.attribute} = i_${it.attribute};\n" }
        attributes = vertexFormats.flatMap { it.items }.joinToString("") { "in ${it.glslType()} a_${it.attribute};\n" } +
                instanceAttributeFormats.flatMap { it.items }.joinToString("") { "in ${it.glslType()} i_${it.attribute};\n" }
    }
}

private fun mapType(type: String): String {
    return when (type) {
        "Int", "int" -> "int"
        "Matrix44" -> "mat4"
        "float" -> "float"
        "Vector2" -> "vec2"
        "Vector3" -> "vec3"
        "Vector4" -> "vec4"
        "ColorRGBa" -> "vec4"
        "BufferTexture" -> "samplerBuffer"
        "ColorBuffer" -> "sampler2D"
        "Cubemap" -> "samplerCube"
        else -> throw RuntimeException("unsupported type $type")
    }
}

private fun VertexElement.glslType(): String {
    if (type == VertexElementType.FLOAT32) {
        return when (count) {
            1 -> "float"
            2 -> "vec2"
            3 -> "vec3"
            4 -> "vec4"
            9 -> "mat3"
            16 -> "mat4"
            else -> throw RuntimeException("unsupported component count ${count}")
        }
    } else {
        throw RuntimeException("unsupported component type $type")
    }
}