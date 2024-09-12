package org.openrndr.shape

import org.openrndr.math.*

sealed interface Movable {
    /** Creates a new shape with the same size but the current position [offset] by the given amount. */
    fun movedBy(offset: Vector2): Movable

    /** Creates a new shape with the same size but the current position is set to [position]. */
    fun movedTo(position: Vector2): Movable
}

sealed interface Movable3D {
    /** Creates a new shape with the same size but the current position [offset] by the given amount. */
    fun movedBy(offset: Vector3): Movable3D

    /** Creates a new shape with the same size but the current position is set to [position]. */
    fun movedTo(position: Vector3): Movable3D
}

sealed interface Scalable1D {
    /** Current scale of this shape. Generally equivalent to its dimensions. */
    val scale: Vector2

    /**
     * Returns a position in the bounding box for parameterized
     * values [u] and [v] between `0.0` and `1.0` where
     * (`0.5`, `0.5`) is the center of the bounding box.
     */
    fun position(u: Double, v: Double): Vector2

    /**
     * Returns a position in the bounding box for a parameterized
     * [uv] value between (`0.0`, `0.0`) and (`1.0`, `1.0`) where
     * (`0.5`, `0.5`) is the center of the bounding box.
     */
    fun position(uv: Vector2) = position(uv.x, uv.y)

    /**
     * Creates a new shape with dimensions scaled by [scale].
     *
     * @param scale the scale factor
     * @param uAnchor x coordinate of the scaling anchor in u parameter space, default is 0.5 (center)
     * @param vAnchor y coordinate of the scaling anchor in v parameter space, default is 0.5 (center)
     */
    fun scaledBy(scale: Double, uAnchor: Double = 0.5, vAnchor: Double = 0.5): Scalable1D

    /** Creates a new shape at the same position with the given dimension, scaled uniformly. */
    fun scaledTo(size: Double): Scalable1D
}

sealed interface Scalable2D : Scalable1D {
    /**
     * Creates a new shape with dimensions scaled by [xScale] and [yScale].
     *
     * @param xScale the x scale factor
     * @param yScale the y scale factor
     * @param uAnchor x coordinate of the scaling anchor in u parameter space, default is 0.5 (center)
     * @param vAnchor y coordinate of the scaling anchor in v parameter space, default is 0.5 (center)
     */
    fun scaledBy(xScale: Double, yScale: Double, uAnchor: Double = 0.5, vAnchor: Double = 0.5): Scalable2D

    /** Creates a new shape at the same position with the given dimensions. */
    fun scaledTo(width: Double, height: Double): Scalable2D
}