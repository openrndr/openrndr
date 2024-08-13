package org.openrndr.internal.glcommon

import org.openrndr.draw.*
import org.openrndr.draw.font.BufferAccess

fun <T> T.structDefinitions(): String where T : StyleParameters, T : StyleBufferBindings {
    val structs = parameterTypes.filterValues {
        it.startsWith("struct")
    } + bufferTypes.filterValues { it.startsWith("struct") }
    val structValues = structs.keys.map {
        if ((parameterValues[it] ?: bufferValues[it]) is Array<*>) {
            @Suppress("UNCHECKED_CAST") val array = (parameterValues[it] ?: bufferValues[it]) as Array<Struct<*>>
            Pair(it, array.first())
        } else {
            Pair(
                it, (parameterValues[it] ?: ((bufferValues[it] as? StructuredBuffer<*>)?.struct))!! as Struct<*>
            )
        }
    }
    val structProtoValues = structValues.distinctBy {
        it.second::class.simpleName
    }
    return structProtoValues.joinToString("\n") {
        it.second.typeDef(
            (parameterTypes[it.first]
                ?: bufferTypes[it.first])!!.split(" ")[1].split(",")[0]
        )
    }
}

fun StyleParameters.uniforms(): String {
    return parameterTypes.map { mapTypeToUniform(it.value, it.key, ) }.joinToString("\n")
}

fun StyleImageBindings.images(): String {
    return imageTypes.map {
        val tokens = it.value.split(",")
        val subtokens = tokens[0].split(" ")
        when (subtokens[0]) {
            "Image2D", "Image3D", "ImageCube", "Image2DArray", "ImageBuffer", "ImageCubeArray" -> {
                val sampler = tokens[0].take(1).lowercase() + tokens[0].drop(1)
                val colorFormat = ColorFormat.valueOf(tokens[1])
                val colorType = ColorType.valueOf(tokens[2])

                val layout = imageLayout(colorFormat, colorType)
                val samplerType = when (colorType.colorSampling) {
                    ColorSampling.SIGNED_INTEGER -> "i"
                    ColorSampling.UNSIGNED_INTEGER -> "u"
                    else -> ""
                }
                val arraySpec = when(val length = imageArrayLength[it.key]) {
                    -1 -> ""
                    0 -> error("zero-sized arrays are not supported")
                    else -> "[$length]"
                }

                listOf("layout($layout, binding = ${imageBindings[it.key]})",
                    (imageFlags[it.key] ?: emptySet()).joinToString(" ") { flag -> flag.glsl },
                    (imageAccess[it.key] ?: ImageAccess.READ_WRITE).glsl,
                    "uniform $samplerType$sampler p_${it.key}${arraySpec};").joinToString(" ")
            }

            else -> {
                error("unknown image type '${subtokens[0]}")
            }
        }
    }.joinToString("\n")
}

fun StyleBufferBindings.buffers(): String {
    var bufferIndex = 2

    return bufferValues.map {
        val r = when (val v = it.value) {
            is StructuredBuffer<*> -> {
                listOf(
                    "layout(std430, binding = $bufferIndex)",
                    (bufferFlags[it.key] ?: emptySet()).joinToString(" ") { flag -> flag.glsl },
                    (bufferAccess[it.key] ?: BufferAccess.READ_WRITE).glsl,
                    "buffer B_${it.key} { ${v.struct.typeDef("", true)} } b_${it.key};"
                ).joinToString(" ")
            }

            is ShaderStorageBuffer -> "layout(std430, binding = $bufferIndex) buffer B_${it.key} { ${v.format.glslLayout} } b_${it.key};"
            is AtomicCounterBuffer -> "layout(binding = $bufferIndex, offset = 0) uniform atomic_uint b_${it.key}[${(it.value as AtomicCounterBuffer).size}];"

            else -> error("unsupported buffer type: $v")
        }
        bufferIndex++
        r
    }.joinToString("\n")
}