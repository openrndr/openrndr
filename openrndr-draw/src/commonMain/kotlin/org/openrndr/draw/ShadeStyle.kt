package org.openrndr.draw

import org.openrndr.color.ColorRGBa
import org.openrndr.math.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.reflect.typeOf


open class Struct<T : Struct<T>> {
    val types = mutableMapOf<String, String>()
    val values = mutableMapOf<String, Any>()

    inner class Field<F : Any>(val type: String) {
        operator fun provideDelegate(any: Any?, kProperty: KProperty<*>): Field<F> {
            types[kProperty.name] = type
            return this
        }

        operator fun getValue(any: Any?, kProperty: KProperty<*>): F {
            return values[kProperty.name] as F
        }

        operator fun setValue(any: Any?, kProperty: KProperty<*>, value: F) {
            values[kProperty.name] = value
        }
    }

    inner class ArrayField<F : Any>(val type: String, val length: Int) {
        operator fun provideDelegate(any: Any?, kProperty: KProperty<*>): ArrayField<F> {
            types[kProperty.name] = "$type, $length"
            return this
        }

        operator fun getValue(any: Any?, kProperty: KProperty<*>): Array<F> {
            @Suppress("UNCHECKED_CAST")
            return values[kProperty.name] as Array<F>
        }

        operator fun setValue(any: Any?, kProperty: KProperty<*>, value: Array<F>) {
            values[kProperty.name] = value
        }
    }




    inline fun <reified F : Any> field(): Field<F> {
        val type = shadeStyleTypeOrNull<F>() ?: "struct ${F::class.simpleName}"
        return Field(type)
    }

    inline fun <reified F : Any> arrayField(length: Int): ArrayField<F> {
        return ArrayField(shadeStyleType<F>(), length)
    }
}


fun <T:Struct<T>> Struct<T>.typeDefImpl(name:String): String {
    var deps = ""
    for ((k,v) in values) {
        if (v is Struct<*>) {
            val type = types.getValue(k)
            deps = "$deps\n${(v as Struct<T>).typeDefImpl(type.drop(7))}"
        }
    }


    return """$deps
#ifndef STRUCT_$name
#define STRUCT_$name
struct $name {
${
        types.toList().joinToString("\n") {
            val tokens = it.second.split(", ")
            if (tokens.size == 1) {
                "${shadeStyleTypeToGLSL(it.second)} ${it.first};"
            } else {
                "${shadeStyleTypeToGLSL(tokens[0])} ${it.first}[${tokens[1]}];"
            }
        }.prependIndent("    ")
    }
};
#endif
"""
}

inline fun <reified T : Struct<T>> Struct<T>.typeDef(name: String = T::class.simpleName!!): String {
    return typeDefImpl(name)

}

inline fun <reified F> shadeStyleTypeOrNull(): String? {
    return when (F::class) {
        Boolean::class -> "boolean"
        Int::class -> "int"
        Float::class -> "float"
        Double::class -> "float"
        Vector2::class -> "Vector2"
        IntVector2::class -> "IntVector2"
        Vector3::class -> "Vector3"
        IntVector3::class -> "IntVector3"
        Vector4::class -> "Vector4"
        IntVector4::class -> "IntVector4"
        Matrix33::class -> "Matrix33"
        Matrix44::class -> "Matrix44"
        ColorRGBa::class -> "ColorRGBa"
        DepthBuffer::class -> "DepthBuffer"
        else -> "struct ${F::class.simpleName}"
    }
}

inline fun <reified F> shadeStyleType(): String {
    return shadeStyleTypeOrNull<F>() ?: error("shade style type not supported: ${F::class.simpleName}")
}

private fun shadeStyleTypeToGLSLOrNull(type: String): String? {
    return when (type) {
        shadeStyleType<Boolean>() -> "bool"
        shadeStyleType<Int>() -> "int"
        shadeStyleType<Matrix33>() -> "mat3"
        shadeStyleType<Matrix44>() -> "mat4"
        shadeStyleType<Float>(), shadeStyleType<Double>() -> "float"
        shadeStyleType<Vector2>() -> "vec2"
        shadeStyleType<Vector3>() -> "vec3"
        shadeStyleType<Vector4>(), shadeStyleType<ColorRGBa>() -> "vec4"
        "BufferTexture" -> "samplerBuffer"
        "BufferTexture_UINT" -> "usamplerBuffer"
        "BufferTexture_SINT" -> "isamplerBuffer"
        "ColorBuffer" -> "sampler2D"
        "ColorBuffer_UINT" -> "usampler2D"
        "ColorBuffer_SINT" -> "isampler2D"
        "DepthBuffer" -> "sampler2D"
        "Cubemap" -> "samplerCube"
        "Cubemap_UINT" -> "usamplerCube"
        "Cubemap_SINT" -> "isamplerCube"
        "ArrayCubemap" -> "samplerCubeArray"
        "ArrayCubemap_UINT" -> "usamplerCubeArray"
        "ArrayCubemap_SINT" -> "isamplerCubeArray"
        "ArrayTexture" -> "sampler2DArray"
        "ArrayTexture_UINT" -> "usampler2DArray"
        "ArrayTexture_SINT" -> "isampler2DArray"
        "VolumeTexture" -> "sampler3D"
        "VolumeTexture_UINT" -> "usampler3D"
        "VolumeTexture_SINT" -> "isampler3D"
        else -> if (type.startsWith("struct")) {
            type.drop(7)
        } else {
            null
        }
    }
}

fun shadeStyleTypeToGLSL(type: String): String {
    return shadeStyleTypeToGLSLOrNull(type) ?: error("unsupported type $type")
}

data class ShadeStyleOutput(
    val attachment: Int,
    val format: ColorFormat = ColorFormat.RGBa,
    val type: ColorType = ColorType.FLOAT32
)


class ObservableHashmap<K, V>(val b: MutableMap<K, V>, inline val onChange: () -> Unit) : MutableMap<K, V> by b {
    override fun put(key: K, value: V): V? {
        if (key in this) {
            if (get(key) != value) {
                onChange()
            }
        }
        return b.put(key, value)
    }

    override fun remove(key: K): V? {
        onChange()
        return b.remove(key)
    }
}

interface ShadeStyleParameters {
    var parameterValues: MutableMap<String, Any>
    var parameterTypes: ObservableHashmap<String, String>


    fun parameter(name: String, value: Cubemap) {
        parameterValues[name] = value
        parameterTypes[name] = when (value.type.colorSampling) {
            ColorSampling.UNSIGNED_INTEGER -> "Cubemap_UINT"
            ColorSampling.SIGNED_INTEGER -> "Cubemap_SINT"
            else -> "Cubemap"
        }
    }

    fun parameter(name: String, value: Boolean) {
        parameterValues[name] = value
        parameterTypes[name] = shadeStyleType<Boolean>()
    }

    fun parameter(name: String, value: Int) {
        parameterValues[name] = value
        parameterTypes[name] = shadeStyleType<Int>()
    }

    fun parameter(name: String, value: Matrix33) {
        parameterValues[name] = value
        parameterTypes[name] = shadeStyleType<Matrix33>()
    }

    fun parameter(name: String, value: Matrix44) {
        parameterValues[name] = value
        parameterTypes[name] = shadeStyleType<Matrix44>()
    }

    fun parameter(name: String, value: Array<Matrix44>) {
        parameterValues[name] = value
        parameterTypes[name] = "${shadeStyleType<Matrix44>()},${value.size}"
    }

    fun parameter(name: String, value: Array<Vector2>) {
        parameterValues[name] = value
        parameterTypes[name] = "${shadeStyleType<Vector2>()},${value.size}"
    }

    fun parameter(name: String, value: Array<Vector3>) {
        parameterValues[name] = value
        parameterTypes[name] = "${shadeStyleType<Vector3>()},${value.size}"
    }

    fun parameter(name: String, value: Array<Vector4>) {
        parameterValues[name] = value
        parameterTypes[name] = "${shadeStyleType<Vector4>()},${value.size}"
    }

    fun parameter(name: String, value: Array<ColorRGBa>) {
        parameterValues[name] = value
        parameterTypes[name] = "${shadeStyleType<ColorRGBa>()},${value.size}"
    }

    fun parameter(name: String, value: Float) {
        parameterValues[name] = value
        parameterTypes[name] = shadeStyleType<Float>()
    }

    fun parameter(name: String, value: Double) {
        parameterValues[name] = value.toFloat()
        parameterTypes[name] = shadeStyleType<Float>()
    }

    fun parameter(name: String, value: Vector2) {
        parameterValues[name] = value
        parameterTypes[name] = shadeStyleType<Vector2>()
    }

    fun parameter(name: String, value: Vector3) {
        parameterValues[name] = value
        parameterTypes[name] = shadeStyleType<Vector3>()
    }

    fun parameter(name: String, value: Vector4) {
        parameterValues[name] = value
        parameterTypes[name] = shadeStyleType<Vector4>()
    }

    fun parameter(name: String, value: IntVector2) {
        parameterValues[name] = value
        parameterTypes[name] = shadeStyleType<IntVector2>()
    }

    fun parameter(name: String, value: IntVector3) {
        parameterValues[name] = value
        parameterTypes[name] = shadeStyleType<IntVector3>()
    }

    fun parameter(name: String, value: IntVector4) {
        parameterValues[name] = value
        parameterTypes[name] = shadeStyleType<IntVector4>()
    }

    fun parameter(name: String, value: ColorRGBa) {
        parameterValues[name] = value
        parameterTypes[name] = shadeStyleType<ColorRGBa>()
    }

    fun parameter(name: String, value: ColorBuffer) {
        parameterValues[name] = value
        parameterTypes[name] = when (value.type.colorSampling) {
            ColorSampling.UNSIGNED_INTEGER -> "ColorBuffer_UINT"
            ColorSampling.SIGNED_INTEGER -> "ColorBuffer_SINT"
            else -> "ColorBuffer"
        }
    }

    fun parameter(name: String, value: DepthBuffer) {
        parameterValues[name] = value
        parameterTypes[name] = shadeStyleType<DepthBuffer>()
    }

    fun parameter(name: String, value: ArrayTexture) {
        parameterValues[name] = value
        parameterTypes[name] = when (value.type.colorSampling) {
            ColorSampling.UNSIGNED_INTEGER -> "ArrayTexture_UINT"
            ColorSampling.SIGNED_INTEGER -> "ArrayTexture_SINT"
            else -> "ArrayTexture"
        }
    }

    fun parameter(name: String, value: IntArray) {
        parameterValues[name] = value
        parameterTypes[name] = "${shadeStyleType<Int>()}, ${value.size}"
    }

    fun parameter(name: String, value: DoubleArray) {
        parameterValues[name] = value
        parameterTypes[name] = "${shadeStyleType<Double>()}, ${value.size}"
    }

    fun parameter(name: String, value: ArrayCubemap) {
        parameterValues[name] = value
        parameterTypes[name] = when (value.type.colorSampling) {
            ColorSampling.UNSIGNED_INTEGER -> "ArrayCubemap_UINT"
            ColorSampling.SIGNED_INTEGER -> "ArrayCubemap_SINT"
            else -> "ArrayCubemap"
        }
    }

    fun parameter(name: String, value: BufferTexture) {
        parameterValues[name] = value
        parameterTypes[name] = when (value.type.colorSampling) {
            ColorSampling.UNSIGNED_INTEGER -> "BufferTexture_UINT"
            ColorSampling.SIGNED_INTEGER -> "BufferTexture_SINT"
            else -> "BufferTexture"
        }
    }

    fun parameter(name: String, value: VolumeTexture) {
        parameterValues[name] = value
        parameterTypes[name] = when (value.type.colorSampling) {
            ColorSampling.UNSIGNED_INTEGER -> "VolumeTexture_UINT"
            ColorSampling.SIGNED_INTEGER -> "VolumeTexture_SINT"
            else -> "VolumeTexture"
        }
    }


    fun parameter(name: String, value: ImageBinding) {
        parameterValues[name] = value
        parameterTypes[name] = when (value) {
            is BufferTextureImageBinding -> {
                "ImageBuffer,${value.bufferTexture.format.name},${value.bufferTexture.type.name},${value.access.name}"
            }

            is CubemapImageBinding -> {
                "ImageCube,${value.cubemap.format.name},${value.cubemap.type.name},${value.access.name}"
            }

            is ArrayCubemapImageBinding -> {
                "ImageCubeArray,${value.arrayCubemap.format.name},${value.arrayCubemap.type.name},${value.access.name}"
            }

            is ColorBufferImageBinding -> {
                "Image2D,${value.colorBuffer.format.name},${value.colorBuffer.type.name},${value.access.name}"
            }

            is ArrayTextureImageBinding -> {
                "Image2DArray,${value.arrayTexture.format.name},${value.arrayTexture.type.name},${value.access.name}"
            }

            is VolumeTextureImageBinding -> {
                "Image3D,${value.volumeTexture.format.name},${value.volumeTexture.type.name},${value.access.name}"
            }

            else -> error("unsupported image binding")
        }
    }
}

inline fun <reified T : Struct<T>> ShadeStyleParameters.parameter(name: String, value: T) {
    parameterValues[name] = value
    parameterTypes[name] = "struct ${T::class.simpleName}"
}

@Suppress("unused")
open class ShadeStyle : ShadeStyleParameters {
    var dirty = true

    var vertexPreamble: String? = null
        set(value) {
            dirty = true
            field = value
        }

    var geometryPreamble: String? = null
        set(value) {
            dirty = true
            field = value
        }

    var fragmentPreamble: String? = null
        set(value) {
            dirty = true
            field = value
        }

    var vertexTransform: String? = null
        set(value) {
            dirty = true
            field = value
        }

    var geometryTransform: String? = null
        set(value) {
            dirty = true
            field = value
        }

    var fragmentTransform: String? = null
        set(value) {
            dirty = true
            field = value
        }

    var bufferValues = mutableMapOf<String, Any>()
    val buffers = mutableMapOf<String, String>()

    var outputs = ObservableHashmap(HashMap<String, ShadeStyleOutput>()) { dirty = true }
    var attributes = mutableListOf<VertexBuffer>()

    var suppressDefaultOutput = false
        set(value) {
            dirty = true
            field = value
        }

    constructor()

    constructor(other: ShadeStyle) {
        this.fragmentPreamble = other.fragmentPreamble
        this.geometryPreamble = other.geometryPreamble
        this.vertexPreamble = other.vertexPreamble

        this.fragmentTransform = other.fragmentTransform
        this.geometryTransform = other.geometryTransform
        this.vertexTransform = other.vertexTransform

        this.parameterTypes.putAll(other.parameterTypes)
        this.outputs.putAll(other.outputs)
    }

    fun output(name: String, output: ShadeStyleOutput) {
        outputs[name] = output
    }

    fun attributes(attributesBuffer: VertexBuffer) {
        attributes.add(attributesBuffer)
    }


    fun buffer(name: String, buffer: ShaderStorageBuffer) {
        bufferValues[name] = buffer
        buffers[name] = buffer.format.hashCode().toString()
    }

    fun buffer(name: String, buffer: AtomicCounterBuffer) {
        bufferValues[name] = buffer
        buffers[name] = "AtomicCounterBuffer"
    }


    operator fun plus(other: ShadeStyle): ShadeStyle {
        val s = ShadeStyle()
        s.vertexTransform = concat(vertexTransform, other.vertexTransform)
        s.fragmentTransform = concat(fragmentTransform, other.fragmentTransform)

        s.vertexPreamble =
            (if (vertexPreamble == null) "" else vertexPreamble) + "\n" + if (other.vertexPreamble == null) "" else other.vertexPreamble
        s.fragmentPreamble =
            (if (fragmentPreamble == null) "" else fragmentPreamble) + "\n" + if (other.fragmentPreamble == null) "" else other.fragmentPreamble

        s.parameterTypes.apply {
            putAll(parameterTypes)
            putAll(other.parameterTypes)
        }

        s.parameterValues.apply {
            putAll(parameterValues)
            putAll(other.parameterValues)
        }

        s.outputs.apply {
            putAll(outputs)
            putAll(other.outputs)
        }

        s.attributes.apply {
            addAll(attributes)
            addAll(other.attributes)
        }

        return s
    }

    inner class Parameter<R : Any> : ReadWriteProperty<ShadeStyle, R> {

        override fun getValue(thisRef: ShadeStyle, property: KProperty<*>): R {
            @Suppress("UNCHECKED_CAST")
            return parameterValues[property.name] as R
        }

        override fun setValue(thisRef: ShadeStyle, property: KProperty<*>, value: R) {
            parameterValues[property.name] = value
            parameterTypes[property.name] = when (value) {
                is Boolean -> "boolean"
                is Int -> "int"
                is Double -> "float"
                is Float -> "float"
                is IntArray -> "int, ${value.size}"
                is FloatArray -> "float, ${value.size}"
                is Vector2 -> "Vector2"
                is Vector3 -> "Vector3"
                is Vector4 -> "Vector4"
                is IntVector2 -> "IntVector2"
                is IntVector3 -> "IntVector3"
                is IntVector4 -> "IntVector4"
                is Matrix33 -> "Matrix33"
                is Matrix44 -> "Matrix44"
                is ColorRGBa -> "ColorRGBa"
                is BufferTexture -> when (value.type.colorSampling) {
                    ColorSampling.UNSIGNED_INTEGER -> "BufferTexture_UINT"
                    ColorSampling.SIGNED_INTEGER -> "BufferTexture_SINT"
                    else -> "BufferTexture"
                }

                is DepthBuffer -> "DepthBuffer"
                is ArrayTexture -> when (value.type.colorSampling) {
                    ColorSampling.UNSIGNED_INTEGER -> "ArrayTexture_UINT"
                    ColorSampling.SIGNED_INTEGER -> "ArrayTexture_SINT"
                    else -> "ArrayTexture"
                }

                is ArrayCubemap -> when (value.type.colorSampling) {
                    ColorSampling.UNSIGNED_INTEGER -> "ArrayCubemap_UINT"
                    ColorSampling.SIGNED_INTEGER -> "ArrayCubemap_SINT"
                    else -> "ArrayCubemap"
                }

                is Cubemap -> when (value.type.colorSampling) {
                    ColorSampling.UNSIGNED_INTEGER -> "Cubemap_UINT"
                    ColorSampling.SIGNED_INTEGER -> "Cubemap_SINT"
                    else -> "Cubemap"
                }

                is ColorBuffer -> when (value.type.colorSampling) {
                    ColorSampling.UNSIGNED_INTEGER -> "ColorBuffer_UINT"
                    ColorSampling.SIGNED_INTEGER -> "ColorBuffer_SINT"
                    else -> "ColorBuffer"
                }

                is Array<*> -> {
                    if (value.isNotEmpty()) {
                        when (value.first()) {
                            is Matrix44 -> {
                                "Matrix44, ${value.size}"
                            }

                            is Vector2 -> {
                                "Vector2, ${value.size}"
                            }

                            is Vector3 -> {
                                "Vector3, ${value.size}"
                            }

                            is Vector4 -> {
                                "Vector4, ${value.size}"
                            }

                            is ColorRGBa -> {
                                "ColorRGBa, ${value.size}"
                            }

                            is Double -> {
                                "float, ${value.size}"
                            }

                            is IntVector2 -> {
                                "IntVector2, ${value.size}"
                            }

                            is IntVector3 -> {
                                "IntVector3, ${value.size}"
                            }

                            is IntVector4 -> {
                                "IntVector4, ${value.size}"
                            }

                            is CastableToVector4 -> {
                                "Vector4, ${value.size}"
                            }

                            else -> error("unsupported array type ${(value.first()!!)::class}")
                        }
                    } else {
                        error("empty array")
                    }
                }

                is CastableToVector4 -> {
                    "Vector4"
                }

                else -> error("unsupported type ${value::class}")
            }
        }
    }

    override var parameterValues: MutableMap<String, Any> = mutableMapOf()
    override var parameterTypes: ObservableHashmap<String, String> = ObservableHashmap(mutableMapOf()) { dirty = true }
}

fun shadeStyle(builder: ShadeStyle.() -> Unit): ShadeStyle = ShadeStyle().apply(builder)

private fun concat(left: String?, right: String?): String? {
    return if (left == null && right == null) {
        null
    } else if (left == null && right != null) {
        right
    } else if (left != null && right == null) {
        left
    } else if (left != null && right != null) {
        "${isolate(left)}\n${isolate(right)}"
    } else {
        throw RuntimeException("should never happen")
    }
}

private fun isolate(code: String): String =
    if (code.startsWith("{") && code.endsWith("}")) {
        code
    } else {
        "{$code}"
    }