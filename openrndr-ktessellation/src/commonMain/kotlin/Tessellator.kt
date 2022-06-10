package org.openrndr.ktessellation

import org.openrndr.ktessellation.GLU.gluErrorString
import org.openrndr.math.Vector2

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