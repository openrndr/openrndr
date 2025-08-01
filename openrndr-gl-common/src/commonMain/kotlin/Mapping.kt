package org.openrndr.internal.glcommon

import org.openrndr.draw.*
import org.openrndr.draw.font.BufferAccess
import org.openrndr.draw.font.BufferFlag

internal fun imageLayout(format: ColorFormat, type: ColorType): String {
    return when (Pair(format, type)) {
        Pair(ColorFormat.R, ColorType.UINT8) -> "r8"
        Pair(ColorFormat.R, ColorType.UINT8_INT) -> "r8u"
        Pair(ColorFormat.R, ColorType.SINT8_INT) -> "r8i"
        Pair(ColorFormat.R, ColorType.UINT16) -> "r16"
        Pair(ColorFormat.R, ColorType.UINT16_INT) -> "r16u"
        Pair(ColorFormat.R, ColorType.SINT16_INT) -> "r16i"
        Pair(ColorFormat.R, ColorType.UINT32_INT) -> "r32u"
        Pair(ColorFormat.R, ColorType.SINT32_INT) -> "r32i"
        Pair(ColorFormat.R, ColorType.FLOAT16) -> "r16f"
        Pair(ColorFormat.R, ColorType.FLOAT32) -> "r32f"

        Pair(ColorFormat.RG, ColorType.UINT8) -> "rg8"
        Pair(ColorFormat.RG, ColorType.UINT8_INT) -> "rg8u"
        Pair(ColorFormat.RG, ColorType.SINT8_INT) -> "rg8i"
        Pair(ColorFormat.RG, ColorType.UINT16) -> "rg16"
        Pair(ColorFormat.RG, ColorType.UINT16_INT) -> "rg16u"
        Pair(ColorFormat.RG, ColorType.SINT16_INT) -> "rg16i"
        Pair(ColorFormat.RG, ColorType.FLOAT16) -> "rg16f"
        Pair(ColorFormat.RG, ColorType.FLOAT32) -> "rg32f"

        Pair(ColorFormat.RGBa, ColorType.UINT8) -> "rgba8"
        Pair(ColorFormat.RGBa, ColorType.UINT8_INT) -> "rgba8u"
        Pair(ColorFormat.RGBa, ColorType.SINT8_INT) -> "rgba8i"
        Pair(ColorFormat.RGBa, ColorType.UINT16) -> "rgba16"
        Pair(ColorFormat.RGBa, ColorType.UINT16_INT) -> "rgba16u"
        Pair(ColorFormat.RGBa, ColorType.SINT16_INT) -> "rgba16i"
        Pair(ColorFormat.RGBa, ColorType.FLOAT16) -> "rgba16f"
        Pair(ColorFormat.RGBa, ColorType.FLOAT32) -> "rgba32f"
        else -> error("unsupported layout: $format $type")
    }
}

internal val ShadeStyleOutput.glslType: String
    get() {
        return when (Pair(this.format.componentCount, this.type.colorSampling)) {
            Pair(1, ColorSampling.NORMALIZED) -> "float"
            Pair(2, ColorSampling.NORMALIZED) -> "vec2"
            Pair(3, ColorSampling.NORMALIZED) -> "vec3"
            Pair(4, ColorSampling.NORMALIZED) -> "vec4"
            Pair(1, ColorSampling.UNSIGNED_INTEGER) -> "uint"
            Pair(2, ColorSampling.UNSIGNED_INTEGER) -> "uvec2"
            Pair(3, ColorSampling.UNSIGNED_INTEGER) -> "uvec3"
            Pair(4, ColorSampling.UNSIGNED_INTEGER) -> "uvec4"
            Pair(1, ColorSampling.SIGNED_INTEGER) -> "int"
            Pair(2, ColorSampling.SIGNED_INTEGER) -> "ivec2"
            Pair(3, ColorSampling.SIGNED_INTEGER) -> "ivec3"
            Pair(4, ColorSampling.SIGNED_INTEGER) -> "ivec4"

            else -> error("unsupported type")
        }
    }

private val BufferPrimitiveType.glslType: String
    get() {
        return when (this) {
            BufferPrimitiveType.BOOLEAN -> "bool"
            BufferPrimitiveType.INT32 -> "int"
            BufferPrimitiveType.UINT32 -> "uint"
            BufferPrimitiveType.FLOAT32 -> "float"

            BufferPrimitiveType.VECTOR2_UINT32 -> "uvec2"
            BufferPrimitiveType.VECTOR2_BOOLEAN -> "bvec2"
            BufferPrimitiveType.VECTOR2_INT32 -> "ivec2"
            BufferPrimitiveType.VECTOR2_FLOAT32 -> "vec2"

            BufferPrimitiveType.VECTOR3_UINT32 -> "uvec3"
            BufferPrimitiveType.VECTOR3_BOOLEAN -> "bvec3"
            BufferPrimitiveType.VECTOR3_INT32 -> "ivec3"
            BufferPrimitiveType.VECTOR3_FLOAT32 -> "vec3"

            BufferPrimitiveType.VECTOR4_UINT32 -> "uvec4"
            BufferPrimitiveType.VECTOR4_BOOLEAN -> "bvec4"
            BufferPrimitiveType.VECTOR4_INT32 -> "ivec4"
            BufferPrimitiveType.VECTOR4_FLOAT32 -> "vec4"

            BufferPrimitiveType.MATRIX22_FLOAT32 -> "mat2"
            BufferPrimitiveType.MATRIX33_FLOAT32 -> "mat3"
            BufferPrimitiveType.MATRIX44_FLOAT32 -> "mat4"
        }
    }


internal val VertexElementType.glslType: String
    get() {
        return when (this) {
            VertexElementType.INT8, VertexElementType.INT16, VertexElementType.INT32 -> "int"
            VertexElementType.UINT8, VertexElementType.UINT16, VertexElementType.UINT32 -> "uint"
            VertexElementType.VECTOR2_UINT8, VertexElementType.VECTOR2_UINT16, VertexElementType.VECTOR2_UINT32 -> "uvec2"
            VertexElementType.VECTOR2_INT8, VertexElementType.VECTOR2_INT16, VertexElementType.VECTOR2_INT32 -> "ivec2"
            VertexElementType.VECTOR3_UINT8, VertexElementType.VECTOR3_UINT16, VertexElementType.VECTOR3_UINT32 -> "uvec3"
            VertexElementType.VECTOR3_INT8, VertexElementType.VECTOR3_INT16, VertexElementType.VECTOR3_INT32 -> "ivec3"
            VertexElementType.VECTOR4_UINT8, VertexElementType.VECTOR4_UINT16, VertexElementType.VECTOR4_UINT32 -> "uvec4"
            VertexElementType.VECTOR4_INT8, VertexElementType.VECTOR4_INT16, VertexElementType.VECTOR4_INT32 -> "ivec4"
            VertexElementType.FLOAT32 -> "float"
            VertexElementType.VECTOR2_FLOAT32 -> "vec2"
            VertexElementType.VECTOR3_FLOAT32 -> "vec3"
            VertexElementType.VECTOR4_FLOAT32 -> "vec4"
            VertexElementType.MATRIX22_FLOAT32 -> "mat2"
            VertexElementType.MATRIX33_FLOAT32 -> "mat3"
            VertexElementType.MATRIX44_FLOAT32 -> "mat4"
        }
    }

internal val VertexElementType.glslVaryingQualifier: String
    get() {
        return when (this) {
            VertexElementType.INT8, VertexElementType.INT16, VertexElementType.INT32 -> "flat "
            VertexElementType.UINT8, VertexElementType.UINT16, VertexElementType.UINT32 -> "flat "
            VertexElementType.VECTOR2_UINT8, VertexElementType.VECTOR2_UINT16, VertexElementType.VECTOR2_UINT32 -> "flat "
            VertexElementType.VECTOR2_INT8, VertexElementType.VECTOR2_INT16, VertexElementType.VECTOR2_INT32 -> "flat "
            VertexElementType.VECTOR3_UINT8, VertexElementType.VECTOR3_UINT16, VertexElementType.VECTOR3_UINT32 -> "flat "
            VertexElementType.VECTOR3_INT8, VertexElementType.VECTOR3_INT16, VertexElementType.VECTOR3_INT32 -> "flat "
            VertexElementType.VECTOR4_UINT8, VertexElementType.VECTOR4_UINT16, VertexElementType.VECTOR4_UINT32 -> "flat "
            VertexElementType.VECTOR4_INT8, VertexElementType.VECTOR4_INT16, VertexElementType.VECTOR4_INT32 -> "flat "
            else -> ""
        }
    }

internal val ShaderStorageStruct.glslDefinition: String
    get() = """#ifndef STRUCT_${structName}
#define STRUCT_${structName}
struct $structName {
    ${glslLayout.prependIndent("    ")}
};
#endif
"""

internal val ShaderStoragePrimitive.glslDefinition: String
    get() {
        return if (arraySize == 1) {
            "${type.glslType} ${name};"
        } else {
            "${type.glslType}[${arraySize}] ${name};"
        }
    }

internal val ShaderStorageFormat.glslLayout: String
    get() = elements.joinToString("\n") {
        when (it) {
            is ShaderStoragePrimitive -> it.glslDefinition
            is ShaderStorageStruct -> {
                if (it.arraySize == 1) {
                    "${it.structName} ${it.name};"
                } else {
                    "${it.structName}[${it.arraySize}] ${it.name};"
                }
            }
        }
    }

internal val ShaderStorageStruct.glslLayout: String
    get() = elements.joinToString("\n") {
        when (it) {
            is ShaderStoragePrimitive -> it.glslDefinition
            is ShaderStorageStruct -> {
                if (it.arraySize == 1) {
                    "${it.structName} ${it.name};"
                } else {
                    "${it.structName}[${it.arraySize}] ${it.name};"
                }
            }
            else -> ""
        }
    }

internal val BufferAccess.glsl: String
    get() = when (this) {
        BufferAccess.READ -> "readonly"
        BufferAccess.READ_WRITE -> ""
        BufferAccess.WRITE -> "writeonly"
    }

internal val BufferFlag.glsl: String
    get() = when (this) {
        BufferFlag.RESTRICT -> "restrict"
        BufferFlag.COHERENT -> "coherent"
        BufferFlag.VOLATILE -> "volatile"
    }


internal fun mapTypeToUniform(type: String, name: String): String {
    val tokens = type.split(",")
    val arraySize = tokens.getOrNull(1)
    val u = "uniform"

    fun String?.arraySizeDefinition() = if (this == null) {
        ";"
    } else {
        "[$arraySize]; \n#define p_${name}_SIZE $arraySize"
    }

    val subtokens = tokens[0].split(" ")
    return when (subtokens[0]) {
        "struct" -> "$u ${subtokens[1]} p_$name${arraySize.arraySizeDefinition()}"
        "Image2D", "Image3D", "ImageCube", "Image2DArray", "ImageBuffer", "ImageCubeArray" -> {
            val sampler = tokens[0].take(1).lowercase() + tokens[0].drop(1)
            val colorFormat = ColorFormat.valueOf(tokens[1])
            val colorType = ColorType.valueOf(tokens[2])
            val access = ImageAccess.valueOf(tokens[3])
            val layout = imageLayout(colorFormat, colorType)
            when (access) {
                ImageAccess.READ -> "layout($layout) readonly $u $sampler p_$name;"
                ImageAccess.READ_WRITE -> "layout($layout) $u $sampler p_$name;"
                ImageAccess.WRITE -> "layout($layout) writeonly $u $sampler p_$name;"
            }
        }

        else -> "$u ${shadeStyleTypeToGLSL(tokens[0])} p_$name${arraySize.arraySizeDefinition()}"
    }
}