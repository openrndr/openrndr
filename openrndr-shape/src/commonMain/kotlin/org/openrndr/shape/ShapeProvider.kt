package org.openrndr.shape

/**
 * provider of Shape
 */
interface ShapeProvider {
    /**
     * the provided shape
     */
    val shape: Shape
}

/**
 * provider of ShapeContour
 */
interface ShapeContourProvider {
    /**
     * the provided contour
     */
    val contour: ShapeContour
}
