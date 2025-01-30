package org.openrndr.ktessellation

/**
 * Interface representing a GLU tessellator, which provides functionality
 * for tessellating complex polygons into simpler primitives such as triangles
 * for rendering in OpenGL. It supports operations such as defining polygon vertices,
 * specifying tessellation normal, and setting callbacks for handling generated vertices.
 */
interface GLUtessellator {
    /**
     * Deletes a previously created GLU tessellation object.
     * This method should be called to free resources associated with
     * the tessellator when it is no longer needed. After calling this,
     * the tessellation object becomes invalid and should not be used.
     */
    fun gluDeleteTess()
    /**
     * Sets a tessellation property for the GLU tessellator.
     * This method allows configuring various properties of the tessellator, such as
     * winding rules, boundary-only settings, and tessellation tolerance.
     *
     * @param which The property to be set. This parameter is one of the predefined constants,
     *              such as GLU_TESS_WINDING_RULE, GLU_TESS_BOUNDARY_ONLY, or GLU_TESS_TOLERANCE.
     * @param value The value to assign to the specified property. This can either represent
     *              the desired tolerance or a setting corresponding to the given property.
     */
    fun gluTessProperty(which: Int, value: Double)
    /**
     * Sets a tessellation property for the GLU tessellator using an integer value.
     * This method internally converts the integer value to a double and delegates to
     * the corresponding method that accepts a double value.
     *
     * @param which The property to be set. This parameter is one of the predefined constants,
     *              such as GLU_TESS_WINDING_RULE, GLU_TESS_BOUNDARY_ONLY, or GLU_TESS_TOLERANCE.
     * @param value The integer value to assign to the specified property. This value will
     *              be converted to a double before being passed to the tessellator.
     */
    fun gluTessProperty(which: Int, value: Int) {
        gluTessProperty(which, value.toDouble())
    }

    /**
     * Retrieves the current value of a specified tessellation property.
     * This method is used to query a property of the GLU tessellator.
     *
     * @param which The property to be retrieved. This parameter specifies one of the predefined constants,
     *              such as GLU_TESS_WINDING_RULE, GLU_TESS_BOUNDARY_ONLY, or GLU_TESS_TOLERANCE.
     * @param value An array to store the retrieved property value. The value will be written into this array.
     * @param value_offset The offset within the array where the property value should be stored.
     */
    /* Returns tessellator property */
    fun gluGetTessProperty(
        which: Int, value: DoubleArray,
        value_offset: Int
    ) /* gluGetTessProperty() */

    /**
     * Specifies the normal vector for the tessellation.
     * This method is used to define a normal vector that provides a hint to the tessellator
     * for the orientation of the following vertices. The normal vector can help
     * determine which direction is "up" for planar tessellation.
     *
     * @param x The x component of the normal vector.
     * @param y The y component of the normal vector.
     * @param z The z component of the normal vector.
     */
    fun gluTessNormal(x: Double, y: Double, z: Double)
    /**
     * Defines a callback function for the GLU tessellation object.
     * The callback is associated with a specific tessellation event identified by the `which` parameter.
     * This method allows users to specify behavior for various tessellation-related events such as errors,
     * vertex processing, edge flags, and others.
     *
     * @param which An integer constant specifying the type of callback to be set. Valid values include
     *              predefined constants such as GLU_TESS_BEGIN, GLU_TESS_VERTEX, GLU_TESS_END, etc.
     *              These constants define the specific event for which the callback is being set.
     * @param aCallback The callback implementation to be invoked for the specified event. This should
     *                  be an instance of `GLUtessellatorCallback` or `null` if no callback is required
     *                  for the specified event.
     */
    fun gluTessCallback(
        which: Int,
        aCallback: GLUtessellatorCallback?
    )

    /**
     * Defines a vertex for tessellation processing in the GLU tessellator.
     * This method specifies a vertex using its coordinates and associates custom data
     * with the vertex. The vertex will be processed as part of the tessellation operation.
     *
     * @param coords A double array containing the x, y, and z coordinates of the vertex.
     *               This array must have at least three elements.
     * @param coords_offset The starting index within the `coords` array where the vertex
     *                      coordinates are located.
     * @param vertexData An optional user-defined object representing custom data associated
     *                   with the vertex. This data can be referenced in tessellation callbacks.
     */
    fun gluTessVertex(
        coords: DoubleArray, coords_offset: Int,
        vertexData: Any?
    )

    /**
     * Begins a new polygon definition for tessellation using the GLU tessellator.
     * This method starts the specification of a polygon, which will be defined using vertices
     * and may consist of multiple contours. The `data` parameter allows attaching custom
     * application-specific data to the polygon, which can be retrieved during callback events.
     *
     * @param data Custom application-specific data associated with the polygon. This can be any object, and its use is determined by the application.
     */
    fun gluTessBeginPolygon(data: Any?)
    /**
     * Marks the beginning of a new contour within the current polygon being defined
     * for tessellation in the GLU tessellator.
     *
     * This method is used after starting a polygon with `gluTessBeginPolygon` and
     * before specifying the vertices of the contour using `gluTessVertex`. Multiple
     * contours can be defined within a single polygon definition. Each contour adds
     * a loop to the polygon, contributing to its shape.
     *
     * It is required to call this method before starting any vertex definitions for
     * the contour and should be followed by a call to `gluTessEndContour` to
     * complete the contour specification.
     */
    fun gluTessBeginContour()
    /**
     * Marks the end of the current contour being defined for tessellation in the GLU tessellator.
     *
     * This method is used to signal the completion of a contour definition that
     * was initiated by a previous call to `gluTessBeginContour`. The defined contour,
     * along with its vertices, becomes part of the current polygon being specified.
     *
     * `gluTessEndContour` should always be called after defining all vertices
     * for a contour to ensure completeness. After completing a contour,
     * additional contours can be defined by calling `gluTessBeginContour` again
     * if needed. Once all contours have been defined for a polygon, the process
     * can be completed by calling `gluTessEndPolygon`.
     */
    fun gluTessEndContour()
    /**
     * Completes the definition of the current polygon in the tessellation process.
     *
     * This method is used to signal the end of a polygon's definition, which would
     * have been started with `gluTessBeginPolygon` and can include multiple contours
     * defined using `gluTessBeginContour` and `gluTessEndContour`. It finalizes the
     * polygon, allowing the tessellator to process and generate the tessellated output
     * for the specified polygon geometry.
     *
     * This method must be called to properly complete the tessellation of a polygon
     * after all contours and vertices have been specified.
     */
    fun gluTessEndPolygon()


    /*ARGSUSED*/
    fun gluNextContour(type: Int)
    fun gluEndPolygon()
}