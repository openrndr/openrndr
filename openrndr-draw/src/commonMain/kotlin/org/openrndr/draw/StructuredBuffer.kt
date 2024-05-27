package org.openrndr.draw

class StructuredBuffer<T : Struct<T>>(val struct: T, val ssbo: ShaderStorageBuffer)// : ShaderStorageBuffer by ssbo

/**
 * Create a [StructuredBuffer] from [struct]
 */
fun <T : Struct<T>> structuredBuffer(struct: T): StructuredBuffer<T> {
    val ssf = structToShaderStorageFormat(struct)
    return StructuredBuffer(struct, shaderStorageBuffer(ssf))
}