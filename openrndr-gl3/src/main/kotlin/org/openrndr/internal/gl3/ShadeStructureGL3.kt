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
                        fragmentTransform = shadeStyle.fragmentTransform
                        vertexPreamble = shadeStyle.vertexPreamble
                        fragmentPreamble = shadeStyle.fragmentPreamble
                        measure("outputs") {
                            outputs = shadeStyle.outputs.map { "// -- output-from  ${it.value} \nlayout(location = ${it.value.attachment}) out ${it.value.glslType} o_${it.key};\n" }.joinToString("")
                        }
                        measure("uniforms") {
                            uniforms = shadeStyle.parameters.map { "uniform ${mapType(it.value)} p_${it.key};\n" }.joinToString("")
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

private fun mapType(type: String): String {
    val tokens = type.split(",")
    val arraySize = tokens.getOrNull(1)
    return when (tokens[0]) {
        "Boolean", "boolean" -> "bool"
        "Int", "int" -> "int"
        "Matrix33" -> "mat3"
        "Matrix44" -> "mat4${if (arraySize != null) "[$arraySize]" else ""}"
        "Float", "float" -> "float"
        "Vector2" -> "vec2${if (arraySize != null) "[$arraySize]" else ""}"
        "Vector3" -> "vec3${if (arraySize != null) "[$arraySize]" else ""}"
        "Vector4" -> "vec4${if (arraySize != null) "[$arraySize]" else ""}"
        "ColorRGBa" -> "vec4"
        "BufferTexture" -> "samplerBuffer"
        "ColorBuffer" -> "sampler2D"
        "ColorBuffer_UINT" -> "usampler2D"
        "ColorBuffer_SINT" -> "isampler2D"
        "DepthBuffer" -> "sampler2D"
        "Cubemap" -> "samplerCube"
        "ArrayCubemap" -> "samplerCubeArray"
        "ArrayTexture" -> "sampler2DArray"
        "ArrayTexture_UINT" -> "usampler2DArray"
        "ArrayTexture_SINT" -> "isampler2DArray"
        else -> throw RuntimeException("unsupported type $type")
    }
}

private val ShadeStyleOutput.glslType: String
    get() {
        return when (val c = Pair(this.format.componentCount, this.type.colorSampling)) {
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
