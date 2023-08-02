package org.openrndr.draw

import org.openrndr.color.ColorRGBa
import org.openrndr.math.*

interface StyleParameters {
    var parameterValues: MutableMap<String, Any>
    var parameterTypes: ObservableHashmap<String, String>

    var textureBaseIndex: Int

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



}

inline fun <reified T : Struct<T>> StyleParameters.parameter(name: String, value: T) {
    parameterValues[name] = value
    parameterTypes[name] = "struct ${T::class.simpleName}"
}

inline fun <reified T : Struct<T>> StyleParameters.parameter(name: String, value: Array<T>) {
    parameterValues[name] = value
    parameterTypes[name] = "struct ${T::class.simpleName},${value.size}"
}
