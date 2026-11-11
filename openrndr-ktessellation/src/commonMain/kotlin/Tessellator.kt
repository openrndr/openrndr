package org.openrndr.ktessellation

import org.openrndr.ktessellation.GLU.gluErrorString
import org.openrndr.math.Vector2

/**
 * A class responsible for handling tessellation functionality. Tessellation is the process of dividing a
 * polygonal shape into smaller components such as triangles or other primitives for rendering purposes.
 *
 * Tessellator extends `GLUtessellatorImpl` and provides a concrete implementation of tessellation
 * callbacks for processing vertices, handling errors, and combining coordinates during the tessellation process.
 *
 * The primary output of the tessellation process is a list of primitives that contain the tessellated
 * shapes and their respective vertex positions.
 *
 * The internal callbacks handle the following:
 * - Beginning the creation of a new primitive by detecting the type of primitive to be generated.
 * - Storing vertex positions associated with the current tessellation primitive.
 * - Handling errors that may arise during tessellation.
 * - Combining vertices when new intersection points are generated during tessellation.
 */
class Tessellator : GLUtessellatorImpl() {
    var primitives = mutableListOf<Primitive>()

    init {
        val callback: GLUtessellatorCallbackAdapter =
            object : GLUtessellatorCallbackAdapter() {
                override fun begin(type: Int) {
                    primitives.add(Primitive(type))
                }

                override fun vertexData(vertexData: Any?, polygonData: Any?) {
                    val data = vertexData as DoubleArray
                    primitives[primitives.size - 1].positions.add(Vector2(data[0], data[1]))
                }

                override fun error(errnum: Int) {
                    throw RuntimeException("GLU Error " + gluErrorString(errnum))
                }

                override fun combine(
                    coords: DoubleArray?,
                    data: Array<Any?>?,
                    weight: FloatArray?,
                    outData: Array<Any?>?
                ) {

                    coords ?: error("coords == null")
                    outData ?: error("outData == null")
                    // the tessellator passes a copy of the coordinates of the new vertex
                    outData[0] = coords
                }
            }
        gluTessCallback(GLU.GLU_TESS_BEGIN, callback)
        gluTessCallback(GLU.GLU_TESS_VERTEX_DATA, callback)
        gluTessCallback(GLU.GLU_TESS_ERROR, callback)
        gluTessCallback(GLU.GLU_TESS_COMBINE, callback)
    }
}


/**
 * A tessellator that outputs triangles as indices of vertices, the vertex data of each vertex passed to
 * `gluTessVertex` is its index.
 *
 * Coincident vertices are merged into the vertex with the first index. Intersections of edges need new vertices:
 * they are added to [newVertices], their index in the primitives is the negative number `-1 - i` for the `i`-th new
 * vertex, which the caller maps to an index after its own vertices.
 */
class IndexedTessellator : GLUtessellatorImpl() {
    var primitives = mutableListOf<IndexedPrimitive>()

    /** the coordinates (x, y, z) of the vertices at intersections of edges */
    val newVertices = mutableListOf<DoubleArray>()

    init {
        val callback: GLUtessellatorCallbackAdapter =
            object : GLUtessellatorCallbackAdapter() {
                override fun begin(type: Int) {
                    primitives.add(IndexedPrimitive(type))
                }

                override fun vertexData(vertexData: Any?, polygonData: Any?) {
                    val data = vertexData as Int
                    primitives[primitives.size - 1].indices.add(data)
                }

                override fun error(errnum: Int) {
                    throw RuntimeException("GLU Error " + gluErrorString(errnum))
                }

                override fun combine(
                    coords: DoubleArray?,
                    data: Array<Any?>?,
                    weight: FloatArray?,
                    outData: Array<Any?>?
                ) {
                    coords ?: error("coords == null")
                    outData ?: error("outData == null")
                    data ?: error("data == null")
                    if (data[2] == null) {
                        // two coincident vertices are merged, the first one is kept
                        outData[0] = data[0]
                    } else {
                        // the intersection of two edges, the tessellator passes a copy of its coordinates
                        newVertices.add(coords)
                        outData[0] = -newVertices.size
                    }
                }
            }
        gluTessCallback(GLU.GLU_TESS_BEGIN, callback)
        gluTessCallback(GLU.GLU_TESS_VERTEX_DATA, callback)
        gluTessCallback(GLU.GLU_TESS_ERROR, callback)
        gluTessCallback(GLU.GLU_TESS_COMBINE, callback)
    }
}
