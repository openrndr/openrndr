package org.openrndr.draw

import org.openrndr.draw.VertexElementType.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Represents a single element within a vertex, describing the structure of vertex data in a graphics pipeline.
 *
 * @property attribute The name of the attribute this element corresponds to (e.g., position, color, normal).
 * @property offset The byte offset of this element within a vertex structure.
 * @property type The data type of this vertex element, represented by [VertexElementType].
 * @property arraySize The number of elements in this vertex attribute array; typically 1 for non-array attributes.
 */
data class VertexElement(val attribute: String, val offset: Int, val type: VertexElementType, val arraySize: Int)

/**
 * VertexBuffer Layout describes how data is organized in the VertexBuffer
 */
class VertexFormat {

    var items: MutableList<VertexElement> = mutableListOf()
    private var vertexSize = 0

    /**
     * The size of the [VertexFormat] in bytes
     */
    val size get() = vertexSize

    /**
     * Appends a position component to the layout
     * @param dimensions
     */
    fun position(dimensions: Int) = attribute("position", floatTypeFromDimensions(dimensions))

    /**
     * Insert padding in the layout
     * @param paddingInBytes the amount of padding in bytes
     */
    fun padding(paddingInBytes: Int) = attribute("_", VertexElementType.UINT8, paddingInBytes)


    fun paddingFloat(sizeInFloats: Int) = attribute("_", VertexElementType.FLOAT32, sizeInFloats)


    private fun floatTypeFromDimensions(dimensions: Int): VertexElementType {
        return when (dimensions) {
            1 -> VertexElementType.FLOAT32
            2 -> VertexElementType.VECTOR2_FLOAT32
            3 -> VertexElementType.VECTOR3_FLOAT32
            4 -> VECTOR4_FLOAT32
            else -> throw IllegalArgumentException("dimensions can only be 1, 2, 3 or 4 (got $dimensions)")
        }
    }

    /**
     * Appends a normal component to the layout
     * @param dimensions the number of dimensions of the normal vector
     */
    fun normal(dimensions: Int) = attribute("normal", floatTypeFromDimensions(dimensions))

    /**
     * Appends a color attribute to the layout
     * @param dimensions
     */
    fun color(dimensions: Int) = attribute("color", floatTypeFromDimensions(dimensions))

    fun textureCoordinate(dimensions: Int = 2, index: Int = 0) =
        attribute("texCoord$index", floatTypeFromDimensions(dimensions))


    /**
     * Adds a custom attribute to the [VertexFormat]
     */
    fun attribute(name: String, type: VertexElementType, arraySize: Int = 1) {
        val offset = items.sumOf { it.arraySize * it.type.sizeInBytes }
        val item = VertexElement(name, offset, type, arraySize)
        items.add(item)
        vertexSize += type.sizeInBytes * arraySize
    }

    override fun toString(): String {
        return "VertexFormat{" +
                "items=" + items +
                ", vertexSize=" + vertexSize +
                '}'
    }

    fun hasAttribute(name: String): Boolean = items.any { it.attribute == name }
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VertexFormat) return false


        return items == other.items
    }

    override fun hashCode(): Int {
        return items.hashCode()
    }

    /**
     * Evaluates if the vertex format adheres to the `std430` memory layout rules.
     *
     * The `std430` layout requires proper alignment of each data type based on its size and characteristics.
     * It ensures that:
     * - The alignment of each item within the layout matches the specified alignment for its type.
     * - The vertex size respects the maximum alignment of all items.
     *
     * The determination involves validating the alignment of offsets for each item in `items` based on their type
     * and verifying that `size` is aligned with the computed `maxAlign` value.
     *
     * The alignment rules are derived as follows:
     * - Matrices (`MATRIX33_FLOAT32`, `MATRIX44_FLOAT32`, `MATRIX22_FLOAT32`) and 4-component vector types
     *   (`VECTOR4_UINT32`, `VECTOR4_FLOAT32`, `VECTOR4_INT32`) have an alignment of 16 bytes.
     * - 3-component vectors (`VECTOR3_UINT32`, `VECTOR3_FLOAT32`, `VECTOR3_INT32`) have an alignment of 16 bytes.
     * - 2-component vectors (`VECTOR2_UINT32`, `VECTOR2_FLOAT32`, `VECTOR2_INT32`) have an alignment of 8 bytes.
     * - Scalars (`FLOAT32`, `UINT32`, `INT32`) have an alignment of 4 bytes.
     *
     * If any item or the overall vertex size violates the alignment rules, the result will be `false`.
     *
     * @return `true` if the vertex format satisfies the alignment constraints of the `std430` layout, otherwise `false`.
     */
    val isInStd430Layout: Boolean
        get() {
            var maxAlign = 4
            vertexSize
            for (item in items) {
                val alignSize = when (item.type) {
                    MATRIX33_FLOAT32,
                    MATRIX44_FLOAT32,
                    MATRIX22_FLOAT32,
                    VECTOR4_UINT32, VECTOR4_FLOAT32, VECTOR4_INT32,
                    VECTOR3_UINT32, VECTOR3_FLOAT32, VECTOR3_INT32 -> 16
                    VECTOR2_UINT32, VECTOR2_FLOAT32, VECTOR2_INT32 -> 8
                    FLOAT32, UINT32, INT32 -> 4
                    else -> error("unsupported item type ${item.type}")
                }
                maxAlign = alignSize
                val aligned = item.offset.mod(alignSize) == 0
                if (!aligned) {
                    return false
                }
            }

            return size.mod(maxAlign) == 0
        }

}

/**
 * Build a vertex format
 */
@OptIn(ExperimentalContracts::class)
fun vertexFormat(builder: VertexFormat.() -> Unit): VertexFormat {
    contract {
        callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
    }

    return VertexFormat().apply { builder() }
}
