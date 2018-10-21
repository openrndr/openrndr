package org.openrndr.draw

/**
 * VertexBuffer Layout describes how data is organized in the VertexBuffer
 */
class VertexFormat {

    internal constructor()

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

    private fun floatTypeFromDimensions(dimensions: Int): VertexElementType {
        return when (dimensions) {
            1 -> VertexElementType.FLOAT32
            2 -> VertexElementType.VECTOR2_FLOAT32
            3 -> VertexElementType.VECTOR3_FLOAT32
            4 -> VertexElementType.VECTOR4_FLOAT32
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

    fun textureCoordinate(dimensions: Int = 2, index: Int = 0) = attribute("texCoord$index", floatTypeFromDimensions(dimensions))


    /**
     * Adds a custom attribute to the [VertexFormat]
     */
    fun attribute(name: String, type: VertexElementType, arraySize: Int = 1) {
        val offset = items.sumBy { it.arraySize * it.type.sizeInBytes }
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
}

fun vertexFormat(builder: VertexFormat.() -> Unit): VertexFormat {
    return VertexFormat().apply { builder() }
}
