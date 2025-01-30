@file:Suppress("UNREACHABLE_CODE", "SENSELESS_COMPARISON", "unused")

package org.openrndr.ktessellation

@Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
open class GLUtessellatorImpl : GLUtessellator {
    private var state /* what begin/end calls have we seen? */: Int
    private var lastEdge /* lastEdge->Org is the most recent vertex */: GLUhalfEdge? =
        null
    internal var mesh: GLUmesh? = null
    /* stores the input contours, and eventually
                                   the tessellation itself */

    /*** state needed for projecting onto the sweep plane  */
    var normal = DoubleArray(3) /* user-specified normal (if provided) */
    var sUnit = DoubleArray(3) /* unit vector in s-direction (debugging) */
    var tUnit = DoubleArray(3) /* unit vector in t-direction (debugging) */

    /*** state needed for the line sweep  */
    private var relTolerance /* tolerance for merging features */: Double
    var windingRule /* rule for determining polygon interior */: Int
    var fatalError /* fatal error: needed combine callback */ = false
    internal var dict /* edge dictionary for sweep line */: Dict? = null
    internal var pq /* priority queue of vertex events */: PriorityQ? = null
    internal var event /* current sweep event being processed */: GLUvertex? = null

    /*** state needed for rendering callbacks (see render.c)  */
    var flagBoundary /* mark boundary edges (use EdgeFlag) */: Boolean
    var boundaryOnly /* Extract contours, not triangles */: Boolean
    internal var lonelyTriList: GLUface? = null
    /* list of triangles which could not be rendered as strips or fans */
    /*** state needed to cache single-contour polygons for renderCache()  */
    private var flushCacheOnNextVertex /* empty cache on next vertex() call */ = false
    var cacheCount /* number of cached vertices */ = 0
    internal var cache: Array<CachedVertex?> =
        arrayOfNulls(
            TESS_MAX_CACHE
        ) /* the vertex data */

    /*** rendering callbacks that also pass polygon data   */
    private var polygonData /* client data for current polygon */: Any?
    private var callBegin: GLUtessellatorCallback
    private var callEdgeFlag: GLUtessellatorCallback
    private var callVertex: GLUtessellatorCallback
    private var callEnd: GLUtessellatorCallback

    //    private GLUtessellatorCallback callMesh;
    private var callError: GLUtessellatorCallback
    private var callCombine: GLUtessellatorCallback
    private var callBeginData: GLUtessellatorCallback
    private var callEdgeFlagData: GLUtessellatorCallback
    private var callVertexData: GLUtessellatorCallback
    private var callEndData: GLUtessellatorCallback

    //    private GLUtessellatorCallback callMeshData;
    private var callErrorData: GLUtessellatorCallback
    private var callCombineData: GLUtessellatorCallback
    private fun makeDormant() {
        /* Return the tessellator to its original dormant state. */
        if (mesh != null) {
            Mesh.__gl_meshDeleteMesh(mesh!!)
        }
        state = TessState.T_DORMANT
        lastEdge = null
        mesh = null
    }

    private fun requireState(newState: Int) {
        check(state == newState) { "state should be $newState is $state" }
        //if (state != newState) gotoState(newState);
    }

    private fun gotoState(newState: Int) {
        println("requesting new state! $newState current state is $state")
        while (state != newState) {
            /* We change the current state one level at a time, to get to
             * the desired state.
             */
            if (state < newState) {
                if (state == TessState.T_DORMANT) {
                    callErrorOrErrorData(GLU.GLU_TESS_MISSING_BEGIN_POLYGON)
                    gluTessBeginPolygon(null)
                } else if (state == TessState.T_IN_POLYGON) {
                    callErrorOrErrorData(GLU.GLU_TESS_MISSING_BEGIN_CONTOUR)
                    gluTessBeginContour()
                }
            } else {
                if (state == TessState.T_IN_CONTOUR) {
                    callErrorOrErrorData(GLU.GLU_TESS_MISSING_END_CONTOUR)
                    gluTessEndContour()
                } else if (state == TessState.T_IN_POLYGON) {
                    callErrorOrErrorData(GLU.GLU_TESS_MISSING_END_POLYGON)
                    /* gluTessEndPolygon( tess ) is too much work! */makeDormant()
                }
            }
        }
    }

    override fun gluDeleteTess() {
        requireState(TessState.T_DORMANT)
    }

    override fun gluTessProperty(which: Int, value: Double) {
        when (which) {
            GLU.GLU_TESS_TOLERANCE -> {
                if (value < 0.0 || value > 1.0) return
                relTolerance = value
                return
            }
            GLU.GLU_TESS_WINDING_RULE -> {
                val windingRule = value.toInt()
                if (windingRule.toDouble() != value) return /* not an integer */
                when (windingRule) {
                    GLU.GLU_TESS_WINDING_ODD, GLU.GLU_TESS_WINDING_NONZERO, GLU.GLU_TESS_WINDING_POSITIVE, GLU.GLU_TESS_WINDING_NEGATIVE, GLU.GLU_TESS_WINDING_ABS_GEQ_TWO -> {
                        this.windingRule = windingRule
                        return
                    }
                    else -> {}
                }
                boundaryOnly = value != 0.0
                return
            }
            GLU.GLU_TESS_BOUNDARY_ONLY -> {
                boundaryOnly = value != 0.0
                return
            }
            else -> {
                callErrorOrErrorData(GLU.GLU_INVALID_ENUM)
                return
            }
        }
        callErrorOrErrorData(GLU.GLU_INVALID_VALUE)
    }

    /* Returns tessellator property */
    override fun gluGetTessProperty(which: Int, value: DoubleArray, value_offset: Int) {
        when (which) {
            GLU.GLU_TESS_TOLERANCE -> {
                require(relTolerance in 0.0..1.0)
                value[value_offset] = relTolerance
            }
            GLU.GLU_TESS_WINDING_RULE -> {
                require(windingRule == GLU.GLU_TESS_WINDING_ODD || windingRule == GLU.GLU_TESS_WINDING_NONZERO || windingRule == GLU.GLU_TESS_WINDING_POSITIVE || windingRule == GLU.GLU_TESS_WINDING_NEGATIVE || windingRule == GLU.GLU_TESS_WINDING_ABS_GEQ_TWO)
                value[value_offset] = windingRule.toDouble()
            }
            GLU.GLU_TESS_BOUNDARY_ONLY -> {
                //require(boundaryOnly == true || boundaryOnly == false)
                value[value_offset] = if (boundaryOnly) 1.0 else 0.0
            }
            else -> {
                value[value_offset] = 0.0
                callErrorOrErrorData(GLU.GLU_INVALID_ENUM)
            }
        }
    } /* gluGetTessProperty() */

    override fun gluTessNormal(x: Double, y: Double, z: Double) {
        normal[0] = x
        normal[1] = y
        normal[2] = z
    }

    override fun gluTessCallback(which: Int, aCallback: GLUtessellatorCallback?) {
        when (which) {
            GLU.GLU_TESS_BEGIN -> {
                callBegin = aCallback ?: NULL_CB
                return
            }
            GLU.GLU_TESS_BEGIN_DATA -> {
                callBeginData = aCallback ?: NULL_CB
                return
            }
            GLU.GLU_TESS_EDGE_FLAG -> {
                callEdgeFlag = aCallback ?: NULL_CB
                /* If the client wants boundary edges to be flagged,
 * we render everything as separate triangles (no strips or fans).
 */flagBoundary = aCallback != null
                return
            }
            GLU.GLU_TESS_EDGE_FLAG_DATA -> {
                run {
                    callBegin = aCallback ?: NULL_CB
                    callEdgeFlagData = callBegin
                }
                /* If the client wants boundary edges to be flagged,
 * we render everything as separate triangles (no strips or fans).
 */flagBoundary = aCallback != null
                return
            }
            GLU.GLU_TESS_VERTEX -> {
                callVertex = aCallback ?: NULL_CB
                return
            }
            GLU.GLU_TESS_VERTEX_DATA -> {
                callVertexData = aCallback ?: NULL_CB
                return
            }
            GLU.GLU_TESS_END -> {
                callEnd = aCallback ?: NULL_CB
                return
            }
            GLU.GLU_TESS_END_DATA -> {
                callEndData = aCallback ?: NULL_CB
                return
            }
            GLU.GLU_TESS_ERROR -> {
                callError = aCallback ?: NULL_CB
                return
            }
            GLU.GLU_TESS_ERROR_DATA -> {
                callErrorData = aCallback ?: NULL_CB
                return
            }
            GLU.GLU_TESS_COMBINE -> {
                callCombine = aCallback ?: NULL_CB
                return
            }
            GLU.GLU_TESS_COMBINE_DATA -> {
                callCombineData = aCallback ?: NULL_CB
                return
            }
            else -> {
                callErrorOrErrorData(GLU.GLU_INVALID_ENUM)
                return
            }
        }
    }

    private fun addVertex(coords: DoubleArray, vertexData: Any?): Boolean {
        var e: GLUhalfEdge?
        e = lastEdge
        if (e == null) {
/* Make a self-loop (one vertex, one edge). */
            e = Mesh.__gl_meshMakeEdge(mesh ?: error("mesh == null"))
            if (e == null) return false
            if (!Mesh.__gl_meshSplice(e, e.Sym ?: error("e.Sym == null"))) return false
        } else {
/* Create a new vertex and edge which immediately follow e
 * in the ordering around the left face.
 */
            if (Mesh.__gl_meshSplitEdge(e) == null) return false
            e = e.Lnext
        }

/* The new vertex is now e.Org. */
        e!!.Org!!.data = vertexData
        e!!.Org!!.coords[0] = coords[0]
        e!!.Org!!.coords[1] = coords[1]
        e!!.Org!!.coords[2] = coords[2]

/* The winding of an edge says how the winding number changes as we
 * cross from the edge''s right face to its left face.  We add the
 * vertices in such an order that a CCW contour will add +1 to
 * the winding number of the region inside the contour.
 */e.winding = 1
        e.Sym!!.winding = -1
        lastEdge = e
        return true
    }

    private fun cacheVertex(coords: DoubleArray, vertexData: Any?) {
        if (cache[cacheCount] == null) {
            cache[cacheCount] = CachedVertex()
        }
        val v: CachedVertex = cache[cacheCount] ?: error("cache[cacheCount] == null")
        v.data = vertexData
        v.coords[0] = coords[0]
        v.coords[1] = coords[1]
        v.coords[2] = coords[2]
        ++cacheCount
    }

    private fun flushCache(): Boolean {
        val v: Array<CachedVertex?> = cache
        mesh = Mesh.__gl_meshNewMesh()
        if (mesh == null) return false
        for (i in 0 until cacheCount) {
            val vertex: CachedVertex = v[i] ?: error("v[i] == null")
            if (!addVertex(vertex.coords, vertex.data)) return false
        }
        cacheCount = 0
        flushCacheOnNextVertex = false
        return true
    }

    override fun gluTessVertex(coords: DoubleArray, coords_offset: Int, vertexData: Any?) {
        var tooLarge = false
        var x: Double
        val clamped = DoubleArray(3)
        requireState(TessState.T_IN_CONTOUR)
        if (flushCacheOnNextVertex) {
            if (!flushCache()) {
                callErrorOrErrorData(GLU.GLU_OUT_OF_MEMORY)
                return
            }
            lastEdge = null
        }
        var i = 0
        while (i < 3) {
            x = coords[i + coords_offset]
            if (x < -GLU.GLU_TESS_MAX_COORD) {
                x = -GLU.GLU_TESS_MAX_COORD
                tooLarge = true
            }
            if (x > GLU.GLU_TESS_MAX_COORD) {
                x = GLU.GLU_TESS_MAX_COORD
                tooLarge = true
            }
            clamped[i] = x
            ++i
        }
        if (tooLarge) {
            callErrorOrErrorData(GLU.GLU_TESS_COORD_TOO_LARGE)
        }
        if (mesh == null) {
            if (cacheCount < TESS_MAX_CACHE) {
                cacheVertex(clamped, vertexData)
                return
            }
            if (!flushCache()) {
                callErrorOrErrorData(GLU.GLU_OUT_OF_MEMORY)
                return
            }
        }
        if (!addVertex(clamped, vertexData)) {
            callErrorOrErrorData(GLU.GLU_OUT_OF_MEMORY)
        }
    }

    override fun gluTessBeginPolygon(data: Any?) {
        requireState(TessState.T_DORMANT)
        state = TessState.T_IN_POLYGON
        cacheCount = 0
        flushCacheOnNextVertex = false
        mesh = null
        polygonData = data
    }

    override fun gluTessBeginContour() {
        requireState(TessState.T_IN_POLYGON)
        state = TessState.T_IN_CONTOUR
        lastEdge = null
        if (cacheCount > 0) {
/* Just set a flag so we don't get confused by empty contours
 * -- these can be generated accidentally with the obsolete
 * NextContour() interface.
 */
            flushCacheOnNextVertex = true
        }
    }

    override fun gluTessEndContour() {
        requireState(TessState.T_IN_CONTOUR)
        state = TessState.T_IN_POLYGON
    }

    override fun gluTessEndPolygon() {
        val mesh: GLUmesh?
        try {
            requireState(TessState.T_IN_POLYGON)
            state = TessState.T_DORMANT
            if (this.mesh == null) {
                if (!flagBoundary /*&& callMesh == NULL_CB*/) {

/* Try some special code to make the easy cases go quickly
 * (e.g. convex polygons).  This code does NOT handle multiple contours,
 * intersections, edge flags, and of course it does not generate
 * an explicit mesh either.
 */
                    if (Render.__gl_renderCache(this)) {
                        polygonData = null
                        return
                    }
                }
                if (!flushCache()) throw RuntimeException() /* could've used a label*/
            }

/* Determine the polygon normal and project vertices onto the plane
         * of the polygon.
         */Normal.__gl_projectPolygon(this)

/* __gl_computeInterior( tess ) computes the planar arrangement specified
 * by the given contours, and further subdivides this arrangement
 * into regions.  Each region is marked "inside" if it belongs
 * to the polygon, according to the rule given by windingRule.
 * Each interior region is guaranteed be monotone.
 */
            if (!Sweep.__gl_computeInterior(this)) {
                throw RuntimeException() /* could've used a label */
            }
            mesh = this.mesh
            if (!fatalError) {

/* If the user wants only the boundary contours, we throw away all edges
 * except those which separate the interior from the exterior.
 * Otherwise we tessellate all the regions marked "inside".
 */
                val rc: Boolean = if (boundaryOnly) {
                    TessMono.__gl_meshSetWindingNumber(mesh!!, 1, true)
                } else {
                    TessMono.__gl_meshTessellateInterior(mesh!!)
                }
                if (!rc) throw RuntimeException() /* could've used a label */
                Mesh.__gl_meshCheckMesh(mesh)
                if (callBegin !== NULL_CB || callEnd !== NULL_CB || callVertex !== NULL_CB || callEdgeFlag !== NULL_CB || callBeginData !== NULL_CB || callEndData !== NULL_CB || callVertexData !== NULL_CB || callEdgeFlagData !== NULL_CB) {
                    if (boundaryOnly) {
                        Render.__gl_renderBoundary(this, mesh) /* output boundary contours */
                    } else {
                        Render.__gl_renderMesh(this, mesh) /* output strips and fans */
                    }
                }
                //                if (callMesh != NULL_CB) {
//
///* Throw away the exterior faces, so that all faces are interior.
//                 * This way the user doesn't have to check the "inside" flag,
//                 * and we don't need to even reveal its existence.  It also leaves
//                 * the freedom for an implementation to not generate the exterior
//                 * faces in the first place.
//                 */
//                    TessMono.__gl_meshDiscardExterior(mesh);
//                    callMesh.mesh(mesh);		/* user wants the mesh itself */
//                    mesh = null;
//                    polygonData = null;
//                    return;
//                }
            }
            Mesh.__gl_meshDeleteMesh(mesh!!)
            polygonData = null
        } catch (e: Exception) {
            e.printStackTrace()
            callErrorOrErrorData(GLU.GLU_OUT_OF_MEMORY)
        }
    }

    
    /*ARGSUSED*/
    override fun gluNextContour(type: Int) {
        gluTessEndContour()
        gluTessBeginContour()
    }

    override fun gluEndPolygon() {
        gluTessEndContour()
        gluTessEndPolygon()
    }

    fun callBeginOrBeginData(a: Int) {
        if (callBeginData !== NULL_CB) callBeginData.beginData(a, polygonData) else callBegin.begin(a)
    }

    fun callVertexOrVertexData(a: Any?) {
        if (callVertexData !== NULL_CB) callVertexData.vertexData(a, polygonData) else callVertex.vertex(a)
    }

    fun callEdgeFlagOrEdgeFlagData(a: Boolean) {
        if (callEdgeFlagData !== NULL_CB) callEdgeFlagData.edgeFlagData(a, polygonData) else callEdgeFlag.edgeFlag(a)
    }

    fun callEndOrEndData() {
        if (callEndData !== NULL_CB) callEndData.endData(polygonData) else callEnd.end()
    }

    fun callCombineOrCombineData(
        coords: DoubleArray?,
        vertexData: Array<Any?>?,
        weights: FloatArray?,
        outData: Array<Any?>?
    ) {
        if (callCombineData !== NULL_CB) callCombineData.combineData(
            coords,
            vertexData,
            weights,
            outData,
            polygonData
        ) else callCombine.combine(coords, vertexData, weights, outData)
    }

    fun callErrorOrErrorData(a: Int) {
        if (callErrorData !== NULL_CB) callErrorData.errorData(a, polygonData) else callError.error(a)
    }

    companion object {
        const val TESS_MAX_CACHE = 100
        private const val GLU_TESS_DEFAULT_TOLERANCE = 0.0

        //    private static final int GLU_TESS_MESH = 100112;	/* void (*)(GLUmesh *mesh)	    */
        private val NULL_CB: GLUtessellatorCallback =
            GLUtessellatorCallbackAdapter()

        fun gluNewTess(): GLUtessellator {
            return GLUtessellatorImpl()
        }
    }

    //    #define MAX_FAST_ALLOC	(MAX(sizeof(EdgePair), \
    //                 MAX(sizeof(GLUvertex),sizeof(GLUface))))
    init {
        state = TessState.T_DORMANT
        normal[0] = 0.0
        normal[1] = 0.0
        normal[2] = 0.0
        relTolerance = GLU_TESS_DEFAULT_TOLERANCE
        windingRule = GLU.GLU_TESS_WINDING_ODD
        flagBoundary = false
        boundaryOnly = false
        callBegin = NULL_CB
        callEdgeFlag = NULL_CB
        callVertex = NULL_CB
        callEnd = NULL_CB
        callError = NULL_CB
        callCombine = NULL_CB
        //        callMesh = NULL_CB;
        callBeginData = NULL_CB
        callEdgeFlagData = NULL_CB
        callVertexData = NULL_CB
        callEndData = NULL_CB
        callErrorData = NULL_CB
        callCombineData = NULL_CB
        polygonData = null
        for (i in cache.indices) {
            cache[i] = CachedVertex()
        }
    }
}
