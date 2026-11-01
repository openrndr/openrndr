package org.openrndr.internal.glcommon

import org.openrndr.shape.IntRectangle

/**
 * The fragment shader of copies that are drawn: it copies the source rectangle of `tex0` into the target rectangle,
 * which can have another size. The rectangles are in pixels of their levels, with the origin in the top-left corner.
 */
val copyRectangleShader = """
    |in vec2 v_texCoord0;
    |uniform sampler2D tex0;
    |// rectangles in pixels with the origin in the top-left corner: x, y, width, height
    |uniform vec4 sourceRectangle;
    |uniform vec4 targetRectangle;
    |uniform vec2 sourceLevelSize;
    |uniform vec2 targetLevelSize;
    |uniform float sourceLevel;
    |out vec4 o_color;
    |
    |void main() {
    |    vec2 p = vec2(v_texCoord0.x, 1.0 - v_texCoord0.y) * targetLevelSize;
    |    vec2 r = (p - targetRectangle.xy) / targetRectangle.zw;
    |    if (r.x < 0.0 || r.y < 0.0 || r.x >= 1.0 || r.y >= 1.0) {
    |        discard;
    |    }
    |    vec2 s = sourceRectangle.xy + r * sourceRectangle.zw;
    |    o_color = textureLod(tex0, vec2(s.x / sourceLevelSize.x, 1.0 - s.y / sourceLevelSize.y), sourceLevel);
    |}
""".trimMargin()

/**
 * The rectangles of a copy without rectangles: the overlapping region of the levels, anchored at the texture origin
 * (the bottom-left corner)
 * @return the source and target rectangles
 */
fun defaultCopyRectangles(sourceLevel: IntRectangle, targetLevel: IntRectangle): Pair<IntRectangle, IntRectangle> {
    val width = minOf(sourceLevel.width, targetLevel.width)
    val height = minOf(sourceLevel.height, targetLevel.height)
    return Pair(
        IntRectangle(0, sourceLevel.height - height, width, height),
        IntRectangle(0, targetLevel.height - height, width, height)
    )
}

/**
 * Clips the rectangles of an unscaled copy to the levels of the source and target
 * @return the clipped source and target rectangles, or null when nothing is left to copy
 */
fun clipCopyRectangles(
    sourceRectangle: IntRectangle,
    targetRectangle: IntRectangle,
    sourceLevel: IntRectangle,
    targetLevel: IntRectangle
): Pair<IntRectangle, IntRectangle>? {
    if (sourceRectangle.width < 0 || sourceRectangle.height < 0) {
        return Pair(sourceRectangle, targetRectangle)
    }
    var sx = sourceRectangle.x
    var sy = sourceRectangle.y
    var tx = targetRectangle.x
    var ty = targetRectangle.y
    var width = sourceRectangle.width
    var height = sourceRectangle.height

    val left = maxOf(0, -sx, -tx)
    sx += left; tx += left; width -= left
    val top = maxOf(0, -sy, -ty)
    sy += top; ty += top; height -= top
    width = minOf(width, sourceLevel.width - sx, targetLevel.width - tx)
    height = minOf(height, sourceLevel.height - sy, targetLevel.height - ty)

    return if (width <= 0 || height <= 0) null else
        Pair(IntRectangle(sx, sy, width, height), IntRectangle(tx, ty, width, height))
}
