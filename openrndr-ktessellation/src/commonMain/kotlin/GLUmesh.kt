package org.openrndr.ktessellation


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
