package org.openrndr.ktessellation

import org.openrndr.math.Vector2

/**
 * Represents a geometric primitive that can be drawn in a graphics context.
 *
 * @property type The OpenGL constant that specifies the type of this primitive (e.g., points, lines, or triangles).
 * @property positions A mutable list of 2D vectors representing the vertex positions required to render this primitive.
 */
class Primitive(
    /**
     * The OpenGL constant defining the type of this primitive
     *
     */
    val type: Int
) {
    /**
     * A list of the indices of the vertices required to draw this primitive.
     *
     */
    val positions: MutableList<Vector2> = mutableListOf()
}

/**
 * Represents a primitive in OpenGL with an associated list of vertex indices.
 *
 * @property type The OpenGL constant that defines the type of this primitive.
 */
class IndexedPrimitive(
    /**
     * The OpenGL constant defining the type of this primitive
     *
     */
    val type: Int
) {
    /**
     * A list of the indices of the vertices required to draw this primitive.
     *
     */
    val indices: MutableList<Int> =  mutableListOf()
}