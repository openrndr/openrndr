package org.openrndr.internal.gl3

import org.openrndr.draw.*
import org.openrndr.measure

fun array(item: VertexElement): String = if (item.arraySize == 1) "" else "[${item.arraySize}]"

interface Cache<K, V> {
    val size: Int
    operator fun set(key: K, value: V)
    operator fun get(key: K): V?
    fun remove(key: K): V?
    fun clear()
}

class PerpetualCache<K, V> : Cache<K, V> {
    private val cache = HashMap<K, V>()
    override val size: Int
        get() = cache.size

    override fun set(key: K, value: V) {
        this.cache[key] = value
    }

    override fun remove(key: K) = this.cache.remove(key)
    override fun get(key: K) = this.cache[key]
    override fun clear() = this.cache.clear()
}

class LRUCache<K, V>(private val delegate: Cache<K, V>, private val minimalSize: Int = DEFAULT_SIZE) : Cache<K, V> by delegate {
    inline fun getOrSet(key: K, forceSet: Boolean, crossinline valueFunction: () -> V): V {
        val v = measure("LRUCache-lookup") { get(key) }
        return if (forceSet || v == null) {
            val n = valueFunction()
            set(key, n)
            n
        } else {
            v
        }
    }

    private val keyMap = object : LinkedHashMap<K, Boolean>(minimalSize, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, Boolean>): Boolean {
            val tooManyCachedItems = size > minimalSize
            if (tooManyCachedItems) eldestKeyToRemove = eldest.key
            return tooManyCachedItems
        }
    }

    private var eldestKeyToRemove: K? = null

    override fun set(key: K, value: V) {
        delegate[key] = value
        cycleKeyMap(key)
    }

    override fun get(key: K): V? {
        keyMap[key]
        return delegate[key]
    }

    override fun clear() {
        keyMap.clear()
        delegate.clear()
    }

    private fun cycleKeyMap(key: K) {
        keyMap[key] = PRESENT
        eldestKeyToRemove?.let { delegate.remove(it) }
        eldestKeyToRemove = null
    }

    companion object {
        private const val DEFAULT_SIZE = 10000
        private const val PRESENT = true
    }
}

data class CacheEntry(val shadeStyle: ShadeStyle?, val vertexFormats: List<VertexFormat>, val instanceAttributeFormats: List<VertexFormat>)

private val shadeStyleCache = LRUCache<CacheEntry, ShadeStructure>(PerpetualCache())

fun structureFromShadeStyle(shadeStyle: ShadeStyle?, vertexFormats: List<VertexFormat>, instanceAttributeFormats: List<VertexFormat>): ShadeStructure {
    return measure("structureFromShadeStyle") {

        val cacheEntry = CacheEntry(shadeStyle, vertexFormats, instanceAttributeFormats)

        shadeStyleCache.getOrSet(cacheEntry, shadeStyle?.dirty ?: false) {

            measure("miss") {
                shadeStyle?.dirty = false

                ShadeStructure().apply {
                    if (shadeStyle != null) {
                        vertexTransform = shadeStyle.vertexTransform
                        geometryTransform = shadeStyle.geometryTransform
                        fragmentTransform = shadeStyle.fragmentTransform
                        vertexPreamble = shadeStyle.vertexPreamble
                        geometryPreamble = shadeStyle.geometryPreamble
                        fragmentPreamble = shadeStyle.fragmentPreamble
                        measure("outputs") {
                            outputs = shadeStyle.outputs.map { "// -- output-from  ${it.value} \nlayout(location = ${it.value.attachment}) out ${it.value.glslType} o_${it.key};\n" }.joinToString("")
                        }
                        measure("uniforms") {
                            uniforms = shadeStyle.parameters.map { "${mapTypeToUniform(it.value, it.key)}"}.joinToString("\n")
                        }

                        measure("buffers") {
                            var bufferIndex = 2
                            buffers = shadeStyle.bufferValues.map {
                                val r  =when (val v = it.value) {
                                    is ShaderStorageBuffer -> "layout(std430, binding = $bufferIndex) buffer B_${it.key} { ${v.format.glslLayout} } b_${it.key};"
                                    else -> error("unsupported buffer type: $v")
                                }
                                bufferIndex++
                                r
                            }.joinToString("\n")
                        }

                    }
                    measure("varying-out") {
                        varyingOut = vertexFormats.flatMap { it.items }.joinToString("") { "${it.type.glslVaryingQualifier}out ${it.type.glslType} va_${it.attribute}${array(it)};\n" } +
                                instanceAttributeFormats.flatMap { it.items }.joinToString("") { "${it.type.glslVaryingQualifier}out ${it.type.glslType} vi_${it.attribute}${array(it)};\n" }
                    }
                    measure("varying-in") {
                        varyingIn = vertexFormats.flatMap { it.items }.joinToString("") { "${it.type.glslVaryingQualifier}in ${it.type.glslType} va_${it.attribute}${array(it)};\n" } +
                                instanceAttributeFormats.flatMap { it.items }.joinToString("") { "${it.type.glslVaryingQualifier}in ${it.type.glslType} vi_${it.attribute}${array(it)};\n" }
                    }
                    measure("varying-bridge") {
                        varyingBridge = vertexFormats.flatMap { it.items }.joinToString("") { "    va_${it.attribute} = a_${it.attribute};\n" } +
                                instanceAttributeFormats.flatMap { it.items }.joinToString("") { "vi_${it.attribute} = i_${it.attribute};\n" }
                    }
                    measure("attributes") {
                        attributes = vertexFormats.flatMap { it.items }.joinToString("") { "in ${it.type.glslType} a_${it.attribute}${array(it)};\n" } +
                                instanceAttributeFormats.flatMap { it.items }.joinToString("") { "in ${it.type.glslType} i_${it.attribute}${array(it)};\n" }
                    }
                    suppressDefaultOutput = shadeStyle?.suppressDefaultOutput ?: false
                }
            }
        }
    }
}

private fun mapTypeToUniform(type: String, name: String): String {
    val tokens = type.split(",")
    val arraySize = tokens.getOrNull(1)
    val u = "uniform"

    fun String?.arraySizeDefinition() = if (this == null) {
        ""
    } else {
        "\n#define p_${name}_SIZE $arraySize"
    }
    return when (tokens[0]) {
        "Boolean", "boolean" -> "$u bool;"
        "Int", "int" -> "$u int${if (arraySize != null) "[$arraySize]" else ""} p_$name; ${arraySize.arraySizeDefinition()}"
        "Matrix33" -> "$u mat3 p_$name; ${arraySize.arraySizeDefinition()}"
        "Matrix44" -> "$u mat4${if (arraySize != null) "[$arraySize]" else ""} p_$name; ${arraySize.arraySizeDefinition()}"
        "Float", "float" -> "$u float${if (arraySize != null) "[$arraySize]" else ""} p_$name; ${arraySize.arraySizeDefinition()}"
        "Vector2" -> "$u vec2${if (arraySize != null) "[$arraySize]" else ""} p_$name; ${arraySize.arraySizeDefinition()}"
        "Vector3" -> "$u vec3${if (arraySize != null) "[$arraySize]" else ""} p_$name; ${arraySize.arraySizeDefinition()}"
        "Vector4" -> "$u vec4${if (arraySize != null) "[$arraySize]" else ""} p_$name; ${arraySize.arraySizeDefinition()}"
        "IntVector2" -> "$u ivec2${if (arraySize != null) "[$arraySize]" else ""} p_$name; ${arraySize.arraySizeDefinition()}"
        "IntVector3" -> "$u ivec3${if (arraySize != null) "[$arraySize]" else ""} p_$name; ${arraySize.arraySizeDefinition()}"
        "IntVector4" -> "$u ivec4${if (arraySize != null) "[$arraySize]" else ""} p_$name; ${arraySize.arraySizeDefinition()}"
        "ColorRGBa" -> "$u vec4${if (arraySize != null) "[$arraySize]" else ""} p_$name; ${arraySize.arraySizeDefinition()}"
        "BufferTexture" -> "$u samplerBuffer p_$name;"
        "BufferTexture_UINT" -> "$u usamplerBuffer p_$name;"
        "BufferTexture_SINT" -> "$u isamplerBuffer p_$name;"
        "ColorBuffer" -> "$u sampler2D p_$name;"
        "ColorBuffer_UINT" -> "$u usampler2D p_$name;"
        "ColorBuffer_SINT" -> "$u isampler2D p_$name;"
        "DepthBuffer" -> "$u sampler2D p_$name;"
        "Cubemap" -> "$u samplerCube p_$name;"
        "Cubemap_UINT" -> "$u usamplerCube p_$name;"
        "Cubemap_SINT" -> "$u isamplerCube p_$name;"
        "ArrayCubemap" -> "$u samplerCubeArray p_$name;"
        "ArrayCubemap_UINT" -> "$u usamplerCubeArray p_$name;"
        "ArrayCubemap_SINT" -> "$u isamplerCubeArray p_$name;"
        "ArrayTexture" -> "$u sampler2DArray p_$name;"
        "ArrayTexture_UINT" -> "$u usampler2DArray p_$name;"
        "ArrayTexture_SINT" -> "$u isampler2DArray p_$name;"
        "VolumeTexture" -> "$u sampler3D p_$name;"
        "VolumeTexture_UINT" -> "$u usampler3D p_$name;"
        "VolumeTexture_SINT" -> "$u isampler3D p_$name;"
        "Image2D", "Image3D", "ImageCube", "Image2DArray", "ImageBuffer", "ImageCubeArray" -> {
            val sampler = tokens[0].take(1).toLowerCase() + tokens[0].drop(1)
            val format = ColorFormat.valueOf(tokens[1])
            val type = ColorType.valueOf(tokens[2])
            val access = ImageAccess.valueOf(tokens[3])
            val layout = imageLayout(format, type)
            when (access) {
                ImageAccess.READ, ImageAccess.READ_WRITE -> "layout($layout) $u $sampler p_$name;"
                ImageAccess.WRITE -> "writeonly $u $sampler"
            }
        }
        else -> throw RuntimeException("unsupported type $type")
    }
}

private fun imageLayout(format: ColorFormat, type: ColorType): String {
    return when (Pair(format, type)) {
        Pair(ColorFormat.R, ColorType.UINT8) -> "r8"
        Pair(ColorFormat.R, ColorType.UINT8_INT) -> "r8u"
        Pair(ColorFormat.R, ColorType.SINT8_INT) -> "r8i"
        Pair(ColorFormat.R, ColorType.UINT16) -> "r16"
        Pair(ColorFormat.R, ColorType.UINT16_INT) -> "r16u"
        Pair(ColorFormat.R, ColorType.SINT16_INT) -> "r16i"
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


private val ShadeStyleOutput.glslType: String
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


private val VertexElementType.glslType: String
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

private val VertexElementType.glslVaryingQualifier: String
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

private val ShaderStorageFormat.glslLayout: String
    get() = items.map {
        if (it.arraySize == 1) {
            "${it.type.glslType} ${it.attribute};"
        } else {
            "${it.type.glslType}[${it.arraySize}] ${it.attribute};"
        }
    }.joinToString("\n")