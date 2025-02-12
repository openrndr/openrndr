package org.openrndr.draw

import org.openrndr.internal.Driver
import kotlin.math.max

/**
 * Represents a shader storage buffer, which is an interface for managing
 * GPU memory used for reading and writing data within shaders.
 */
expect interface ShaderStorageBuffer : AutoCloseable {
    val session: Session?
    val format: ShaderStorageFormat

    /**
     * Provides a shadow interface for managing the associated shader storage buffer.
     * The shadow allows operations such as uploading, downloading, and manipulating buffer data.
     * It acts as a utility for interacting with the actual GPU storage buffer represented
     * by the `ShaderStorageBuffer` instance.
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
    fun put(elementOffset: Int = 0, putter: BufferWriter.() -> Unit): Int
}

/**
 * Represents an element in shader storage, typically used in GPU programming
 * for storing data buffers. This interface defines the core properties
 * of such an element, including its name, memory offset, and size when
 * represented as an array.
 */
sealed interface ShaderStorageElement {
    val name: String
    val arraySize: Int
    fun alignmentInBytes(): Int
}

/**
 * Represents different types of data primitives that can be stored in a buffer.
 * Each type is associated with the number of components it contains, its size in bytes,
 * and its alignment requirement in bytes.
 *
 * @property componentCount Number of components in the primitive type.
 * @property sizeInBytes Size of the primitive type in bytes.
 * @property alignmentInBytes Alignment requirement of the primitive type in bytes.
 */
enum class BufferPrimitiveType(val componentCount: Int, val sizeInBytes: Int, val alignmentInBytes: Int) {
    UINT32(1, 4, 4),
    INT32(1, 4, 4),
    BOOLEAN(1, 4, 4),
    FLOAT32(1, 4, 4),

    VECTOR2_BOOLEAN(2, 8, 8),
    VECTOR2_INT32(2, 8, 8),
    VECTOR2_UINT32(2, 8, 8),
    VECTOR2_FLOAT32(2, 8, 8),

    VECTOR3_BOOLEAN(3, 12, 16),
    VECTOR3_INT32(3, 12, 16),
    VECTOR3_UINT32(3, 12, 16),
    VECTOR3_FLOAT32(3, 12, 16),

    VECTOR4_BOOLEAN(4, 16, 16),
    VECTOR4_INT32(4, 16, 16),
    VECTOR4_UINT32(4, 16, 16),
    VECTOR4_FLOAT32(4, 16, 16),

    MATRIX22_FLOAT32(4, 4 * 4, 8),
    MATRIX33_FLOAT32(9, 9 * 4, 16),
    MATRIX44_FLOAT32(16, 16 * 4, 16),
}

/**
 * Represents a primitive element within a shader storage buffer.
 * This class enables detailed definition of a primitive type and its properties
 * such as the buffer primitive type, array size and memory offset.
 **
 * @property name The name of the shader storage primitive.
 * @property type The type of the primitive, represented by the `BufferPrimitiveType` enumeration.
 * @property arraySize The number of elements in the array, default value is 1.
 * @property offset The memory offset of this primitive in the buffer.
 */
data class ShaderStoragePrimitive(
    override val name: String,
    val type: BufferPrimitiveType,
    override val arraySize: Int = 1,
) : ShaderStorageElement {
    override fun alignmentInBytes(): Int {
        return type.alignmentInBytes
    }
}


/**
 * Represents a structure within shader storage, typically used for
 * organizing and grouping related shader storage elements.
 *
 * @property structName The name of the structure as defined in the shader.
 * @property name The name identifier for this specific instance of the structure.
 * @property elements A list of elements contained within the structure.
 * @property arraySize The size of the structure when represented as an array. Defaults to 1.
 */
data class ShaderStorageStruct(
    val structName: String,
    override val name: String,
    val elements: List<ShaderStorageElement>,
    override val arraySize: Int = 1,
) : ShaderStorageElement {
    override fun alignmentInBytes(): Int {
        return elements.maxOf { it.alignmentInBytes() }
    }
}

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
     * Adds a primitive element to the shader storage format.
     *
     * @param name The name of the primitive element to be added.
     * @param type The type of the primitive, represented by the `BufferPrimitiveType` enumeration.
     * @param arraySize The number of elements in the array for this primitive. Defaults to 1.
     */
    fun primitive(name: String, type: BufferPrimitiveType, arraySize: Int = 1) {
        val item = ShaderStoragePrimitive(name, type, arraySize)
        elements.add(item)
    }

    /**
     * Adds a struct definition to the shader storage format.
     *
     * @param structName The name of the struct type as defined in the shader.
     * @param name The name identifier for the struct instance.
     * @param arraySize The number of elements in the array for this struct. Defaults to 1.
     * @param builder A lambda used to define the individual elements of the struct.
     */
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

    private suspend fun SequenceScope<ShaderStorageElement>.processElements(
        elements: List<ShaderStorageElement>,
    ) {
        elements.forEach { e ->
            when (e) {
                is ShaderStoragePrimitive -> {
                    for (i in 0 until e.arraySize) {
                        yield(e)
                    }
                }
                is ShaderStorageStruct -> {
                    for (i in 0 until e.arraySize) {
                        yield(e)
                        processElements(e.elements)
                    }
                }
            }
        }
    }

    /**
     * Generates a sequence of `ShaderStorageElement` objects by processing the elements
     * defined in the `ShaderStorageFormat`. This function delegates the processing of
     * individual elements to the `processElements` method.
     *
     * The resulting sequence yields each element according to its definition and
     * structure, including handling arrays and nested structures.
     *
     * @return A sequence of `ShaderStorageElement` objects.
     */
    fun elementSequence() = sequence {
        processElements(elements)
    }

    /**
     * Processes and updates the internal format size based on the alignment and size
     * requirements of the elements within the shader storage format. The method ensures
     * that the memory layout complies with the alignment constraints of each element,
     * including primitives and structures, while accounting for array sizes and nested
     * structures.
     *
     * This function works recursively for nested structures, adjusting the format size
     * at each level and ensuring proper alignment. It operates on the `elements` property
     * of the enclosing class.
     */
    fun commit() {
        fun updateElements(elements: List<ShaderStorageElement>) {
            for (element in elements) {
                when (element) {
                    is ShaderStoragePrimitive -> {
                        if (formatSize.mod(element.alignmentInBytes()) != 0) {
                            formatSize += (element.alignmentInBytes() - formatSize.mod(element.alignmentInBytes()))
                        }
                        formatSize += element.type.sizeInBytes * max(element.arraySize, 1)
                    }

                    is ShaderStorageStruct -> {
                        if (formatSize.mod(element.alignmentInBytes()) != 0) {
                            formatSize += (element.alignmentInBytes() - formatSize.mod(element.alignmentInBytes()))
                        }
                        val start = formatSize
                        updateElements(element.elements)

                        if (element.arraySize > 1) {
                            if (formatSize.mod(element.alignmentInBytes()) != 0) {
                                formatSize += (element.alignmentInBytes() - formatSize.mod(element.alignmentInBytes()))
                            }
                            val end = formatSize
                            val structSize = end - start
                            formatSize += structSize * (element.arraySize - 1)
                        }
                    }
                }
            }
        }
        updateElements(elements)
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

/**
 * Creates a shader storage buffer based on the given format.
 *
 * @param format The [ShaderStorageFormat] specifying the structure and layout of the shader storage buffer.
 * @return A [ShaderStorageBuffer] instance configured according to the provided format.
 */
fun shaderStorageBuffer(format: ShaderStorageFormat): ShaderStorageBuffer {
    return Driver.instance.createShaderStorageBuffer(format)
}

