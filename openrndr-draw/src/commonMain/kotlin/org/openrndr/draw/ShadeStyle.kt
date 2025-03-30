package org.openrndr.draw

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.font.BufferAccess
import org.openrndr.draw.font.BufferFlag
import org.openrndr.math.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

inline fun <reified F> shadeStyleTypeOrNull(): String? {
    return when (F::class) {
        Boolean::class -> "boolean"
        Int::class -> "int"
        Float::class -> "float"
        Double::class -> "float"
        Vector2::class -> "Vector2"
        IntVector2::class -> "IntVector2"
        BooleanVector2::class -> "BooleanVector2"
        Vector3::class -> "Vector3"
        IntVector3::class -> "IntVector3"
        BooleanVector3::class -> "BooleanVector3"
        Vector4::class -> "Vector4"
        IntVector4::class -> "IntVector4"
        BooleanVector4::class -> "BooleanVector4"
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
        shadeStyleType<IntVector2>() -> "ivec2"
        shadeStyleType<IntVector3>() -> "ivec3"
        shadeStyleType<IntVector4>() -> "ivec4"
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

/**
 * Represents the output configuration for a shading style.
 *
 * @property attachment The attachment index for the output.
 * @property format The color format of the output, which determines how color components (e.g., red, green, blue, alpha) are represented.
 * Defaults to `ColorFormat.RGBa`.
 * @property type The color type of the output, which specifies the data type or encoding used for handling the colors.
 * Defaults to `ColorType.FLOAT32`.
 */
data class ShadeStyleOutput(
    val attachment: Int,
    val format: ColorFormat = ColorFormat.RGBa,
    val type: ColorType = ColorType.FLOAT32
)


class ObservableHashmap<K, V>(val b: MutableMap<K, V>, val onChange: () -> Unit) : MutableMap<K, V> by b {
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


/**
 * Represents a customizable and composable style for shaders and rendering pipelines.
 * This class allows defining transformations, preambles, and shader outputs
 * as well as binding buffers and images for use in rendering.
 *
 * ShadeStyle provides mechanisms for combining different styles and managing
 * parameters for complex rendering effects.
 *
 * Primary Features:
 *
 * - Shader Transformations: Define custom vertex, geometry, and fragment transformations.
 * - Shader Preambles: Specify custom preamble code blocks for vertex, geometry, and fragment shaders.
 * - Parameter Management: Facilitate management of shader parameters and types.
 * - Outputs: Manage shader outputs such as fragment colors.
 * - Buffer and Image Bindings: Enable configuration and binding of buffers and images.
 *
 * ShadeStyle is designed for flexibility and modularity in shader-based rendering pipelines,
 * enabling advanced rendering techniques and reusable styles.
 */
@Suppress("unused")
open class ShadeStyle(
    override var parameterValues: MutableMap<String, Any> = mutableMapOf(),
    override var textureBaseIndex: Int = 2) : StyleParameters, StyleBufferBindings, StyleImageBindings {
    var dirty = true

    override var parameterTypes: ObservableHashmap<String, String> = ObservableHashmap(mutableMapOf()) { dirty = true }
    /**
     * Represents the preamble for vertex shading code in the shade style.
     * This property is used to define custom vertex processing logic and,
     * when modified, marks the state as dirty to trigger a necessary update.
     *
     * A null value indicates that no custom preamble is defined.
     */
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

    /**
     * Represents an optional preamble string for a shader's fragment stage.
     * This property allows customization of the initial setup or definitions
     * that precede main shader code for the fragment stage.
     *
     * When modified, it automatically marks the containing ShadeStyle as dirty,
     * indicating that the shader configuration has been updated and may require
     * recompilation or reprocessing.
     */
    var fragmentPreamble: String? = null
        set(value) {
            dirty = true
            field = value
        }

    /**
     * Specifies a transformation applied to the vertex shader in the form of a string.
     *
     * When the value of this property is changed, the containing shader style is marked as dirty
     * to indicate that it requires reprocessing or recompilation.
     *
     * A null value indicates that no additional vertex transformation is applied.
     */
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

    /**
     * Specifies the transformation applied to the fragment shader stage.
     * This property holds an optional string value which represents the GLSL code or expressions
     * defining the transformation logic. When the value of this property is changed, the `dirty`
     * flag is set to true, indicating that the shader needs to be recompiled or updated.
     */
    var fragmentTransform: String? = null
        set(value) {
            dirty = true
            field = value
        }

    override var bufferValues = mutableMapOf<String, Any>()
    override val buffers = mutableMapOf<String, String>()
    override val bufferTypes = mutableMapOf<String, String>()
    override val bufferFlags = mutableMapOf<String, Set<BufferFlag>>()
    override val bufferAccess = mutableMapOf<String, BufferAccess>()

    override val imageTypes: MutableMap<String, String> = mutableMapOf()
    override val imageValues: MutableMap<String, Array<out ImageBinding>> = mutableMapOf()
    override val imageAccess: MutableMap<String, ImageAccess> = mutableMapOf()
    override val imageFlags: MutableMap<String, Set<ImageFlag>> = mutableMapOf()
    override val imageArrayLength: MutableMap<String, Int> = mutableMapOf()
    override val imageBindings: MutableMap<String, Int> = mutableMapOf()


    var outputs = ObservableHashmap(HashMap<String, ShadeStyleOutput>()) { dirty = true }
    var attributes = mutableListOf<VertexBuffer>()

    var suppressDefaultOutput = false
        set(value) {
            dirty = true
            field = value
        }

    fun output(name: String, output: ShadeStyleOutput) {
        outputs[name] = output
    }

    fun attributes(attributesBuffer: VertexBuffer) {
        attributes.add(attributesBuffer)
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
}

/**
 * Creates and returns a new instance of `ShadeStyle`, configured using the provided builder function.
 *
 * @param builder A lambda function that applies custom configurations to the `ShadeStyle` instance.
 *                This lambda is called exactly once within the scope of this function.
 * @return A configured `ShadeStyle` instance. The modifications applied in the `builder` lambda
 *         are incorporated into the returned object.
 */
@OptIn(ExperimentalContracts::class)
fun shadeStyle(builder: ShadeStyle.() -> Unit): ShadeStyle {
    contract {
        callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
    }
    return ShadeStyle().apply(builder)
}

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