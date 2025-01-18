package org.openrndr.draw

import org.openrndr.internal.Driver
import kotlin.math.max

/**
 * Represents a shader storage buffer, which is an interface for managing
 * GPU memory used for reading and writing data within shaders.
 */
expect interface ShaderStorageBuffer: AutoCloseable {
    val session: Session?
    val format: ShaderStorageFormat

    /* Gives a read/write shadow for the shader storage buffer
    */
    val shadow: ShaderStorageBufferShadow

    fun clear()
    fun destroy()

    /**
     * Writes data to the shader storage buffer starting at the specified element offset.
     *
     * @param elementOffset The offset (in elements) within the buffer where writing should begin.
     *                      Defaults to 0 if not specified.
     * @param putter A lambda expression that defines the data to write to the buffer.
     *               It provides a `BufferWriterStd430` context for writing the desired data to the buffer.
     * @return The number of elements written to the buffer.
     */
    fun put(elementOffset: Int = 0, putter: BufferWriterStd430.() -> Unit): Int

    /**
     * Creates a vertex buffer view for a specific element within the shader storage buffer.
     *
     * This method allows you to extract a view of the underlying GPU memory as a vertex buffer
     * formatted according to the vertex format associated with the given element name.
     *
     * @param elementName The name of the element in the shader storage buffer to view as a vertex buffer.
     *                    If null, the view is created for the entire buffer.
     * @return A [VertexBuffer] representing the specified element or the entire buffer if no element name is provided.
     */
    fun vertexBufferView(elementName: String? = null): VertexBuffer
}

/**
 * Represents an element in shader storage, typically used in GPU programming
 * for storing data buffers. This interface defines the core properties
 * of such an element, including its name, memory offset, and size when
 * represented as an array.
 */
interface ShaderStorageElement {
    val name: String
    val offset: Int
    val arraySize: Int
}

enum class BufferPrimitiveType(val componentCount: Int, val sizeInBytes: Int, val alignmentInBytes: Int) {
    UINT32(1, 4, 4),
    INT32(1, 4, 4),
    BOOLEAN(1, 4, 4),
    FLOAT32(1, 4, 4),

    FLOAT64(1, 8, 8),

    VECTOR2_BOOLEAN(2, 8, 8),
    VECTOR2_INT32(2, 8, 8),
    VECTOR2_UINT32(2, 8, 8),
    VECTOR2_FLOAT32(2, 8, 8),

    VECTOR2_FLOAT64(2, 16, 16),

    VECTOR3_BOOLEAN(2, 12, 16),
    VECTOR3_INT32(2, 12, 16),
    VECTOR3_UINT32(2, 12, 16),
    VECTOR3_FLOAT32(3, 12, 16),

    VECTOR3_FLOAT64(3, 24, 32),

    VECTOR4_BOOLEAN(2, 16, 16),
    VECTOR4_INT32(2, 16, 16),
    VECTOR4_UINT32(2, 16, 16),
    VECTOR4_FLOAT32(4, 16, 16),

    VECTOR4_FLOAT64(4, 16, 32),

    MATRIX22_FLOAT32(4, 4 * 4, 8),
    MATRIX33_FLOAT32(9, 9 * 4, 16),
    MATRIX44_FLOAT32(16, 16 * 4, 16),
}

/**
 * Represents a primitive element within a shader storage buffer.
 * This class enables detailed definition of a primitive type and its properties
 * such as the buffer primitive type, array size, memory offset, and padding.
 **
 * @property name The name of the shader storage primitive.
 * @property type The type of the primitive, represented by the `BufferPrimitiveType` enumeration.
 * @property arraySize The number of elements in the array, default value is 1.
 * @property offset The memory offset of this primitive in the buffer.
 * @property padding Additional custom padding applied after the primitive to align subsequent data structures.
 */
data class ShaderStoragePrimitive(
    override val name: String,
    val type: BufferPrimitiveType,
    override val arraySize: Int = 1,
    override var offset: Int = 0,
    var padding: Int = 0
) : ShaderStorageElement


/**
 * Represents a structure within shader storage, typically used for
 * organizing and grouping related shader storage elements.
 *
 * @property structName The name of the structure as defined in the shader.
 * @property name The name identifier for this specific instance of the structure.
 * @property elements A list of elements contained within the structure.
 * @property arraySize The size of the structure when represented as an array. Defaults to 1.
 * @property offset The memory offset for the structure's data. Defaults to 0.
 */
data class ShaderStorageStruct(
    val structName: String,
    override val name: String,
    val elements: List<ShaderStorageElement>,
    override val arraySize: Int = 1,
    override var offset: Int = 0,
) : ShaderStorageElement

/**
 * Represents the format specification for a shader storage buffer.
 * Defines the structure, primitives, and memory layout for data
 * used in GPU programming.
 *
 * The [ShaderStorageFormat] class allows users to describe and
 * organize shader storage elements, compute memory alignments, and
 * manage offsets and paddings accordingly.
 *
 * The format consists of multiple elements that can be primitives
 * or nested structures, and it ensures that the overall memory layout
 * adheres to GPU alignment and padding requirements.
 */
class ShaderStorageFormat {
    var elements: MutableList<ShaderStorageElement> = mutableListOf()
    private var formatSize = 0

    /**
     * The size of the [ShaderStorageFormat] in bytes
     */
    val size get() = formatSize


    /**
     * Adds a custom member to the [ShaderStorageFormat]
     */
    fun primitive(name: String, type: BufferPrimitiveType, arraySize: Int = 1) {
        val item = ShaderStoragePrimitive(name, type, arraySize)
        elements.add(item)
    }

    fun struct(structName: String, name: String, arraySize: Int = 1, builder: ShaderStorageFormat.() -> Unit) {
        val structElements = ShaderStorageFormat().let {
            it.builder()
            it.elements
        }.filterIsInstance<ShaderStoragePrimitive>()

        val struct = ShaderStorageStruct(structName, name, structElements, arraySize)
        elements.add(struct)
    }

    override fun toString(): String {
        return "ShaderStorageFormat{" +
                "items=" + elements +
                ", formatSize=" + formatSize +
                '}'
    }

    fun hasMember(name: String): Boolean = elements.any { it.name == name }

    override fun hashCode(): Int {
        return elements.hashCode()
    }

    fun commit() {
        val memberCount = elements.sumOf { if (it is ShaderStorageStruct) it.elements.size else 1 }
        val paddings = IntArray(memberCount)
        var largestAlign = 0
        var ints = 0

        var paddingIdx = -1
        /* Compute necessary padding after each field */
        for (idx in elements.indices) {

            when (val element = elements[idx]) {
                is ShaderStoragePrimitive -> {
                    val len = element.arraySize
                    val align = element.type.alignmentInBytes

                    largestAlign = max(largestAlign, align)

                    if (idx >= 1) {
                        val neededPadding = (align - ints % align) % align
                        paddings[paddingIdx] = neededPadding
                        ints += neededPadding
                    }

                    ints += element.type.sizeInBytes * len
                    paddingIdx++
                }

                is ShaderStorageStruct -> {
                    for (sIdx in element.elements.indices) {
                        val structMember = element.elements[sIdx] as ShaderStoragePrimitive
                        val len = structMember.arraySize
                        val align = structMember.type.alignmentInBytes

                        largestAlign = max(largestAlign, align)

                        if (idx + sIdx >= 1) {
                            val neededPadding = (align - ints % align) % align
                            paddings[paddingIdx] = neededPadding
                            ints += neededPadding
                        }

                        ints += structMember.type.sizeInBytes * len
                        paddingIdx++
                    }
                }
            }
        }

        /* Compute padding at the end of the struct */
        val endPadding = (largestAlign - ints % largestAlign) % largestAlign
        paddings[memberCount - 1] = endPadding

        paddingIdx = 0

        for (memberIdx in elements.indices) {

            when (val element = elements[memberIdx]) {
                is ShaderStoragePrimitive -> {
                    val padding = paddings[paddingIdx]

                    element.offset = element.arraySize * element.type.sizeInBytes
                    element.padding = padding

                    formatSize += element.offset + padding
                    paddingIdx++
                }

                is ShaderStorageStruct -> {
                    var totalSize = 0

                    for (sIdx in element.elements.indices) {
                        val structMember = element.elements[sIdx] as ShaderStoragePrimitive
                        val padding = paddings[paddingIdx]

                        structMember.offset = structMember.arraySize * structMember.type.sizeInBytes
                        structMember.padding = padding

                        totalSize += structMember.offset + padding
                        paddingIdx++
                    }

                    formatSize += totalSize * element.arraySize
                }
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ShaderStorageFormat

        return elements == other.elements
    }
}

/**
 * Constructs a new [ShaderStorageFormat] instance and allows the caller to configure it using a lambda.
 *
 * @param builder A lambda function used to configure the [ShaderStorageFormat] instance.
 * @return A fully constructed and committed [ShaderStorageFormat] instance.
 */
fun shaderStorageFormat(builder: ShaderStorageFormat.() -> Unit): ShaderStorageFormat {
    return ShaderStorageFormat().apply {
        builder()
        commit()
    }
}

fun shaderStorageFormatToVertexFormat(format: ShaderStorageFormat, elementName: String?): VertexFormat {

    val elementIndex = if (elementName == null) 0 else {
        format.elements.indexOfFirst { it.name == elementName }
    }

    require(elementIndex != -1) {
        "no such element: $elementName"
    }
    return vertexFormat {
        if (format.elements[elementIndex] is ShaderStorageStruct) {
            val outerStruct = format.elements.first() as ShaderStorageStruct
            for (member in outerStruct.elements) {
                if (member is ShaderStoragePrimitive) {
                    val vet = vertexElementType(member)
                    val padding = member.padding
                    attribute(member.name, vet)
                    if (padding > 0) {
                        padding(padding)
                    }
                }
            }
        } else {
            val member = format.elements[elementIndex] as ShaderStoragePrimitive
            val vet = vertexElementType(member)
            attribute(member.name, vet)
        }
    }
}

private fun vertexElementType(member: ShaderStoragePrimitive) = when (member.type) {
    BufferPrimitiveType.VECTOR4_FLOAT32 -> VertexElementType.VECTOR4_FLOAT32
    BufferPrimitiveType.VECTOR3_FLOAT32 -> VertexElementType.VECTOR3_FLOAT32
    BufferPrimitiveType.VECTOR2_FLOAT32 -> VertexElementType.VECTOR2_FLOAT32
    BufferPrimitiveType.MATRIX22_FLOAT32 -> VertexElementType.MATRIX33_FLOAT32
    BufferPrimitiveType.MATRIX33_FLOAT32 -> VertexElementType.MATRIX33_FLOAT32
    BufferPrimitiveType.MATRIX44_FLOAT32 -> VertexElementType.MATRIX44_FLOAT32
    BufferPrimitiveType.INT32 -> VertexElementType.INT32
    BufferPrimitiveType.UINT32 -> VertexElementType.UINT32
    BufferPrimitiveType.FLOAT32 -> VertexElementType.FLOAT32
    else -> error("unsupported type '${member.type}")
}

/**
 * Creates a shader storage buffer based on the given format.
 *
 * @param format The [ShaderStorageFormat] specifying the structure and layout of the shader storage buffer.
 * @return A [ShaderStorageBuffer] instance configured according to the provided format.
 */
fun shaderStorageBuffer(format: ShaderStorageFormat): ShaderStorageBuffer {
    return Driver.instance.createShaderStorageBuffer(format)
}

