package org.openrndr.internal.gl3

import org.openrndr.draw.*

fun structureFromShadeTyle(shadeStyle: ShadeStyle?, vertexFormats: List<VertexFormat>, instanceAttributeFormats: List<VertexFormat>): ShadeStructure {
    val structure = ShadeStructure().apply {

        if (shadeStyle != null) {
            vertexTransform = shadeStyle.vertexTransform
            fragmentTransform = shadeStyle.fragmentTransform
            vertexPreamble = shadeStyle.vertexPreamble
            fragmentPreamble = shadeStyle.fragmentPreamble

            outputs = shadeStyle.outputs.map { "layout(location = ${it.value}) out vec4 o_${it.key};\n" }.joinToString("")

            uniforms = shadeStyle.parameters.map { "uniform ${mapType(it.value)} p_${it.key};\n" }.joinToString("")
        }
        varyingOut = vertexFormats.flatMap { it.items }.map { "out ${it.glslType()} va_${it.attribute};\n" }.joinToString("") +
                instanceAttributeFormats.flatMap { it.items }.map { "out ${it.glslType()} vi_${it.attribute};\n" }.joinToString("")
        varyingIn = vertexFormats.flatMap { it.items }.map { "in ${it.glslType()} va_${it.attribute};\n" }.joinToString("") +
                instanceAttributeFormats.flatMap { it.items }.map { "in ${it.glslType()} vi_${it.attribute};\n" }.joinToString("")
        varyingBridge = vertexFormats.flatMap { it.items }.map { "va_${it.attribute} = a_${it.attribute};\n" }.joinToString("") +
                instanceAttributeFormats.flatMap { it.items }.map { "vi_${it.attribute} = i_${it.attribute};\n" }.joinToString("")
        attributes = vertexFormats.flatMap { it.items }.map { "in ${it.glslType()} a_${it.attribute};\n" }.joinToString("") +
                instanceAttributeFormats.flatMap { it.items }.map { "in ${it.glslType()} i_${it.attribute};\n" }.joinToString("")

    }
    return structure
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
