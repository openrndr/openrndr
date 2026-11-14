package org.openrndr.internal.glcommon

import org.openrndr.draw.*
import org.openrndr.utils.collections.LRUCache

/**
 * The structures of a shade style, one for every combination of vertex formats it is used with, made for the
 * [structureVersion][ShadeStyle.structureVersion] they hold
 */
private class StyleStructures {
    class Entry(
        val vertexFormats: List<VertexFormat>,
        val instanceAttributeFormats: List<VertexFormat>,
        var structureVersion: Long,
        var structure: ShadeStructure
    )

    val entries = ArrayList<Entry>(2)
}

/** the structures of shade styles by the identity of the style */
private val shadeStyleCache = LRUCache<ShadeStyle?, StyleStructures>()

/** do [a] and [b] hold equal formats? Compares the identities first and does not allocate. */
private fun sameFormats(a: List<VertexFormat>, b: List<VertexFormat>): Boolean {
    if (a === b) return true
    if (a.size != b.size) return false
    for (i in a.indices) {
        val fa = a[i]
        val fb = b[i]
        if (fa !== fb && fa != fb) return false
    }
    return true
}

private fun array(item: VertexElement): String = if (item.arraySize == 1) "" else "[${item.arraySize}]"

/**
 * The structure of [shadeStyle] for [vertexFormats] and [instanceAttributeFormats]. Structures are cached by the
 * identity of the style, its structure version and the formats, so a style used with several formats has a structure
 * for each of them.
 */
fun structureFromShadeStyle(
    shadeStyle: ShadeStyle?,
    vertexFormats: List<VertexFormat>,
    instanceAttributeFormats: List<VertexFormat>
): ShadeStructure {
    val version = shadeStyle?.structureVersion ?: 0L
    val structures = shadeStyleCache.getOrSet(shadeStyle) { StyleStructures() }
    val entries = structures.entries
    for (i in entries.indices) {
        val entry = entries[i]
        if (sameFormats(entry.vertexFormats, vertexFormats) && sameFormats(entry.instanceAttributeFormats, instanceAttributeFormats)) {
            if (entry.structureVersion != version) {
                entry.structure = buildStructure(shadeStyle, vertexFormats, instanceAttributeFormats)
                entry.structureVersion = version
            }
            return entry.structure
        }
    }
    val structure = buildStructure(shadeStyle, vertexFormats, instanceAttributeFormats)
    // the lists may be reused by the caller, the entry keeps copies
    entries.add(StyleStructures.Entry(vertexFormats.toList(), instanceAttributeFormats.toList(), version, structure))
    return structure
}

private fun buildStructure(
    shadeStyle: ShadeStyle?,
    vertexFormats: List<VertexFormat>,
    instanceAttributeFormats: List<VertexFormat>
): ShadeStructure {
    return ShadeStructure().apply {
        if (shadeStyle != null) {
            vertexTransform = shadeStyle.vertexTransform
            geometryTransform = shadeStyle.geometryTransform
            fragmentTransform = shadeStyle.fragmentTransform
            vertexPreamble = shadeStyle.vertexPreamble
            geometryPreamble = shadeStyle.geometryPreamble
            fragmentPreamble = shadeStyle.fragmentPreamble

            structDefinitions = shadeStyle.structDefinitions()
            outputs =
                shadeStyle.outputs.map { "// -- output-from ${it.value} \n" +
                        "#define OUTPUT_${it.key} \n" +
                        "layout(location = ${it.value.attachment}) out ${it.value.glslType} o_${it.key};\n" }
                    .joinToString("")
            uniforms = shadeStyle.uniforms()
            buffers = listOf(shadeStyle.buffers(), shadeStyle.images()).joinToString("\n")
        }

        varyingOut =
            vertexFormats.flatMap { it.items.filter { format -> format.attribute != "_" } }.joinToString("") {
                "${it.type.glslVaryingQualifier}out ${it.type.glslType} va_${it.attribute}${
                    array(it)
                };\n"
            } +
                    instanceAttributeFormats.flatMap { format -> format.items.filter { it.attribute != "_" } }
                        .joinToString("") {
                            "${it.type.glslVaryingQualifier}out ${it.type.glslType} vi_${it.attribute}${
                                array(it)
                            };\n"
                        }


        varyingIn = vertexFormats.flatMap { it.items.filter { format -> format.attribute != "_" } }.joinToString("") {
            "${it.type.glslVaryingQualifier}in ${it.type.glslType} va_${it.attribute}${
                array(it)
            };\n"
        } +
                instanceAttributeFormats.flatMap { format -> format.items.filter { it.attribute != "_" } }
                    .joinToString("") {
                        "${it.type.glslVaryingQualifier}in ${it.type.glslType} vi_${it.attribute}${
                            array(it)
                        };\n"
                    }

        varyingBridge = vertexFormats.flatMap { format -> format.items.filter { it.attribute != "_" } }
            .joinToString("") { "    va_${it.attribute} = a_${it.attribute};\n" } +
                instanceAttributeFormats.flatMap { format -> format.items.filter { it.attribute != "_" } }
                    .joinToString("") { "vi_${it.attribute} = i_${it.attribute};\n" }


        attributes = vertexFormats.flatMap { format -> format.items.filter { it.attribute != "_" } }
            .joinToString("") { "in ${it.type.glslType} a_${it.attribute}${array(it)};\n" } +
                instanceAttributeFormats.flatMap { format -> format.items.filter { it.attribute != "_" } }
                    .joinToString("") { "in ${it.type.glslType} i_${it.attribute}${array(it)};\n" }

        suppressDefaultOutput = shadeStyle?.suppressDefaultOutput ?: false
    }
}



