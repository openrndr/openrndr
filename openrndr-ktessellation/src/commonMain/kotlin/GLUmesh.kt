package org.openrndr.ktessellation


/**
 * Represents the core structural mesh used in the tessellation process.
 *
 * The `GLUmesh` class serves as the main container for the tessellation mesh, managing
 * the vertex, face, and edge data structures used throughout the tessellation algorithm.
 * It provides dummy headers for linked lists of vertices, faces, and edges, enabling
 * efficient traversal and manipulation of the tessellation structure while maintaining
 * a consistent and connected representation.
 *
 * Properties:
 * - `vHead`: A dummy header for the list of vertices in the mesh. Connections to other
 *    vertices are built off this header during the tessellation process.
 * - `fHead`: A dummy header for the list of faces in the mesh. Faces are created and
 *    linked starting from this header.
 * - `eHead`: A dummy header for the list of half-edges. This serves as the primary half-edge
 *    in its symmetric edge pair.
 * - `eHeadSym`: The symmetric counterpart of `eHead`, representing the same edge but in
 *    the opposite direction.
 */
internal class GLUmesh {
    var vHead: GLUvertex =
        GLUvertex() /* dummy header for vertex list */
    var fHead: GLUface =
        GLUface() /* dummy header for face list */
    var eHead: GLUhalfEdge =
        GLUhalfEdge(true) /* dummy header for edge list */
    var eHeadSym: GLUhalfEdge =
        GLUhalfEdge(false) /* and its symmetric counterpart */
}
