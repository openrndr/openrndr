@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package org.openrndr.ktessellation

/*
 * Copyright (c) 2002-2008 LWJGL Project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'LWJGL' nor the names of
 *   its contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/**
 * GLU.java
 *
 *
 * Created 23-dec-2003
 * @author Erik Duijs
 */
object GLU {
    const val PI: Float = kotlin.math.PI.toFloat()

    /* Errors: (return value 0 = no error) */
    const val GLU_INVALID_ENUM = 100900
    const val GLU_INVALID_VALUE = 100901
    const val GLU_OUT_OF_MEMORY = 100902

    /****           Tesselation constants            */
    const val GLU_TESS_MAX_COORD = 1.0e150
    const val TESS_MAX_COORD = 1.0e150

    /* TessProperty */
    const val GLU_TESS_WINDING_RULE = 100140
    const val GLU_TESS_BOUNDARY_ONLY = 100141
    const val GLU_TESS_TOLERANCE = 100142

    /* TessWinding */
    const val GLU_TESS_WINDING_ODD = 100130
    const val GLU_TESS_WINDING_NONZERO = 100131
    const val GLU_TESS_WINDING_POSITIVE = 100132
    const val GLU_TESS_WINDING_NEGATIVE = 100133
    const val GLU_TESS_WINDING_ABS_GEQ_TWO = 100134

    /* TessCallback */
    const val GLU_TESS_BEGIN = 100100 /* void (CALLBACK*)(GLenum    type)  */
    const val GLU_TESS_VERTEX = 100101 /* void (CALLBACK*)(void      *data) */
    const val GLU_TESS_END = 100102 /* void (CALLBACK*)(void)            */
    const val GLU_TESS_ERROR = 100103 /* void (CALLBACK*)(GLenum    errno) */
    const val GLU_TESS_EDGE_FLAG = 100104 /* void (CALLBACK*)(GLboolean boundaryEdge)  */
    const val GLU_TESS_COMBINE = 100105 /* void (CALLBACK*)(GLdouble  coords[3],
	                                                            void      *data[4],
	                                                            GLfloat   weight[4],
	                                                            void      **dataOut)     */
    const val GLU_TESS_BEGIN_DATA = 100106 /* void (CALLBACK*)(GLenum    type,
	                                                            void      *polygon_data) */
    const val GLU_TESS_VERTEX_DATA = 100107 /* void (CALLBACK*)(void      *data,
	                                                            void      *polygon_data) */
    const val GLU_TESS_END_DATA = 100108 /* void (CALLBACK*)(void      *polygon_data) */
    const val GLU_TESS_ERROR_DATA = 100109 /* void (CALLBACK*)(GLenum    errno,
	                                                            void      *polygon_data) */
    const val GLU_TESS_EDGE_FLAG_DATA = 100110 /* void (CALLBACK*)(GLboolean boundaryEdge,
	                                                            void      *polygon_data) */
    const val GLU_TESS_COMBINE_DATA = 100111 /* void (CALLBACK*)(GLdouble  coords[3],
	                                                            void      *data[4],
	                                                            GLfloat   weight[4],
	                                                            void      **dataOut,
	                                                            void      *polygon_data) */

    /* TessError */
    const val GLU_TESS_ERROR1 = 100151
    const val GLU_TESS_ERROR2 = 100152
    const val GLU_TESS_ERROR3 = 100153
    const val GLU_TESS_ERROR4 = 100154
    const val GLU_TESS_ERROR5 = 100155
    const val GLU_TESS_ERROR6 = 100156
    const val GLU_TESS_ERROR7 = 100157
    const val GLU_TESS_ERROR8 = 100158
    const val GLU_TESS_MISSING_BEGIN_POLYGON: Int = GLU_TESS_ERROR1
    const val GLU_TESS_MISSING_BEGIN_CONTOUR: Int = GLU_TESS_ERROR2
    const val GLU_TESS_MISSING_END_POLYGON: Int = GLU_TESS_ERROR3
    const val GLU_TESS_MISSING_END_CONTOUR: Int = GLU_TESS_ERROR4
    const val GLU_TESS_COORD_TOO_LARGE: Int = GLU_TESS_ERROR5
    const val GLU_TESS_NEED_COMBINE_CALLBACK: Int = GLU_TESS_ERROR6
    fun gluErrorString(error_code: Int): String {
        return when (error_code) {
            GLU_INVALID_ENUM -> "Invalid enum (glu)"
            GLU_INVALID_VALUE -> "Invalid value (glu)"
            GLU_OUT_OF_MEMORY -> "Out of memory (glu)"
            GLU_TESS_MISSING_BEGIN_POLYGON -> "missing begin polygon"
            GLU_TESS_MISSING_BEGIN_CONTOUR -> "missing begin contour"
            GLU_TESS_MISSING_END_POLYGON -> "missing end polygon"
            GLU_TESS_MISSING_END_CONTOUR -> "missing end contour"
            GLU_TESS_COORD_TOO_LARGE -> "tess coord too large"
            GLU_TESS_NEED_COMBINE_CALLBACK -> "tess need combine callback"
            else -> "E_NO_CLUE"
        }
    }

    fun gluNewTess(): GLUtessellator {
        return GLUtessellatorImpl()
    }
}