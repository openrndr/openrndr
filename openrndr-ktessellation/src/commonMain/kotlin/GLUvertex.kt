@file:Suppress("SpellCheckingInspection")

package org.openrndr.ktessellation

/**
 * Represents a vertex in the tessellation process.
 *
 * This class is used internally within the tessellation algorithm to manage vertex data,
 * including connections to edges and other vertices, as well as associated computational
 * properties.
 *
 * Properties:
 * - `next`: References the next vertex in a linked structure, never null.
 * - `prev`: References the previous vertex in a linked structure, never null.
 * - `anEdge`: References a half-edge originating from this vertex.
 * - `data`: Stores client-specific data associated with the vertex.
 * - `coords`: Stores the 3D coordinates of the vertex.
 * - `s`: Represents a parameter value used in computational projections.
 * - `t`: Represents the result of projecting the vertex onto the sweep plane.
 * - `pqHandle`: Used to manage this vertex in a priority queue.
 */
internal class GLUvertex {
    var next /* next vertex (never NULL) */: GLUvertex? = null
    var prev /* previous vertex (never NULL) */: GLUvertex? = null
    var anEdge /* a half-edge with this origin */: GLUhalfEdge? = null
    var data /* client's data */: Any? = null

    /* Internal data (keep hidden) */
    var coords = DoubleArray(3) /* vertex location in 3D */
    var s = 0.0
    var t /* projection onto the sweep plane */ = 0.0
    var pqHandle /* to allow deletion from priority queue */ = 0
}
