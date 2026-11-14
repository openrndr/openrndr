package org.openrndr.internal.glcommon

import io.github.oshai.kotlinlogging.KotlinLogging
import org.openrndr.draw.*
import org.openrndr.internal.Driver
import org.openrndr.utils.collections.LRUCache

private val logger = KotlinLogging.logger {  }

fun ComputeStyle.structure(): ComputeStructure {
    return ComputeStructure(
        structDefinitions = structDefinitions(),
        uniforms = uniforms(),
        buffers = listOf(buffers(), images()).joinToString("\n"),
        computeTransform = computeTransform,
        computePreamble = computePreamble,
        workGroupSize = workGroupSize
    )
}

/**
 * What the structure of a compute style was made from besides its [structure version][ComputeStyle.structureVersion]:
 * its bindings, which change without changing the version. The layout of a buffer depends on its type and size.
 */
private class ComputeBindings(style: ComputeStyle) {
    val buffers = style.buffers.toMap()
    val bufferTypes = style.bufferTypes.toMap()
    val bufferAccess = style.bufferAccess.toMap()
    val bufferFlags = style.bufferFlags.toMap()
    val bufferLayouts = style.bufferValues.mapValues { bufferLayout(it.value) }
    val imageBindings = style.imageBindings.toMap()
    val imageTypes = style.imageTypes.toMap()
    val imageAccess = style.imageAccess.toMap()
    val imageFlags = style.imageFlags.toMap()
    val imageArrayLength = style.imageArrayLength.toMap()

    /** are the bindings of [style] the same? Compares without copying. */
    fun matches(style: ComputeStyle): Boolean {
        if (buffers != style.buffers || bufferTypes != style.bufferTypes || bufferAccess != style.bufferAccess ||
            bufferFlags != style.bufferFlags || imageBindings != style.imageBindings || imageTypes != style.imageTypes ||
            imageAccess != style.imageAccess || imageFlags != style.imageFlags ||
            imageArrayLength != style.imageArrayLength
        ) return false
        val values = style.bufferValues
        if (values.size != bufferLayouts.size) return false
        for (entry in values.entries) {
            val layout = bufferLayouts[entry.key] ?: return false
            if (!layout.matches(entry.value)) return false
        }
        return true
    }
}

/** the parts of a buffer value that its declaration in a compute shader depends on */
private class BufferLayout(val type: Any, val size: Int) {
    fun matches(value: Any): Boolean = when (value) {
        is StructuredBuffer<*> -> type === value.struct::class
        is ShaderStorageBuffer -> type == value.format
        is AtomicCounterBuffer -> type === AtomicCounterBuffer::class && size == value.size.toInt()
        is IndexBuffer -> type === IndexBuffer::class && size == value.indexCount
        is CommandBuffer<*> -> type == value.type && size == value.size.toInt()
        else -> type === value::class
    }
}

private fun bufferLayout(value: Any): BufferLayout = when (value) {
    is StructuredBuffer<*> -> BufferLayout(value.struct::class, 0)
    is ShaderStorageBuffer -> BufferLayout(value.format, 0)
    is AtomicCounterBuffer -> BufferLayout(AtomicCounterBuffer::class, value.size.toInt())
    is IndexBuffer -> BufferLayout(IndexBuffer::class, value.indexCount)
    is CommandBuffer<*> -> BufferLayout(value.type, value.size.toInt())
    else -> BufferLayout(value::class, 0)
}

/** a structure of a compute style and what it was made from */
private class CachedComputeStructure(
    val structureVersion: Long,
    val bindings: ComputeBindings,
    val structure: ComputeStructure
)

class ComputeStyleManagerGLCommon : ComputeStyleManager(), StyleManagerDispatchUniform,
    StyleManagerDispatchBufferBindings, StyleManagerDispatchImageBindings {

    val shaders = mutableMapOf<ComputeStructure, ComputeShader>()
    override var textureIndex: Int = 2
    override var imageIndex: Int = 0
    override val uniformNames = UniformNames()

    /** the structures of compute styles by the identity of the style */
    private val structures = LRUCache<ComputeStyle, CachedComputeStructure>(256)

    /** the structure of [style], made again when its structure version or its bindings changed */
    private fun structureOf(style: ComputeStyle): ComputeStructure {
        val cached = structures.get(style)
        if (cached != null && cached.structureVersion == style.structureVersion && cached.bindings.matches(style)) {
            return cached.structure
        }
        val structure = style.structure()
        structures.set(style, CachedComputeStructure(style.structureVersion, ComputeBindings(style), structure))
        return structure
    }

    override fun shader(computeStyle: ComputeStyle, name: String): ComputeShader {
        // dynamic parameters are read through generated resolver functions
        val style = computeStyle.resolveDynamicParameters()
        val structure = structureOf(style)
        val shader = shaders.getOrPut(structure) {
            val code = """${Driver.instance.shaderConfiguration(ShaderType.COMPUTE)}
layout(local_size_x = ${structure.workGroupSize.x}, local_size_y = ${structure.workGroupSize.y}, local_size_z = ${structure.workGroupSize.z}) in;

${structure.structDefinitions ?: ""}
${structure.uniforms ?: ""}
${structure.buffers ?: ""}

${structure.computePreamble}

void main() {
${structure.computeTransform.prependIndent("    ")}
}"""
            val computeShader = Driver.instance.createComputeShader(code, name)
            logger.debug { "created compute shader '$name', ${computeShader} " }
            computeShader
        }

        dispatchBufferBindings(style, shader)
        dispatchParameters(style, shader, shader.textureBindings)
        dispatchImageBindings(style, shader)

        return shader
    }
}
