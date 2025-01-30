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
                    val result = DoubleArray(3)
                    result[0] = coords[0]
                    result[1] = coords[1]
                    result[2] = coords[2]
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
 * A class that extends the functionality of `GLUtessellatorImpl` to construct
 * tessellated geometric primitives with indexed vertex data.
 *
 * The `IndexedTessellator` collects tessellated primitives in the form of
 * `IndexedPrimitive` instances, where each instance represents a tessellated
 * piece of geometry (such as triangles or polygons) along with the indices of
 * the vertices used to define the geometry.
 *
 * This class utilizes a customized `GLUtessellatorCallback` implementation
 * to handle tessellation events. The supported callback methods include:
 * - `begin`: Called at the start of a new geometric primitive definition.
 * - `vertexData`: Processes vertex data during tessellation and stores
 *   indices into the current geometric primitive.
 * - `error`: Raises an exception on tessellation errors with a human-readable
 *   GLU error message.
 * - `combine`: Handles vertex combination in the tessellation process.
 *
 * The tessellation callbacks are registered internally during the initialization
 * of this class to handle specific stages of the tessellation process.
 */
class IndexedTessellator : GLUtessellatorImpl() {
    var primitives = mutableListOf<IndexedPrimitive>()

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
                    val result = DoubleArray(3)
                    result[0] = coords[0]
                    result[1] = coords[1]
                    result[2] = coords[2]
                    data!!

                    outData[0] = data[0]
                }
            }
        gluTessCallback(GLU.GLU_TESS_BEGIN, callback)
        gluTessCallback(GLU.GLU_TESS_VERTEX_DATA, callback)
        gluTessCallback(GLU.GLU_TESS_ERROR, callback)
        gluTessCallback(GLU.GLU_TESS_COMBINE, callback)
    }
}

