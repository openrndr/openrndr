package org.openrndr.draw

import org.openrndr.color.ColorRGBa
import org.openrndr.math.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * A class representing a parameter that supports getting and setting typed values with automatic type tracking.
 *
 * @param R The type of the parameter value, which must be a non-nullable type.
 * @property parameterValues A mutable map storing parameter names and their assigned values.
 * @property parameterTypes An observable map that tracks parameter names and their corresponding types.
 * @property customName A custom name for the parameter. If null, the property name will be used.
 * @constructor Initializes the parameter with an optional custom name and initial value. If an initial value
 * is provided, it is assigned to the parameter using the `customName` if provided, or "value" by default.
 */
class Parameter<R : Any>(
    val parameterValues: MutableMap<String, Any>,
    val parameterTypes: ObservableHashmap<String, String>,
    val customName: String? = null,
    initialValue: R? = null
) : ReadWriteProperty<Any, R> {

    init {
        if (initialValue != null && customName != null) {
            setValue(customName, initialValue)
        } else if (initialValue != null && customName == null) {
            error("customName must be provided if initialValue is provided")
        }
    }

    override fun getValue(thisRef: Any, property: KProperty<*>): R {
        @Suppress("UNCHECKED_CAST")
        return parameterValues[customName ?: property.name] as R
    }

    /**
     * Sets the given value for the specified parameter name and determines its type.
     *
     * @param name The name of the parameter to which the value will be assigned.
     * @param value The value to be assigned to the parameter. The type of the value determines the type stored.
     */
    fun setValue(name: String, value: R) {
        parameterValues[name] = value
        parameterTypes[name] = when (value) {
            is DynamicParameter -> value.parameterType
            is DynamicParameterSource -> DYNAMIC_SOURCE_TYPE
            is Boolean -> "boolean"
            is Int -> "int"
            is Double -> "float"
            is Float -> "float"
            is IntArray -> arrayType(parameterTypes[name], "int", ", ", value.size)
            is FloatArray -> arrayType(parameterTypes[name], "float", ", ", value.size)
            is Vector2 -> "Vector2"
            is Vector3 -> "Vector3"
            is Vector4 -> "Vector4"
            is IntVector2 -> "IntVector2"
            is IntVector3 -> "IntVector3"
            is IntVector4 -> "IntVector4"
            is BooleanVector2 -> "BooleanVector2"
            is BooleanVector3 -> "BooleanVector3"
            is BooleanVector4 -> "BooleanVector4"
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
                            arrayType(parameterTypes[name], "Matrix44", ", ", value.size)
                        }

                        is Vector2 -> {
                            arrayType(parameterTypes[name], "Vector2", ", ", value.size)
                        }

                        is Vector3 -> {
                            arrayType(parameterTypes[name], "Vector3", ", ", value.size)
                        }

                        is Vector4 -> {
                            arrayType(parameterTypes[name], "Vector4", ", ", value.size)
                        }

                        is ColorRGBa -> {
                            arrayType(parameterTypes[name], "ColorRGBa", ", ", value.size)
                        }

                        is Double -> {
                            arrayType(parameterTypes[name], "float", ", ", value.size)
                        }

                        is IntVector2 -> {
                            arrayType(parameterTypes[name], "IntVector2", ", ", value.size)
                        }

                        is IntVector3 -> {
                            arrayType(parameterTypes[name], "IntVector3", ", ", value.size)
                        }

                        is IntVector4 -> {
                            arrayType(parameterTypes[name], "IntVector4", ", ", value.size)
                        }

                        is CastableToVector4 -> {
                            arrayType(parameterTypes[name], "Vector4", ", ", value.size)
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

    override fun setValue(thisRef: Any, property: KProperty<*>, value: R) {
        setValue(customName ?: property.name, value)
    }
}

/**
 * An interface designed to handle and manage shader style parameters. It provides functionality
 * for associating named parameters with various data types and maintaining type information.
 * These parameters are intended to be used in shader programming for configuring and managing
 * graphical rendering styles.
 */
interface StyleParameters {
    var parameterValues: MutableMap<String, Any>
    var parameterTypes: ObservableHashmap<String, String>
    var textureBaseIndex: Int

    /**
     * Creates a `Parameter` property with the specified constraints and associates it with a custom name or the property name.
     *
     * @param customName An optional custom name to be used as the key for this parameter. If null, the property name will be used.
     * @return A `Parameter` instance with the assigned type and optional custom name.
     */
    fun <R : Any> Parameter(customName: String? = null, initialValue: R? = null): Parameter<R> {
        return Parameter(parameterValues, parameterTypes, customName, initialValue)
    }

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

    fun parameter(name: String, value: Array<Matrix33>) {
        parameterValues[name] = value
        setArrayType(name, shadeStyleType<Matrix33>(), ",", value.size)
    }

    fun parameter(name: String, value: Array<Matrix44>) {
        parameterValues[name] = value
        setArrayType(name, shadeStyleType<Matrix44>(), ",", value.size)
    }

    fun parameter(name: String, value: Array<Vector2>) {
        parameterValues[name] = value
        setArrayType(name, shadeStyleType<Vector2>(), ",", value.size)
    }

    fun parameter(name: String, value: Array<Vector3>) {
        parameterValues[name] = value
        setArrayType(name, shadeStyleType<Vector3>(), ",", value.size)
    }

    fun parameter(name: String, value: Array<Vector4>) {
        parameterValues[name] = value
        setArrayType(name, shadeStyleType<Vector4>(), ",", value.size)
    }

    fun parameter(name: String, value: Array<ColorRGBa>) {
        parameterValues[name] = value
        setArrayType(name, shadeStyleType<ColorRGBa>(), ",", value.size)
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

    fun parameter(name: String, value: BooleanVector2) {
        parameterValues[name] = value
        parameterTypes[name] = shadeStyleType<BooleanVector2>()
    }

    fun parameter(name: String, value: BooleanVector3) {
        parameterValues[name] = value
        parameterTypes[name] = shadeStyleType<BooleanVector3>()
    }

    fun parameter(name: String, value: BooleanVector4) {
        parameterValues[name] = value
        parameterTypes[name] = shadeStyleType<BooleanVector4>()
    }

    fun parameter(name: String, value: ColorRGBa) {
        parameterValues[name] = value
        parameterTypes[name] = shadeStyleType<ColorRGBa>()
    }

    /**
     * Sets [name] to a parameter that is resolved in the shader, read from a texture or computed by GLSL code
     * @see textureParameter
     * @see glslParameter
     */
    fun parameter(name: String, value: DynamicParameter) {
        parameterValues[name] = value
        parameterTypes[name] = value.parameterType
    }

    /**
     * Sets [name] to the dynamic parameter that [value] provides, it is read from [value] when the style is used
     * @see DynamicParameterSource
     */
    fun parameter(name: String, value: DynamicParameterSource) {
        parameterValues[name] = value
        parameterTypes[name] = DYNAMIC_SOURCE_TYPE
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
        setArrayType(name, shadeStyleType<Int>(), ", ", value.size)
    }

    fun parameter(name: String, value: DoubleArray) {
        parameterValues[name] = value
        setArrayType(name, shadeStyleType<Double>(), ", ", value.size)
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
}

inline fun <reified T : Struct<T>> StyleParameters.parameter(name: String, value: T) {
    parameterValues[name] = value
    parameterTypes[name] = "struct ${T::class.simpleName}"
}

inline fun <reified T : Struct<T>> StyleParameters.parameter(name: String, value: Array<T>) {
    parameterValues[name] = value
    parameterTypes[name] = "struct ${T::class.simpleName},${value.size}"
}

/**
 * The type of an array parameter, `<type><separator><size>`: [existing] when it is that type already, so that setting
 * an array of the same type and size every frame does not make a string
 */
internal fun arrayType(existing: String?, type: String, separator: String, size: Int): String {
    if (existing != null && existing.length > type.length + separator.length &&
        existing.startsWith(type) && existing.startsWith(separator, type.length)
    ) {
        val digits = existing.length - type.length - separator.length
        var parsed = 0
        var valid = digits <= 9
        for (i in existing.length - digits until existing.length) {
            val c = existing[i]
            if (c !in '0'..'9') { valid = false; break }
            parsed = parsed * 10 + (c - '0')
        }
        if (valid && parsed == size) return existing
    }
    return "$type$separator$size"
}

/** sets the type of the array parameter [name], see [arrayType] */
internal fun StyleParameters.setArrayType(name: String, type: String, separator: String, size: Int) {
    parameterTypes[name] = arrayType(parameterTypes[name], type, separator, size)
}

/**
 * The type of parameters that are [DynamicParameterSource]s, their structure is the structure of the dynamic parameter
 * they provide when the style is used
 */
internal const val DYNAMIC_SOURCE_TYPE = "dynamic source"
