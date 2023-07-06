package org.openrndr.internal.glcommon

import org.openrndr.draw.*
import org.openrndr.draw.font.BufferAccess
import org.openrndr.draw.font.BufferFlag


private val shadeStyleCache = LRUCache<CacheEntry, ShadeStructure>()

private fun array(item: VertexElement): String = if (item.arraySize == 1) "" else "[${item.arraySize}]"
private data class CacheEntry(
    val shadeStyle: ShadeStyle?,
    val vertexFormats: List<VertexFormat>,
    val instanceAttributeFormats: List<VertexFormat>
)

fun structureFromShadeStyle(
    shadeStyle: ShadeStyle?,
    vertexFormats: List<VertexFormat>,
    instanceAttributeFormats: List<VertexFormat>
): ShadeStructure {
    return run {
        val cacheEntry = CacheEntry(shadeStyle, vertexFormats, instanceAttributeFormats)

        shadeStyleCache.getOrSet(cacheEntry, shadeStyle?.dirty ?: false) {
            run {
                shadeStyle?.dirty = false

                ShadeStructure().apply {
                    if (shadeStyle != null) {
                        vertexTransform = shadeStyle.vertexTransform
                        geometryTransform = shadeStyle.geometryTransform
                        fragmentTransform = shadeStyle.fragmentTransform
                        vertexPreamble = shadeStyle.vertexPreamble
                        geometryPreamble = shadeStyle.geometryPreamble
                        fragmentPreamble = shadeStyle.fragmentPreamble

                        structDefinitions = shadeStyle.structDefinitions()
                        outputs =
                            shadeStyle.outputs.map { "// -- output-from  ${it.value} \nlayout(location = ${it.value.attachment}) out ${it.value.glslType} o_${it.key};\n" }
                                .joinToString("")
                        uniforms = shadeStyle.uniforms()
                        buffers = listOf(shadeStyle.buffers(), shadeStyle.images()).joinToString("\n")
                    }

                    varyingOut =
                        vertexFormats.flatMap { it.items.filter { it.attribute != "_" } }.joinToString("") {
                            "${it.type.glslVaryingQualifier}out ${it.type.glslType} va_${it.attribute}${
                                array(it)
                            };\n"
                        } +
                                instanceAttributeFormats.flatMap { it.items.filter { it.attribute != "_" } }
                                    .joinToString("") {
                                        "${it.type.glslVaryingQualifier}out ${it.type.glslType} vi_${it.attribute}${
                                            array(it)
                                        };\n"
                                    }


                    varyingIn = vertexFormats.flatMap { it.items.filter { it.attribute != "_" } }.joinToString("") {
                        "${it.type.glslVaryingQualifier}in ${it.type.glslType} va_${it.attribute}${
                            array(it)
                        };\n"
                    } +
                            instanceAttributeFormats.flatMap { it.items.filter { it.attribute != "_" } }
                                .joinToString("") {
                                    "${it.type.glslVaryingQualifier}in ${it.type.glslType} vi_${it.attribute}${
                                        array(it)
                                    };\n"
                                }

                    varyingBridge = vertexFormats.flatMap { it.items.filter { it.attribute != "_" } }
                        .joinToString("") { "    va_${it.attribute} = a_${it.attribute};\n" } +
                            instanceAttributeFormats.flatMap { it.items.filter { it.attribute != "_" } }
                                .joinToString("") { "vi_${it.attribute} = i_${it.attribute};\n" }


                    attributes = vertexFormats.flatMap { it.items.filter { it.attribute != "_" } }
                        .joinToString("") { "in ${it.type.glslType} a_${it.attribute}${array(it)};\n" } +
                            instanceAttributeFormats.flatMap { it.items.filter { it.attribute != "_" } }
                                .joinToString("") { "in ${it.type.glslType} i_${it.attribute}${array(it)};\n" }

                    suppressDefaultOutput = shadeStyle?.suppressDefaultOutput ?: false
                }
            }
        }
    }
}



