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
    return parameterTypes.map { mapTypeToUniform(it.value, it.key) }.joinToString("\n")
}

fun StyleImageBindings.images(): String {
    return imageTypes.map {
        mapTypeToImage(
            it.key,
            it.value,
            imageAccess[it.key] ?: error("no image access for '${it.key}'")
        )
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