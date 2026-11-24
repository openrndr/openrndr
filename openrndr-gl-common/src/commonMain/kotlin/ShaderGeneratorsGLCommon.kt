@file:Suppress("RemoveCurlyBracesFromTemplate")

package org.openrndr.internal.glcommon

import org.openrndr.draw.ShadeStructure
import org.openrndr.draw.ShadeStyleGLSL.Companion.drawerUniforms
import org.openrndr.draw.ShadeStyleGLSL.Companion.fragmentMainConstants
import org.openrndr.draw.ShadeStyleGLSL.Companion.postVertexTransform
import org.openrndr.draw.ShadeStyleGLSL.Companion.preVertexTransform
import org.openrndr.draw.ShadeStyleGLSL.Companion.primitiveTypes
import org.openrndr.draw.ShadeStyleGLSL.Companion.transformVaryingIn
import org.openrndr.draw.ShadeStyleGLSL.Companion.transformVaryingOut
import org.openrndr.draw.ShadeStyleGLSL.Companion.vertexMainConstants

import org.openrndr.internal.ShaderGenerators

private val rotate2 = """mat2 rotate2(float rotationInDegrees) {
    float r = radians(rotationInDegrees);
    float cr = cos(r);
    float sr = sin(r);
    return mat2(vec2(cr, sr), vec2(-sr, cr));
}
""".trimIndent()


/** the uniforms of the joins and caps of mesh lines */
private const val meshLineUniforms = """uniform highp int u_meshLineJoin;
uniform highp int u_meshLineCap;
uniform highp int u_meshLineWidthMode;
uniform highp float u_miterLimit;
uniform highp float u_smooth;
uniform highp float u_meshLineCoverageThreshold;"""

class ShaderGeneratorsGLCommon : ShaderGenerators {
    override fun vertexBufferFragmentShader(shadeStructure: ShadeStructure): String = """|
|${primitiveTypes("d_vertex_buffer")}
|${shadeStructure.structDefinitions ?: ""}
|${shadeStructure.buffers ?: ""}
|${shadeStructure.uniforms ?: ""}

|uniform sampler2D image;
|${drawerUniforms()}
|${shadeStructure.varyingIn ?: ""}
|${shadeStructure.outputs ?: ""}
|${transformVaryingIn}

|#ifndef OUTPUT_color
|${if (!shadeStructure.suppressDefaultOutput) "out vec4 o_color;" else ""}
|#endif
|
|flat in int v_instance;
|flat in float va_pointSize;
|in vec3 v_objectPosition;
|${fragmentMainConstants(element = "v_instance")}
|${shadeStructure.fragmentPreamble ?: ""}
|void main(void) {
|    vec4 x_fill = u_fill;
|    vec4 x_stroke = u_stroke;
|    {
${shadeStructure.fragmentTransform?.prependIndent("        ") ?: ""}
|    }
     ${
        if (!shadeStructure.suppressDefaultOutput) """
     |    o_color = x_fill;
     |    o_color.rgb *= o_color.a;
     |""".trimMargin() else ""
    }
|}""".trimMargin()

    override fun vertexBufferVertexShader(shadeStructure: ShadeStructure): String = """
${primitiveTypes("d_vertex_buffer")}
${shadeStructure.structDefinitions ?: ""}
${shadeStructure.buffers ?: ""}
${drawerUniforms()}
${shadeStructure.attributes ?: ""}
${shadeStructure.uniforms ?: ""}
${shadeStructure.varyingOut ?: ""}
${transformVaryingOut}
${shadeStructure.vertexPreamble ?: ""}

flat out int v_instance;
flat out float va_pointSize;
out vec3 v_objectPosition;
void main() {
    int instance = gl_InstanceID; // this will go use c_instance instead
${vertexMainConstants()}
${shadeStructure.varyingBridge ?: ""}
    vec3 x_normal = vec3(0.0, 0.0, 0.0);
    ${if (shadeStructure.attributes?.contains("vec3 a_normal;") == true) "x_normal = a_normal;" else ""}
    vec3 x_position = a_position.xyz;
    float x_pointSize = 1.0;
    v_objectPosition = a_position.xyz;

    ${preVertexTransform}
    {
${shadeStructure.vertexTransform?.prependIndent("        ") ?: ""}
    }
    ${postVertexTransform}

    v_instance = instance;
    gl_Position = v_clipPosition;
    gl_PointSize = x_pointSize;
    va_pointSize = x_pointSize;
}
            """.trimMargin()

    override fun imageFragmentShader(shadeStructure: ShadeStructure): String = """
${primitiveTypes("d_image")}
${shadeStructure.structDefinitions ?: ""}
${shadeStructure.buffers ?: ""}
${shadeStructure.uniforms ?: ""}

uniform sampler2D image;
${drawerUniforms()}
${shadeStructure.varyingIn ?: ""}
${transformVaryingIn}
in vec3 v_objectPosition;

${shadeStructure.outputs ?: ""}

#ifndef OUTPUT_color
${if (!shadeStructure.suppressDefaultOutput) "out vec4 o_color;" else ""}
#endif

in vec3 v_boundsPosition;
flat in int v_instance;
vec4 colorTransform(vec4 color, float[25] matrix) {
    float r = color.r * matrix[0] + color.g * matrix[5] + color.b * matrix[10] + color.a * matrix[15] + matrix[20];
    float g = color.r * matrix[1] + color.g * matrix[6] + color.b * matrix[11] + color.a * matrix[16] + matrix[21];
    float b = color.r * matrix[2] + color.g * matrix[7] + color.b * matrix[12] + color.a * matrix[17] + matrix[22];
    float a = color.r * matrix[3] + color.g * matrix[8] + color.b * matrix[13] + color.a * matrix[18] + matrix[23];
    return vec4(r, g, b, a);
}
${fragmentMainConstants(boundsPosition = "v_boundsPosition", boundsSize = "vec3(vi_target.zw, 0.0)")}
${shadeStructure.fragmentPreamble ?: ""}
void main(void) {

    vec4 x_fill = texture(image, va_texCoord0);
    vec4 x_stroke = u_stroke;
    float div = x_fill.a != 0.0 ? x_fill.a : 1.0;
    x_fill.rgb /= div;
    {
${shadeStructure.fragmentTransform?.prependIndent("        ") ?: ""}
    }
    x_fill = colorTransform(x_fill, u_colorMatrix);
    x_fill.rgb *= x_fill.a;
    ${if (!shadeStructure.suppressDefaultOutput) "o_color = x_fill;" else ""}
}"""

    override fun imageVertexShader(shadeStructure: ShadeStructure): String = """

${primitiveTypes("d_image")}
${shadeStructure.structDefinitions ?: ""}
${shadeStructure.buffers ?: ""}
${drawerUniforms()}
uniform int u_flipV;
${shadeStructure.attributes ?: ""}
${shadeStructure.uniforms ?: ""}
${shadeStructure.varyingOut ?: ""}
${transformVaryingOut}
out vec3 v_objectPosition;

${vertexMainConstants()}
${shadeStructure.vertexPreamble ?: ""}

flat out int v_instance;

out vec3 v_boundsPosition;
void main() {
    v_instance = gl_InstanceID;

    ${shadeStructure.varyingBridge ?: ""}
    ${preVertexTransform}
    vec3 x_normal = a_normal;
    vec3 x_position = a_position.xyz;
    x_position.xy = a_position.xy * i_target.zw + i_target.xy;
    v_boundsPosition = vec3(a_texCoord0.xy, 1.0);
    va_texCoord0.xy = a_texCoord0.xy * i_source.zw + i_source.xy;
    if (u_flipV == 0) {
        va_texCoord0.y = 1.0 - va_texCoord0.y;
    }
    // the position before the model transform
    v_objectPosition = x_position;
    {
${shadeStructure.vertexTransform?.prependIndent("        ") ?: ""}
    }
    ${postVertexTransform}
    gl_Position = v_clipPosition;
}
"""

    override fun imageArrayTextureFragmentShader(shadeStructure: ShadeStructure): String = """
${primitiveTypes("d_image")}
${shadeStructure.structDefinitions ?: ""}
${shadeStructure.buffers ?: ""}
${shadeStructure.uniforms ?: ""}

uniform sampler2DArray image;
${drawerUniforms()}
${shadeStructure.varyingIn ?: ""}
${transformVaryingIn}
in vec3 v_objectPosition;

#ifndef OUTPUT_color
${if (!shadeStructure.suppressDefaultOutput) "out vec4 o_color;" else ""}
#endif


in vec3 v_boundsPosition;
flat in int v_instance;
flat in int v_layer;
vec4 colorTransform(vec4 color, float[25] matrix) {
    float r = color.r * matrix[0] + color.g * matrix[5] + color.b * matrix[10] + color.a * matrix[15] + matrix[20];
    float g = color.r * matrix[1] + color.g * matrix[6] + color.b * matrix[11] + color.a * matrix[16] + matrix[21];
    float b = color.r * matrix[2] + color.g * matrix[7] + color.b * matrix[12] + color.a * matrix[17] + matrix[22];
    float a = color.r * matrix[3] + color.g * matrix[8] + color.b * matrix[13] + color.a * matrix[18] + matrix[23];
    return vec4(r, g, b, a);
}

${fragmentMainConstants(boundsPosition = "v_boundsPosition")}
${shadeStructure.fragmentPreamble ?: ""}
void main(void) {
    vec4 x_fill = texture(image, vec3(va_texCoord0, float(v_layer) * 1.0));
    vec4 x_stroke = u_stroke;
    {
${shadeStructure.fragmentTransform?.prependIndent("        ") ?: ""}
    }
    float div = x_fill.a != 0.0 ? x_fill.a : 1.0;
    x_fill.rgb /= div;
    x_fill = colorTransform(x_fill, u_colorMatrix);
    x_fill.rgb *= x_fill.a;
    ${if (!shadeStructure.suppressDefaultOutput) "o_color = x_fill;" else ""}
}"""

    override fun imageArrayTextureVertexShader(shadeStructure: ShadeStructure): String = """

${primitiveTypes("d_image")}
${shadeStructure.structDefinitions ?: ""}
${shadeStructure.buffers ?: ""}
${drawerUniforms()}
uniform int u_flipV;
${shadeStructure.attributes ?: ""}
${shadeStructure.uniforms ?: ""}
${shadeStructure.varyingOut ?: ""}
${transformVaryingOut}
out vec3 v_objectPosition;

${vertexMainConstants()}
${shadeStructure.vertexPreamble ?: ""}

flat out int v_instance;
flat out int v_layer;

out vec3 v_boundsPosition;
void main() {
    v_instance = gl_InstanceID;

    ${shadeStructure.varyingBridge ?: ""}
    ${preVertexTransform}
    vec3 x_normal = a_normal;
    vec3 x_position = a_position.xyz;
    x_position.xy = a_position.xy * i_target.zw + i_target.xy;
    v_boundsPosition = vec3(a_texCoord0.xy, 1.0);
    va_texCoord0.xy = a_texCoord0.xy * i_source.zw + i_source.xy;
    v_layer = int(floor(i_layer+0.5));
    if (u_flipV == 0) {
        va_texCoord0.y = 1.0 - va_texCoord0.y;
    }
    // the position before the model transform
    v_objectPosition = x_position;
    {
${shadeStructure.vertexTransform?.prependIndent("        ") ?: ""}
    }
    ${postVertexTransform}
    gl_Position = v_clipPosition;
}
"""

    override fun pointFragmentShader(shadeStructure: ShadeStructure): String = """
${primitiveTypes("d_circle")}
${shadeStructure.structDefinitions ?: ""}
${shadeStructure.buffers ?: ""}
${shadeStructure.uniforms ?: ""}

${drawerUniforms(styleBlock = false)}
${shadeStructure.varyingIn ?: ""}
${transformVaryingIn}
in vec3 v_objectPosition;

#ifndef OUTPUT_color
${if (!shadeStructure.suppressDefaultOutput) "out vec4 o_color;" else ""}
#endif


flat in int v_instance;
in vec3 v_boundsSize;
flat in float va_pointSize;
${
        fragmentMainConstants(boundsPosition = "vec3(0.0, 0.0, 0.0)", boundsSize = "v_boundsSize")
    }
${shadeStructure.fragmentPreamble ?: ""}
void main(void) {


    vec4 x_fill = vi_fill;
    vec4 x_stroke = vi_stroke;
    {
${shadeStructure.fragmentTransform?.prependIndent("        ") ?: ""}
    }
    x_fill.rgb *= x_fill.a;
    ${if (!shadeStructure.suppressDefaultOutput) "o_color = x_fill;" else ""}
}

    """.trimMargin()

    override fun pointVertexShader(shadeStructure: ShadeStructure): String = """
${primitiveTypes("d_point")}
${shadeStructure.structDefinitions ?: ""}
${shadeStructure.buffers ?: ""}
${drawerUniforms(styleBlock = false)}
${shadeStructure.attributes ?: ""}
${shadeStructure.uniforms ?: ""}
${shadeStructure.varyingOut ?: ""}
${transformVaryingOut}
out vec3 v_objectPosition;

${vertexMainConstants()}
${shadeStructure.vertexPreamble ?: ""}

flat out int v_instance;
out vec3 v_boundsSize;
flat out float va_pointSize;
void main() {
    v_instance = gl_InstanceID;

    ${shadeStructure.varyingBridge ?: ""}

    v_boundsSize = vec3(0, 0.0, 0.0);
    ${preVertexTransform}
    vec3 x_normal = vec3(0.0, 0.0, 1.0);
    vec3 x_position = a_position.xyz + i_offset;
    float x_pointSize = 1.0;
    // the position before the model transform
    v_objectPosition = x_position;
    {
${shadeStructure.vertexTransform?.prependIndent("        ") ?: ""}
    }
    va_position = x_position;
    ${postVertexTransform}
    gl_Position = v_clipPosition;
    gl_PointSize = x_pointSize;
    va_pointSize = x_pointSize;
}"""


    override fun circleFragmentShader(shadeStructure: ShadeStructure): String = """
${primitiveTypes("d_circle")}
${shadeStructure.structDefinitions ?: ""}
${shadeStructure.uniforms ?: ""}
${shadeStructure.buffers ?: ""}

${drawerUniforms(styleBlock = false)}
${shadeStructure.varyingIn ?: ""}
${transformVaryingIn}
in vec3 v_objectPosition;

#ifndef OUTPUT_color
${if (!shadeStructure.suppressDefaultOutput) "out vec4 o_color;" else ""}
#endif

${shadeStructure.fragmentPreamble ?: ""}

flat in int v_instance;
in vec3 v_boundsSize;
in vec2 v_local;
// the fraction of the stroke that lies outside of the outline: 0.0 inside, 0.5 centered, 1.0 outside
uniform float u_strokeAlignment;
// 1.0 for anti-aliased edges, 0.0 for hard edges
uniform float u_smooth;
// 0: the stroke is painted over the fill, 1: the fill over the stroke
uniform int u_paintOrder;
${
        fragmentMainConstants(
            // the bounds of a circle are its diameter, v_boundsSize holds its radius
            boundsPosition = "vec3(va_texCoord0, 0.0)", boundsSize = "v_boundsSize * 2.0"
        )
    }

// coverage of the region with signed distance [d] (negative inside) for a pixel of size [f]
float edgeCoverage(float d, float f) {
    return u_smooth != 0.0 ? clamp(0.5 - d / f, 0.0, 1.0) : (d <= 1E-3 * f ? 1.0 : 0.0);
}


void main(void) {
    vec4 x_fill = vi_fill;
    vec4 x_stroke = vi_stroke;
    float x_strokeWeight = vi_strokeWeight;

    {
${shadeStructure.fragmentTransform?.prependIndent("        ") ?: ""}
    }
    // distances are in object units, a centered stroke is as in CSS/SVG
    float d = length(v_local);
    // the size of a pixel across the edge, fwidth() would overestimate it for edges that are not axis aligned
    float f = max(length(vec2(dFdx(d), dFdy(d))), 1E-6);
    float r = vi_radius.x;
    float outer = r + x_strokeWeight * u_strokeAlignment;
    float inner = r - x_strokeWeight * (1.0 - u_strokeAlignment);

    float fillCoverage = edgeCoverage(d - r, f);
    float outerCoverage = edgeCoverage(d - outer, f);
    float innerCoverage = inner > 0.0 ? edgeCoverage(d - inner, f) : 0.0;
    float strokeCoverage = max(outerCoverage - innerCoverage, 0.0);
    if (u_smooth == 0.0 && fillCoverage == 0.0 && strokeCoverage == 0.0) {
        discard;
    }

    // the stroke is painted over the fill, or the fill over the stroke by the paint order
    float sa = strokeCoverage * x_stroke.a;
    float fa = fillCoverage * x_fill.a;
    vec4 final = vec4(0.0);
    if (u_paintOrder == 1) {
        final.rgb = x_fill.rgb * fa + x_stroke.rgb * sa * (1.0 - fa);
        final.a = fa + sa * (1.0 - fa);
    } else {
        final.rgb = x_stroke.rgb * sa + x_fill.rgb * fa * (1.0 - sa);
        final.a = sa + fa * (1.0 - sa);
    }
    ${if (!shadeStructure.suppressDefaultOutput) "o_color = final;" else ""}
}
"""

    override fun circleVertexShader(shadeStructure: ShadeStructure): String = """
// -- circle vertex shader
${primitiveTypes("d_circle")}
${shadeStructure.structDefinitions ?: ""}
${shadeStructure.buffers ?: ""}
${drawerUniforms(styleBlock = false)}
${shadeStructure.attributes ?: ""}
${shadeStructure.uniforms ?: ""}
${shadeStructure.varyingOut ?: ""}
${transformVaryingOut}
out vec3 v_objectPosition;

${vertexMainConstants()}
${shadeStructure.vertexPreamble ?: ""}

flat out int v_instance;
out vec3 v_boundsSize;
out vec2 v_local;
uniform float u_strokeAlignment;
uniform float u_smooth;
void main() {
    v_instance = gl_InstanceID;

    ${shadeStructure.varyingBridge ?: ""}

    // the quad covers the part of the stroke outside of the circle plus a margin for anti-aliasing
    vec2 effectiveRadius = i_radius.xy + vec2(i_strokeWeight * u_strokeAlignment) + u_smooth * vec2(1.25 / u_contentScale) / (u_modelViewScalingFactor);
    // texture coordinates span the bounds of the circle, not those of the quad
    va_texCoord0 = vec2(0.5) + (a_texCoord0 - vec2(0.5)) * effectiveRadius / max(i_radius.xy, vec2(1E-6));
    v_local = a_position.xy * effectiveRadius;

    v_boundsSize = vec3(i_radius.xy, 0.0);
    ${preVertexTransform}
    vec3 x_normal = a_normal;
    vec3 x_position = vec3(a_position.xy * effectiveRadius, 0.0) + i_offset;
    // the position before the model transform
    v_objectPosition = x_position;
    {
${shadeStructure.vertexTransform?.prependIndent("        ") ?: ""}
    }
    va_position = x_position;
    ${postVertexTransform}
    gl_Position = v_clipPosition;

}
    """

    override fun fontImageMapFragmentShader(shadeStructure: ShadeStructure): String = """
${primitiveTypes("d_font_image_map")}
${shadeStructure.structDefinitions ?: ""}
${shadeStructure.buffers ?: ""}
${shadeStructure.uniforms ?: ""}

uniform sampler2D image;
flat in int v_instance;
flat in int v_element;

${drawerUniforms()}
${shadeStructure.varyingIn ?: ""}
${transformVaryingIn}
in vec3 v_objectPosition;

#ifndef OUTPUT_color
${if (!shadeStructure.suppressDefaultOutput) "out vec4 o_color;" else ""}
#endif

${shadeStructure.fragmentPreamble ?: ""}

${
        fragmentMainConstants(
            element = "v_element",
            instance = "v_instance",
            boundsPosition = "vec3(va_bounds.xy, 0.0)",
            boundsSize = "vec3(va_bounds.zw, 0.0)"
        )
    }
void main(void) {
    float imageMap = texture(image, va_texCoord0).r;

    vec4 x_fill = vec4(u_fill.rgb,u_fill.a * imageMap);
    vec4 x_stroke = u_stroke;
    {
${shadeStructure.fragmentTransform?.prependIndent("        ") ?: ""}
    }
    vec4 final = x_fill;
    final.rgb *= final.a;
    ${if (!shadeStructure.suppressDefaultOutput) "o_color = final;" else ""}
}
"""

    override fun fontImageMapVertexShader(shadeStructure: ShadeStructure): String = """
${primitiveTypes("d_font_image_map")}
${shadeStructure.structDefinitions ?: ""}
${shadeStructure.buffers ?: ""}
${drawerUniforms()}

${shadeStructure.attributes ?: ""}
${shadeStructure.uniforms ?: ""}
${shadeStructure.varyingOut ?: ""}
${transformVaryingOut}
out vec3 v_objectPosition;
${vertexMainConstants("int(a_position.z)")}
${shadeStructure.vertexPreamble ?: ""}
flat out int v_instance;
flat out int v_element;

void main() {

    vec3 decodedPosition = vec3(a_position.xy, 0.0);
    v_element = int(a_position.z);
    v_instance = int(a_instance);

    ${shadeStructure.varyingBridge ?: ""}
    ${preVertexTransform}
    vec3 x_normal = vec3(0.0, 0.0, 1.0);
    vec3 x_position = decodedPosition;
    // the position before the model transform
    v_objectPosition = x_position;
    {
${shadeStructure.vertexTransform?.prependIndent("        ") ?: ""}
    }
    ${postVertexTransform}
    gl_Position = v_clipPosition;
}
            """

    override fun rectangleFragmentShader(shadeStructure: ShadeStructure): String = """
${primitiveTypes("d_rectangle")}
${shadeStructure.structDefinitions ?: ""}
${shadeStructure.buffers ?: ""}
${shadeStructure.uniforms ?: ""}

${drawerUniforms(styleBlock = false)}
${shadeStructure.varyingIn ?: ""}
${shadeStructure.outputs ?: ""}
${transformVaryingIn}
in vec3 v_objectPosition;

#ifndef OUTPUT_color
${if (!shadeStructure.suppressDefaultOutput) "out vec4 o_color;" else ""}
#endif

${shadeStructure.fragmentPreamble ?: ""}
flat in int v_instance;
in vec3 v_boundsSize;
in vec2 v_local;
// the fraction of the stroke that lies outside of the outline: 0.0 inside, 0.5 centered, 1.0 outside
uniform float u_strokeAlignment;
// the join of the outer stroke corners: 0 miter, 1 bevel, 2 round
uniform int u_lineJoin;
uniform float u_miterLimit;
// 1.0 for anti-aliased edges, 0.0 for hard edges
uniform float u_smooth;
// 0: the stroke is painted over the fill, 1: the fill over the stroke
uniform int u_paintOrder;

${
        fragmentMainConstants(
            boundsPosition = "vec3(va_texCoord0, 0.0)",
            boundsSize = "v_boundsSize"
        )
    }

// box filtered coverage of the interval [-halfExtent, halfExtent] for a pixel of size f centered at x
float intervalCoverage(float halfExtent, float x, float f) {
    return clamp(min(x + 0.5 * f, halfExtent) - max(x - 0.5 * f, -halfExtent), 0.0, f) / f;
}

float boxCoverage(vec2 halfExtents, vec2 p, vec2 f) {
    if (u_smooth != 0.0) {
        return intervalCoverage(halfExtents.x, p.x, f.x) * intervalCoverage(halfExtents.y, p.y, f.y);
    }
    // hard edges: a pixel is covered when its center is
    vec2 q = abs(p) - halfExtents;
    return (q.x <= 1E-3 * f.x && q.y <= 1E-3 * f.y) ? 1.0 : 0.0;
}

// coverage of the region with signed distance [d] (negative inside) for a pixel of size [f]
float edgeCoverage(float d, float f) {
    return u_smooth != 0.0 ? clamp(0.5 - d / f, 0.0, 1.0) : (d <= 1E-3 * f ? 1.0 : 0.0);
}

// coverage of the rectangle with half extents [halfSize] grown by [o], with its corners joined by [lineJoin]
float outsetCoverage(vec2 halfSize, float o, vec2 p, vec2 f, int lineJoin) {
    float box = boxCoverage(halfSize + vec2(o), p, f);
    if (lineJoin == 0 || o <= 0.0) {
        return box;
    }
    // position relative to the nearest corner of the rectangle
    vec2 c = abs(p) - halfSize;
    float pf = length(f) * 0.70710678;
    if (lineJoin == 2) {
        float d = length(max(c, vec2(0.0))) + min(max(c.x, c.y), 0.0) - o;
        return min(box, edgeCoverage(d, pf));
    } else if (lineJoin == 3) {
        // the miter is clipped perpendicular to the diagonal, at the miter limit times the outset from the corner
        float d = (c.x + c.y) * 0.70710678 - u_miterLimit * o;
        return min(box, edgeCoverage(d, pf));
    } else {
        float d = (c.x + c.y - o) * 0.70710678;
        return min(box, edgeCoverage(d, pf));
    }
}

void main(void) {
    vec4 x_fill = vi_fill;
    vec4 x_stroke = vi_stroke;
    float x_strokeWeight = vi_strokeWeight;
    {
${shadeStructure.fragmentTransform?.prependIndent("        ") ?: ""}
    }
    // distances are in object units, a centered stroke is as in CSS/SVG, with mitered corners
    // the size of a pixel across the edges, fwidth() would overestimate it for rotated rectangles
    vec2 f = max(vec2(length(vec2(dFdx(v_local.x), dFdy(v_local.x))), length(vec2(dFdx(v_local.y), dFdy(v_local.y)))), vec2(1E-6));
    vec2 halfSize = abs(vi_dimensions) * 0.5;

    float fillCoverage = boxCoverage(halfSize, v_local, f);
    float outerCoverage = outsetCoverage(halfSize, x_strokeWeight * u_strokeAlignment, v_local, f, u_lineJoin);
    vec2 innerHalfSize = halfSize - vec2(x_strokeWeight * (1.0 - u_strokeAlignment));
    float innerCoverage = (innerHalfSize.x > 0.0 && innerHalfSize.y > 0.0) ? boxCoverage(innerHalfSize, v_local, f) : 0.0;
    float strokeCoverage = max(outerCoverage - innerCoverage, 0.0);
    if (u_smooth == 0.0 && fillCoverage == 0.0 && strokeCoverage == 0.0) {
        discard;
    }

    // the stroke is painted over the fill, or the fill over the stroke by the paint order
    float sa = strokeCoverage * x_stroke.a;
    float fa = fillCoverage * x_fill.a;
    vec4 final = vec4(0.0);
    if (u_paintOrder == 1) {
        final.rgb = x_fill.rgb * fa + x_stroke.rgb * sa * (1.0 - fa);
        final.a = fa + sa * (1.0 - fa);
    } else {
        final.rgb = x_stroke.rgb * sa + x_fill.rgb * fa * (1.0 - sa);
        final.a = sa + fa * (1.0 - sa);
    }

    ${
        if (!shadeStructure.suppressDefaultOutput) """
    |   o_color = final;
    """.trimMargin() else ""
    }
}
"""

    override fun rectangleVertexShader(shadeStructure: ShadeStructure): String = """
${primitiveTypes("d_rectangle")}
${shadeStructure.structDefinitions ?: ""}
${shadeStructure.buffers ?: ""}
${drawerUniforms(styleBlock = false)}
${shadeStructure.attributes ?: ""}
${shadeStructure.uniforms ?: ""}
${shadeStructure.varyingOut ?: ""}
${transformVaryingOut}
out vec3 v_objectPosition;

${vertexMainConstants()}
${shadeStructure.vertexPreamble ?: ""}

flat out int v_instance;
out vec3 v_boundsSize;
out vec2 v_local;
uniform float u_strokeAlignment;
uniform float u_smooth;
${rotate2}

void main() {
    v_instance =  gl_InstanceID;
    ${shadeStructure.varyingBridge ?: ""}
    ${preVertexTransform}
    vec3 x_normal = vec3(0.0, 0.0, 1.0);
    // the quad covers the part of the stroke outside of the rectangle plus a margin for anti-aliasing
    vec2 margin = vec2(i_strokeWeight * u_strokeAlignment) + u_smooth * vec2(1.25 / u_contentScale) / (u_modelViewScalingFactor);
    vec2 quadSize = i_dimensions + sign(i_dimensions) * margin * 2.0;
    v_local = (a_position.xy - vec2(0.5)) * quadSize;
    // texture coordinates span the bounds of the rectangle, not those of the quad
    va_texCoord0 = vec2(0.5) + (a_texCoord0 - vec2(0.5)) * abs(quadSize) / max(abs(i_dimensions), vec2(1E-6));
    vec2 rotatedPosition = rotate2(i_rotation) * v_local + vec2(0.5) * i_dimensions;

    vec3 x_position = vec3(rotatedPosition, 0.0) + i_offset;
    v_boundsSize = vec3(i_dimensions, 1.0);
    // the position before the model transform
    v_objectPosition = x_position;
    {
${shadeStructure.vertexTransform?.prependIndent("        ") ?: ""}
    }
    ${postVertexTransform}
    gl_Position = v_clipPosition;
    }
    """

    override fun expansionFragmentShader(shadeStructure: ShadeStructure): String = """
${primitiveTypes("d_expansion")}
${shadeStructure.structDefinitions ?: ""}
${shadeStructure.buffers ?: ""}
${shadeStructure.uniforms ?: ""}

${drawerUniforms()}
${shadeStructure.varyingIn ?: ""}
${transformVaryingIn}
flat in int v_instance;
uniform float strokeMult;
uniform float strokeThr;
uniform float strokeFillFactor;
uniform sampler2D tex;
uniform vec4 bounds;
// 0: strokes and convex fills, the fringes of stenciled fills: 1 outside of the fill, 2 inside of the fill,
// 3 inside on the left of the outline, 4 inside on the right of the outline
uniform int fringeMode;
// 1: the coverage of strokes is measured in device pixels, strokeEdge is |2u - 1| on the edge of the stroke
uniform int strokeCoverage;
uniform float strokeEdge;

in vec3 v_objectPosition;
in vec2 v_ftcoord;

#ifndef OUTPUT_color
${if (!shadeStructure.suppressDefaultOutput) "out vec4 o_color;" else ""}
#endif

${shadeStructure.fragmentPreamble ?: ""}

float strokeMask() {
    if (strokeCoverage == 1) {
        // the coverage of the pixel by the stroke across it: the distance of the pixel to the center of the stroke
        // and the half width of the stroke in device pixels, by the change of u across pixels
        float across = 1.0;
        float du = length(vec2(dFdx(v_ftcoord.x), dFdy(v_ftcoord.x)));
        if (du > 1e-12) {
            float center = abs(v_ftcoord.x * 2.0 - 1.0) / (2.0 * du);
            float halfWidth = strokeEdge / (2.0 * du);
            across = clamp(min(center + halfWidth, 0.5) - max(center - halfWidth, -0.5), 0.0, 1.0);
        }
        // caps and bevels: v is 0 a fringe outside of their edge, 0.5 on it
        float dv = length(vec2(dFdx(v_ftcoord.y), dFdy(v_ftcoord.y)));
        float along = dv > 1e-12 ? clamp((v_ftcoord.y - 0.5) / dv + 0.5, 0.0, 1.0) : min(1.0, v_ftcoord.y);
        return across * along;
    }
	return min(1.0, (1.0-abs(v_ftcoord.x*2.0-1.0))*strokeMult) * min(1.0, v_ftcoord.y);
	//return pow(min(1.0, (1.0-abs(v_ftcoord.x*2.0-1.0)*strokeMult)) * min(1.0, v_ftcoord.y), 1.0);
    //return smoothstep(0.0, 1.0, (1.0-abs(v_ftcoord.x*2.0-1.0))*strokeMult) * smoothstep(0.0, 1.0, v_ftcoord.y);
}

${
        fragmentMainConstants(
            boundsPosition = "vec3(v_objectPosition.xy - bounds.xy, 0.0) / vec3(bounds.zw,1.0)",
            boundsSize = "vec3(bounds.zw, 0.0)",
            contourPosition = "va_vertexOffset"
        )
    }

void main(void) {
	float strokeAlpha = strokeMask();
    if (fringeMode != 0) {
        // the fringe is centered on the outline, half of a pixel on both sides. The coverage of a pixel on the
        // outside is a half minus its distance to the outline, on the inside a half plus its distance
        float outside = 0.5 * (1.0 - abs(v_ftcoord.x * 2.0 - 1.0));
        // the fringe of a side of the outline has v = 2 on the side its u = 0 is on, which is on the left of the
        // outline when the strip faces the back. The windings on the left of the outline are one more than on the right
        bool left = (v_ftcoord.y > 1.5) != gl_FrontFacing;
        if ((fringeMode == 3 && !left) || (fringeMode == 4 && left)) {
            discard;
        }
        strokeAlpha = fringeMode == 1 ? outside : 1.0 - outside;
    }

    vec4 x_stroke = u_stroke;
    vec4 x_fill = u_fill;

${shadeStructure.fragmentTransform?.prependIndent("        ") ?: ""}

    vec4 color = mix(x_stroke, x_fill, strokeFillFactor)  * vec4(1.0, 1.0, 1.0, strokeAlpha);
    vec4 result = color;

    if (strokeAlpha < strokeThr) {
	    discard;
	}

    vec4 final = result;
	final = result;
	final.rgb *= final.a;
    ${if (!shadeStructure.suppressDefaultOutput) "o_color = final;" else ""}
}
"""

    override fun expansionVertexShader(shadeStructure: ShadeStructure): String = """
${primitiveTypes("d_expansion")}
${shadeStructure.structDefinitions ?: ""}
${shadeStructure.buffers ?: ""}
${drawerUniforms()}
${shadeStructure.uniforms ?: ""}
${shadeStructure.attributes}
${shadeStructure.varyingOut ?: ""}
${transformVaryingOut}

${vertexMainConstants()}
${shadeStructure.vertexPreamble ?: ""}

out vec2 v_ftcoord;
out float v_offset;

out vec3 v_objectPosition;
flat out int v_instance;

void main() {
    v_instance = 0;
    ${shadeStructure.varyingBridge ?: ""}
    v_objectPosition = vec3(a_position, 0.0);
    v_ftcoord = a_texCoord0;

    vec3 x_position = vec3(a_position, 0.0);
    vec3 x_normal = vec3(0.0, 0.0, 1.0);
    $preVertexTransform
    {
${shadeStructure.vertexTransform?.prependIndent("        ") ?: ""}
    }
    $postVertexTransform

    gl_Position = v_clipPosition;
}
"""

    override fun fastLineFragmentShader(shadeStructure: ShadeStructure): String = """
${primitiveTypes("d_fast_line")}
${shadeStructure.structDefinitions ?: ""}
${shadeStructure.buffers ?: ""}
${shadeStructure.uniforms ?: ""}

uniform sampler2D image;
${drawerUniforms()}
${shadeStructure.varyingIn ?: ""}
$transformVaryingIn
in vec3 v_objectPosition;
flat in int v_instance;

#ifndef OUTPUT_color
${if (!shadeStructure.suppressDefaultOutput) "out vec4 o_color;" else ""}
#endif

${shadeStructure.fragmentPreamble ?: ""}

${fragmentMainConstants()}
void main(void) {
    vec4 x_fill = u_fill;
    vec4 x_stroke = u_stroke;
    {
${shadeStructure.fragmentTransform?.prependIndent("        ") ?: ""}
    }


    vec4 final = x_stroke;
    final = x_stroke;
    final.rgb *= final.a;
    ${if (!shadeStructure.suppressDefaultOutput) "o_color = final;" else ""}
}
"""

    override fun fastLineVertexShader(shadeStructure: ShadeStructure): String = """
${primitiveTypes("d_fast_line")}
${shadeStructure.structDefinitions ?: ""}
${shadeStructure.buffers ?: ""}
${drawerUniforms()}
${shadeStructure.attributes ?: ""}
${shadeStructure.uniforms ?: ""}
${shadeStructure.varyingOut ?: ""}
${transformVaryingOut}
out vec3 v_objectPosition;

${vertexMainConstants()}
${shadeStructure.vertexPreamble ?: ""}

flat out int v_instance;

void main() {
    v_instance = gl_InstanceID;

    ${shadeStructure.varyingBridge ?: ""}
    $preVertexTransform
    vec3 x_normal = vec3(0.0, 0.0, 1.0);
    vec3 x_position = a_position.xyz;
    // the position before the model transform
    v_objectPosition = x_position;
    {
${shadeStructure.vertexTransform?.prependIndent("        ") ?: ""}
    }
    $postVertexTransform
    gl_Position = v_clipPosition;
}
"""

    override fun meshLineFragmentShader(shadeStructure: ShadeStructure): String = """
        |
        |${primitiveTypes("d_mesh_line")}
        |${shadeStructure.structDefinitions ?: ""}
        |${shadeStructure.buffers ?: ""}
        |${shadeStructure.outputs ?: ""}
        |${shadeStructure.uniforms ?: ""}
        |
        |uniform sampler2D image;
        |${shadeStructure.fragmentPreamble ?: ""}
        |${drawerUniforms()}
        |${shadeStructure.varyingIn ?: ""}
        |$transformVaryingIn
        |in vec3 v_objectPosition;
        |flat in int v_instance;
        |$meshLineUniforms
        |flat in vec4 v_meshLineSegment;
        |flat in vec4 v_meshLineWidths;
        |flat in vec4 v_meshLineStartJoin;
        |flat in vec4 v_meshLineStartShape;
        |flat in vec4 v_meshLineEndJoin;
        |flat in vec4 v_meshLineEndShape;
        |#ifndef OUTPUT_color
        |${if (!shadeStructure.suppressDefaultOutput) "out vec4 o_color;" else ""}
        |#endif
        |   ${fragmentMainConstants()}
        |
        |// the signed distance in pixels of x to the outline at the end j of the segment, outward points away from the
        |// segment and normal is the normal of the segment; returns false when the segment does not own x
        |bool meshLineEnd(vec2 x, vec2 j, vec2 outward, vec2 normal, float hw, vec4 join, vec4 shape, bool isEnd, out float distance) {
        |    float beyond = dot(x - j, outward);
        |    float roundDistance = beyond > 0.0 ? length(x - j) - hw : -1e20;
        |    if (shape.x < 0.5) {
        |        // a cap
        |        distance = u_meshLineCap == 0 ? roundDistance : (u_meshLineCap == 1 ? beyond : beyond - hw);
        |        return true;
        |    }
        |    if (shape.x > 1.5) {
        |        // a butt or round end
        |        distance = shape.x < 2.5 ? beyond : roundDistance;
        |        return true;
        |    }
        |    distance = u_meshLineJoin == 2 ? roundDistance : dot(x - j, join.zw) - shape.z;
        |    if (shape.w > 0.5 && dot(x - j, normal) * shape.y > 0.0) {
        |        // the inner side of a sharp turn: the segments overlap up to their ends
        |        return beyond <= 0.0;
        |    }
        |    // the segments meet at the bisector of the join, the segment before the join owns the pixels before it
        |    float s = dot(x - j, join.xy);
        |    return isEnd ? s < 0.0 : s >= 0.0;
        |}
        |
        |void main(void) {
        |   vec2 ml_x = gl_FragCoord.xy;
        |#if defined(OR_MESH_LINE_PER_SAMPLE) && ((defined(OR_GL) && __VERSION__ >= 400) || (defined(OR_GLES) && __VERSION__ >= 320))
        |   // using gl_SamplePosition runs the shader for every sample, the outline is cut out at every sample position
        |   ml_x = floor(gl_FragCoord.xy) + gl_SamplePosition;
        |#endif
        |   vec2 ml_start = v_meshLineSegment.xy;
        |   vec2 ml_end = v_meshLineSegment.zw;
        |   float ml_length = length(ml_end - ml_start);
        |   vec2 ml_direction = ml_length > 1e-4 ? (ml_end - ml_start) / ml_length : vec2(1.0, 0.0);
        |   vec2 ml_normal = vec2(-ml_direction.y, ml_direction.x);
        |   float ml_along = ml_length > 1e-4 ? clamp(dot(ml_x - ml_start, ml_direction) / ml_length, 0.0, 1.0) : 0.0;
        |   float ml_lateral = abs(dot(ml_x - ml_start, ml_normal)) - mix(v_meshLineWidths.x, v_meshLineWidths.y, ml_along);
        |   float ml_startDistance;
        |   float ml_endDistance;
        |   bool ml_ownsStart = meshLineEnd(ml_x, ml_start, -ml_direction, ml_normal, v_meshLineWidths.x, v_meshLineStartJoin, v_meshLineStartShape, false, ml_startDistance);
        |   bool ml_ownsEnd = meshLineEnd(ml_x, ml_end, ml_direction, ml_normal, v_meshLineWidths.y, v_meshLineEndJoin, v_meshLineEndShape, true, ml_endDistance);
        |   if (!ml_ownsStart || !ml_ownsEnd) {
        |       discard;
        |   }
        |   float ml_distance = max(ml_lateral, max(ml_startDistance, ml_endDistance));
        |   float ml_coverage = u_smooth > 0.5 ? clamp(0.5 - ml_distance, 0.0, 1.0) : (ml_distance <= 0.0 ? 1.0 : 0.0);
        |   // lines thinner than a pixel are drawn a pixel wide and fade with their width
        |   ml_coverage *= clamp(2.0 * mix(v_meshLineWidths.z, v_meshLineWidths.w, ml_along), 0.0, 1.0);
        |   if (ml_coverage <= 0.0 || ml_coverage < u_meshLineCoverageThreshold) {
        |       discard;
        |   }
        |   vec4 x_fill = u_fill;
        |   vec4 x_stroke = va_color;
        |   {
${shadeStructure.fragmentTransform?.prependIndent("        ") ?: ""}
        |   }
        |${
        if (!shadeStructure.suppressDefaultOutput) """
            |o_color = x_stroke;
            |o_color.rgb *= o_color.a;
            |o_color *= ml_coverage;
            """.trimMargin() else ""
    }
        |}
        """.trimMargin()

    override fun meshLineVertexShader(shadeStructure: ShadeStructure): String = """
        |
        |${shadeStructure.structDefinitions ?: ""}
        |${shadeStructure.buffers ?: ""}
        |${primitiveTypes("d_mesh_line")}
        |${drawerUniforms()}
        |${shadeStructure.attributes ?: ""}
        |${shadeStructure.uniforms ?: ""}
        |${shadeStructure.varyingOut ?: ""}
        |$transformVaryingOut
        |out vec3 v_objectPosition;
        |${vertexMainConstants(element = "int(a_element)")}
        |${shadeStructure.vertexPreamble ?: ""}
        |flat out int v_instance;
        |$meshLineUniforms
        |flat out vec4 v_meshLineSegment;
        |flat out vec4 v_meshLineWidths;
        |flat out vec4 v_meshLineStartJoin;
        |flat out vec4 v_meshLineStartShape;
        |flat out vec4 v_meshLineEndJoin;
        |flat out vec4 v_meshLineEndShape;
        |
        |vec2 meshLinePixels(vec4 c, vec2 viewport) {
        |    return (c.xy / c.w * 0.5 + 0.5) * viewport;
        |}
        |
        |// moves the end points of the segment from a to b that lie behind the near plane onto it, returns false when
        |// the whole segment lies behind it
        |bool meshLineNearClip(inout vec4 a, inout vec4 b) {
        |    float da = a.z + a.w;
        |    float db = b.z + b.w;
        |    if (da < 0.0 && db < 0.0) {
        |        return false;
        |    }
        |    if (da < 0.0) {
        |        a = mix(a, b, da / (da - db));
        |    } else if (db < 0.0) {
        |        b = mix(a, b, da / (da - db));
        |    }
        |    return true;
        |}
        |
        |// the join at j between the segment from p to j and the segment from j to n, in pixels, hw is the half width at j.
        |// join.xy is the direction of the line, the segment before j owns the pixels x with dot(x - j, join.xy) < 0,
        |// join.zw is the outward miter direction. shape is (1 for a join, 2 for a butt end, 3 for a round end; the inner side;
        |// the distance of the miter clip line; 1 when the inner side falls back to overlapping segments).
        |// Both segments compute the join from the same values, they agree on it. Returns the reach of the join along the segments.
        |float meshLineJoin(vec2 p, vec2 j, vec2 n, float hw, out vec4 join, out vec4 shape) {
        |    vec2 e1 = j - p;
        |    vec2 e2 = n - j;
        |    float l1 = length(e1);
        |    float l2 = length(e2);
        |    if (l1 < 1e-4 || l2 < 1e-4) {
        |        vec2 t = l1 >= 1e-4 ? e1 / l1 : (l2 >= 1e-4 ? e2 / l2 : vec2(1.0, 0.0));
        |        join = vec4(t, -t.y, t.x);
        |        shape = vec4(1.0, 1.0, 1e20, 0.0);
        |        return hw;
        |    }
        |    vec2 d1 = e1 / l1;
        |    vec2 d2 = e2 / l2;
        |    if (dot(d1, d2) < -0.9999) {
        |        // the line turns back, the segments end
        |        join = vec4(d1, -d1.y, d1.x);
        |        shape = vec4(u_meshLineJoin == 2 ? 3.0 : 2.0, 1.0, 1e20, 0.0);
        |        return hw;
        |    }
        |    vec2 n1 = vec2(-d1.y, d1.x);
        |    vec2 n2 = vec2(-d2.y, d2.x);
        |    vec2 m = normalize(n1 + n2);
        |    float cosHalf = max(dot(n1, m), 1e-4);
        |    float tanHalf = sqrt(max(1.0 - cosHalf * cosHalf, 0.0)) / cosHalf;
        |    float side = d1.x * d2.y - d1.y * d2.x > 0.0 ? 1.0 : -1.0;
        |    // the inner corner lies a distance hw * tanHalf before and after the joint
        |    float inner = hw * tanHalf;
        |    float fallback = inner > min(l1, l2) ? 1.0 : 0.0;
        |    float clip = 1e20;
        |    float outer = hw * tanHalf;
        |    bool overLimit = 1.0 / cosHalf > u_miterLimit;
        |    if (u_meshLineJoin == 1 || (u_meshLineJoin == 0 && overLimit)) {
        |        clip = hw * cosHalf;
        |        outer = hw;
        |    } else if (u_meshLineJoin == 3 && overLimit) {
        |        clip = u_miterLimit * hw;
        |        outer = clip;
        |    } else if (u_meshLineJoin == 2) {
        |        outer = hw;
        |    }
        |    join = vec4(normalize(d1 + d2), -m * side);
        |    shape = vec4(1.0, side, clip, fallback);
        |    return max(hw, max(outer, fallback > 0.5 ? 0.0 : inner));
        |}
        |
        |void main() {
        |   v_instance = gl_InstanceID;
        |   ${shadeStructure.varyingBridge ?: ""}
        |   $preVertexTransform
        |   vec3 x_normal = vec3(0.0, 0.0, 1.0);
        |   vec3 x_position = a_corner.x < 0.5 ? a_position : a_end;
        |   {
${shadeStructure.vertexTransform?.prependIndent("        ") ?: ""}
        |   }
        |   $postVertexTransform
        |   vec2 viewport = u_viewDimensions * u_contentScale;
        |   mat4 pvm = x_projectionMatrix * x_viewMatrix * x_modelMatrix;
        |   vec4 cPrevious = pvm * vec4(a_previous, 1.0);
        |   vec4 cStart = pvm * vec4(a_position, 1.0);
        |   vec4 cEnd = pvm * vec4(a_end, 1.0);
        |   vec4 cNext = pvm * vec4(a_next, 1.0);
        |   bool startInFront = cStart.z + cStart.w >= 0.0;
        |   bool endInFront = cEnd.z + cEnd.w >= 0.0;
        |   vec4 s = cStart;
        |   vec4 e = cEnd;
        |   if (!meshLineNearClip(s, e)) {
        |       gl_Position = vec4(0.0);
        |       v_clipPosition = gl_Position;
        |       return;
        |   }
        |   vec2 S = meshLinePixels(s, viewport);
        |   vec2 E = meshLinePixels(e, viewport);
        |   // perspective widths are the stroke weight in pixels at w = 1, screen widths at any w
        |   bool screenWidth = u_meshLineWidthMode == 1;
        |   float rawStart = a_width * u_contentScale * 0.5 / (screenWidth ? 1.0 : max(s.w, 1e-6));
        |   float rawEnd = a_width * u_contentScale * 0.5 / (screenWidth ? 1.0 : max(e.w, 1e-6));
        |   float hwStart = max(rawStart, 0.5);
        |   float hwEnd = max(rawEnd, 0.5);
        |   float capReach = u_meshLineCap == 1 ? 0.0 : 1.0;
        |
        |   vec4 startJoin = vec4(0.0);
        |   vec4 startShape;
        |   float reachStart;
        |   if (!startInFront) {
        |       // the near plane cuts the segment
        |       startShape = vec4(2.0, 1.0, 1e20, 0.0);
        |       reachStart = 0.0;
        |   } else if (a_previous == a_position) {
        |       startShape = vec4(0.0, 1.0, 1e20, 0.0);
        |       reachStart = capReach * hwStart;
        |   } else {
        |       vec4 p = cPrevious;
        |       vec4 j = cStart;
        |       meshLineNearClip(p, j);
        |       reachStart = meshLineJoin(meshLinePixels(p, viewport), S, E, hwStart, startJoin, startShape);
        |   }
        |
        |   vec4 endJoin = vec4(0.0);
        |   vec4 endShape;
        |   float reachEnd;
        |   if (!endInFront) {
        |       endShape = vec4(2.0, 1.0, 1e20, 0.0);
        |       reachEnd = 0.0;
        |   } else if (a_next == a_end) {
        |       endShape = vec4(0.0, 1.0, 1e20, 0.0);
        |       reachEnd = capReach * hwEnd;
        |   } else {
        |       vec4 j = cEnd;
        |       vec4 n = cNext;
        |       meshLineNearClip(j, n);
        |       reachEnd = meshLineJoin(S, E, meshLinePixels(n, viewport), hwEnd, endJoin, endShape);
        |   }
        |
        |   v_meshLineSegment = vec4(S, E);
        |   v_meshLineWidths = vec4(hwStart, hwEnd, rawStart, rawEnd);
        |   v_meshLineStartJoin = startJoin;
        |   v_meshLineStartShape = startShape;
        |   v_meshLineEndJoin = endJoin;
        |   v_meshLineEndShape = endShape;
        |
        |   // a quad around the segment, its joins and caps, with a pixel to spare for anti-aliasing
        |   float segmentLength = length(E - S);
        |   vec2 direction = segmentLength > 1e-4 ? (E - S) / segmentLength : vec2(1.0, 0.0);
        |   vec2 normal = vec2(-direction.y, direction.x);
        |   float halfWidth = max(hwStart, hwEnd) + 1.0;
        |   bool atStart = a_corner.x < 0.5;
        |   float along = atStart ? -(reachStart + 1.0) : reachEnd + 1.0;
        |   vec2 pixel = (atStart ? S : E) + direction * along + normal * a_corner.y * halfWidth;
        |   vec4 c = atStart ? s : e;
        |   gl_Position = vec4((pixel / viewport * 2.0 - 1.0) * c.w, c.z, c.w);
        |   v_clipPosition = gl_Position;
        |
        |   // the attributes of the corner of the quad
        |   float t = (atStart ? 0.0 : 1.0) + (segmentLength > 1e-4 ? along / segmentLength : 0.0);
        |   va_position = mix(a_position, a_end, t);
        |   // the position on the segment before the model transform, also on the caps past its ends
        |   v_objectPosition = va_position;
        |   va_texCoord0 = vec2(0.5 + 0.5 * a_corner.y * halfWidth / max(hwStart, hwEnd), mix(a_texCoord0.x, a_texCoord0.y, t));
        |}
        """.trimMargin()

    override fun filterVertexShader(shadeStructure: ShadeStructure): String = """
        |// -- ShaderGeneratorsGL3.filterVertexShader
        |${shadeStructure.structDefinitions ?: ""}
        |${shadeStructure.buffers ?: ""}
        |
        |in vec2 a_texCoord0;
        |in vec2 a_position;
        |// -- vertexPreamble
        |${shadeStructure.vertexPreamble ?: ""}
        |uniform vec2 targetSize;
        |uniform vec2 padding;
        |uniform mat4 projectionMatrix;
        |out vec2 v_texCoord0;
        |void main() {
        |   v_texCoord0 = a_texCoord0;
        |   vec2 transformed = a_position * (targetSize - 2.0 * padding) + padding;
        |   gl_Position = projectionMatrix * vec4(transformed, 0.0, 1.0);
${shadeStructure.vertexTransform?.prependIndent("        ") ?: ""}
        |}
    """.trimMargin()

    override fun filterFragmentShader(shadeStructure: ShadeStructure): String = """
        |// -- ShaderGeneratorsGL3.filterFragmentShader
        |${shadeStructure.structDefinitions ?: ""}
        |${shadeStructure.buffers ?: ""}
        |
        |in vec2 v_texCoord0;
        |uniform sampler2D tex0;
        |uniform sampler2D tex1;
        |uniform sampler2D tex2;
        |uniform sampler2D tex3;
        |uniform sampler2D tex4;
        |uniform sampler2D tex5;
        |uniform sampler2D tex6;
        |uniform sampler2D tex7;
        |uniform vec2 targetSize;
        |// -- drawerUniforms
        |${drawerUniforms()}
        |// -- shadeStructure.outputs
        |${shadeStructure.outputs ?: ""}
        |#ifndef OUTPUT_color
        |${if (!shadeStructure.suppressDefaultOutput) "out vec4 o_color;" else ""}
        |#endif
        |// -- shadeStructure.uniforms
        |${shadeStructure.uniforms ?: ""}
        |// -- shadeStructure.fragmentPreamble
        |${
        fragmentMainConstants(
            instance = "0",
            screenPosition = "v_texCoord0",
            boundsPosition = "vec3(v_texCoord0, 0.0)",
            boundsSize = "vec3(targetSize, 0.0)",
            textureCoordinate = "v_texCoord0"
        )
    }
        |${shadeStructure.fragmentPreamble ?: ""}
        |void main() {
        |   // inputs hold premultiplied colors, x_fill is straight like in the image shaders, it is premultiplied on output
        |   vec4 x_fill = texture(tex0, v_texCoord0);
        |   if (x_fill.a != 0.0) {
        |       x_fill.rgb /= x_fill.a;
        |   }
        |   vec4 x_stroke = vec4(0.0);
        |   {
        |       // -- shadeStructure.fragmentTransform
${shadeStructure.fragmentTransform?.prependIndent("        ") ?: ""}
        |   }
        |${
        if (!shadeStructure.suppressDefaultOutput) """
            |o_color = x_fill;
            |o_color.rgb *= o_color.a;
            """.trimMargin() else ""
    }
        |}
        |

    """.trimMargin()

    override fun slugVertexShader(shadeStructure: ShadeStructure): String = """
// -- slug vertex shader
${primitiveTypes("d_slug")}
${shadeStructure.structDefinitions ?: ""}
${shadeStructure.buffers ?: ""}
${drawerUniforms()}
${shadeStructure.attributes ?: ""}
${shadeStructure.uniforms ?: ""}
${shadeStructure.varyingOut ?: ""}
${transformVaryingOut}
${slugDilate}
${shadeStructure.vertexPreamble ?: ""}

flat out int v_instance;
out vec2 v_dilatedTexCoord;
// the position in the coordinates of the text, before the model matrix, like the expansion shaders
out vec3 v_objectPosition;

void main() {
    v_instance = gl_InstanceID;
${vertexMainConstants()}
${shadeStructure.varyingBridge ?: ""}
    ${preVertexTransform}
    vec3 x_normal = vec3(0.0, 0.0, 1.0);
    vec3 x_position;
    {
        // dilate the glyph quad by half a pixel (plus stroke) in viewport space
        vec2 dims = i_slugBounds.zw - i_slugBounds.xy;
        vec3 boundsPosition = vec3(mix(i_slugBounds.xy, i_slugBounds.zw, a_texCoord0.xy), 0.0);
        vec2 quadNormal = (a_texCoord0.xy - vec2(0.5)) * 2.0;
        vec4 pos = vec4(boundsPosition.xy, quadNormal);

        // maps the object-space dilation vector to normalized texture coordinates
        vec4 jac = vec4(1.0 / dims.x, 0.0, 0.0, 1.0 / dims.y);

        mat4 mvpt = transpose(u_projectionMatrix * u_viewMatrix * u_modelMatrix * i_transform);
        vec2 viewportPixels = u_viewDimensions * u_contentScale;

        vec2 dilatedPosition;
        v_dilatedTexCoord = SlugDilate(pos, a_texCoord0, jac, mvpt[0], mvpt[1], mvpt[3], viewportPixels, dilatedPosition);
        x_position = (i_transform * vec4(dilatedPosition, 0.0, 1.0)).xyz;
    }
    v_objectPosition = x_position;
    {
${shadeStructure.vertexTransform?.prependIndent("        ") ?: ""}
    }
    ${postVertexTransform}
    gl_Position = v_clipPosition;
}
"""

    override fun slugFragmentShader(shadeStructure: ShadeStructure): String = """
// -- slug fragment shader
${primitiveTypes("d_slug")}
${shadeStructure.structDefinitions ?: ""}
${shadeStructure.buffers ?: ""}
${shadeStructure.uniforms ?: ""}

uniform sampler2D u_slugCurves;
uniform usampler2D u_slugBands;
uniform float u_slugContrast;
// 0: the stroke is painted over the fill, 1: the fill over the stroke
uniform int u_paintOrder;
uniform float u_slugMinification;

${drawerUniforms()}
${shadeStructure.varyingIn ?: ""}
${shadeStructure.outputs ?: ""}
${transformVaryingIn}

#ifndef OUTPUT_color
${if (!shadeStructure.suppressDefaultOutput) "out vec4 o_color;" else ""}
#endif

flat in int v_instance;
in vec2 v_dilatedTexCoord;
in vec3 v_objectPosition;

${slugBezierDistance}
${slugCoverageFunctions}
${strokeEdgeFunction}
${slugContrastFunctions}
${slugFetch}

${
        fragmentMainConstants(
            instance = "v_instance",
            boundsPosition = "vec3(v_dilatedTexCoord, 0.0)",
            boundsSize = "vec3(vi_slugBounds.zw - vi_slugBounds.xy, 0.0)"
        )
    }
${shadeStructure.fragmentPreamble ?: ""}

void main(void) {
    // -- slug coverage, defines slugCoverage and slugStrokeCoverage
${slugCoverage.prependIndent("    ")}

    vec4 x_fill = vi_fill;
    vec4 x_stroke = vi_stroke;
    {
${shadeStructure.fragmentTransform?.prependIndent("        ") ?: ""}
    }
    float fillCoverage = slugContrastCoverage(slugCoverage, x_fill.rgb, u_slugContrast);
    float strokeCoverage = slugContrastCoverage(slugStrokeCoverage, x_stroke.rgb, u_slugContrast);
    // the stroke over the fill, or the fill over the stroke by the paint order, composited premultiplied, final holds
    // the straight-alpha result
    float fillAlpha = x_fill.a * fillCoverage;
    float strokeAlpha = x_stroke.a * strokeCoverage;
    vec4 premultiplied = u_paintOrder == 1
        ? vec4(x_fill.rgb * fillAlpha, fillAlpha) + vec4(x_stroke.rgb * strokeAlpha, strokeAlpha) * (1.0 - fillAlpha)
        : vec4(x_stroke.rgb * strokeAlpha, strokeAlpha) + vec4(x_fill.rgb * fillAlpha, fillAlpha) * (1.0 - strokeAlpha);
    vec4 final = vec4(premultiplied.rgb / max(premultiplied.a, 1e-6), premultiplied.a);
    ${
        if (!shadeStructure.suppressDefaultOutput) """
    o_color = final;
    o_color.rgb *= o_color.a;
    """ else ""
    }
}
"""

    override fun windfoilVertexShader(shadeStructure: ShadeStructure): String = """
// -- windfoil vertex shader
${primitiveTypes("d_windfoil")}
${shadeStructure.structDefinitions ?: ""}
${shadeStructure.buffers ?: ""}
${drawerUniforms()}
${shadeStructure.attributes ?: ""}
${shadeStructure.uniforms ?: ""}
${shadeStructure.varyingOut ?: ""}
${transformVaryingOut}
${windfoilDilate}
${shadeStructure.vertexPreamble ?: ""}

flat out int v_instance;
out vec2 v_dilatedTexCoord;
// the position in the coordinates of the text, before the model matrix, like the expansion shaders
out vec3 v_objectPosition;

void main() {
    v_instance = gl_InstanceID;
${vertexMainConstants()}
${shadeStructure.varyingBridge ?: ""}
    ${preVertexTransform}
    vec3 x_normal = vec3(0.0, 0.0, 1.0);
    vec3 x_position;
    {
        // dilate the shape quad in viewport space, plus the stroke outset
        vec2 dims = max(i_windfoilBounds.zw - i_windfoilBounds.xy, vec2(1e-6));
        vec3 boundsPosition = vec3(mix(i_windfoilBounds.xy, i_windfoilBounds.zw, a_texCoord0.xy), 0.0);
        vec2 quadNormal = (a_texCoord0.xy - vec2(0.5)) * 2.0;
        vec4 pos = vec4(boundsPosition.xy, quadNormal);

        // maps the object-space dilation vector to normalized texture coordinates
        vec4 jac = vec4(1.0 / dims.x, 0.0, 0.0, 1.0 / dims.y);

        mat4 mvpt = transpose(u_projectionMatrix * u_viewMatrix * u_modelMatrix * i_transform);
        vec2 viewportPixels = u_viewDimensions * u_contentScale;

        vec2 dilatedPosition;
        v_dilatedTexCoord = WindfoilDilate(pos, a_texCoord0, jac, mvpt[0], mvpt[1], mvpt[3], viewportPixels, dilatedPosition);
        x_position = (i_transform * vec4(dilatedPosition, 0.0, 1.0)).xyz;
    }
    v_objectPosition = x_position;
    {
${shadeStructure.vertexTransform?.prependIndent("        ") ?: ""}
    }
    ${postVertexTransform}
    gl_Position = v_clipPosition;
}
"""

    override fun windfoilFragmentShader(shadeStructure: ShadeStructure): String = """
// -- windfoil fragment shader
${primitiveTypes("d_windfoil")}
${shadeStructure.structDefinitions ?: ""}
${shadeStructure.buffers ?: ""}
${shadeStructure.uniforms ?: ""}

uniform sampler2D u_windfoilCurves;
uniform usampler2D u_windfoilRows;
uniform float u_windfoilContrast;
// 0: the stroke is painted over the fill, 1: the fill over the stroke
uniform int u_paintOrder;
uniform float u_windfoilMinification;
uniform int u_windfoilExactSamples;

${drawerUniforms()}
${shadeStructure.varyingIn ?: ""}
${shadeStructure.outputs ?: ""}
${transformVaryingIn}

#ifndef OUTPUT_color
${if (!shadeStructure.suppressDefaultOutput) "out vec4 o_color;" else ""}
#endif

flat in int v_instance;
in vec2 v_dilatedTexCoord;
in vec3 v_objectPosition;

${slugBezierDistance}
${slugContrastFunctions}
${strokeEdgeFunction}
${windfoilFunctions}

${
        fragmentMainConstants(
            instance = "v_instance",
            boundsPosition = "vec3(v_dilatedTexCoord, 0.0)",
            boundsSize = "vec3(vi_windfoilBounds.zw - vi_windfoilBounds.xy, 0.0)"
        )
    }
${shadeStructure.fragmentPreamble ?: ""}

void main(void) {
    // the position of the fragment in shape space and the size of its pixel box
    vec2 rc = mix(vi_windfoilBounds.xy, vi_windfoilBounds.zw, v_dilatedTexCoord);
    vec2 s = max(fwidth(rc), vec2(1e-9));

    bool hasStroke = vi_stroke.a > 0.0 && vi_strokeWeight > 0.0;
    bool needsCoverage = vi_fill.a > 0.0 || (hasStroke && vi_strokeMode != 0);

    float windfoilCoverage = 0.0;
    if (needsCoverage) {
        if (u_windfoilExactSamples > 0) {
            windfoilCoverage = windfoilExactCoverage(vi_rowBase, vi_bandCount, vi_bandScale, vi_windfoilBounds.y,
                vi_fillRule, rc, s, u_windfoilExactSamples);
        } else {
            // shapes up to u_windfoilMinification pixels are drawn from the ink profiles of their bands, up to 1.5
            // times that size the profile is blended with the winding integral
            vec2 sizePixels = (vi_windfoilBounds.zw - vi_windfoilBounds.xy) / s;
            float integralWeight = u_windfoilMinification > 0.0 && !hasStroke
                ? smoothstep(u_windfoilMinification, u_windfoilMinification * 1.5, max(sizePixels.x, sizePixels.y))
                : 1.0;
            float profileCoverage = 0.0;
            float integralCoverage = 0.0;
            if (integralWeight < 1.0) {
                float f = windfoilProfileFace(vi_rowBase, vi_bandCount, vi_bandScale, vi_windfoilBounds, rc, s);
                profileCoverage = windfoilFold(f / (s.x * s.y), vi_fillRule);
            }
            if (integralWeight > 0.0) {
                float f = windfoilIntegrateFace(vi_rowBase, vi_bandCount, vi_bandScale, vi_windfoilBounds.y, rc, s);
                integralCoverage = windfoilFold(f / (s.x * s.y), vi_fillRule);
            }
            windfoilCoverage = mix(profileCoverage, integralCoverage, integralWeight);
        }
    }

    float windfoilStrokeCoverage = 0.0;
    if (hasStroke) {
        // pieces within two pixels beyond the stroke edge are found, so the anti-aliased edge and its neighbours see
        // their distances, and distances beyond reach are capped so the derivative stays small
        float reach = (vi_strokeMode == 0 ? 0.5 : 1.0) * vi_strokeWeight + 2.0 * max(s.x, s.y);
        float minDist = min(windfoilDistance(vi_rowBase, vi_bandCount, vi_bandScale, vi_windfoilBounds.y, rc, reach), reach);
        // half a pixel on each side of the stroke edge
        float w = 0.5 * fwidth(minDist);
        if (vi_strokeMode == 0) { // CENTER
            windfoilStrokeCoverage = strokeEdgeCoverage(vi_strokeWeight / 2.0, minDist, w);
        } else if (vi_strokeMode == 1) { // INSIDE
            windfoilStrokeCoverage = strokeEdgeCoverage(vi_strokeWeight, minDist, w) * windfoilCoverage;
        } else if (vi_strokeMode == 2) { // OUTSIDE
            windfoilStrokeCoverage = strokeEdgeCoverage(vi_strokeWeight, minDist, w) * (1.0 - windfoilCoverage);
        } else if (vi_strokeMode == 3) { // ERODE
            float erosion = strokeEdgeCoverage(vi_strokeWeight, minDist, w);
            windfoilCoverage *= (1.0 - erosion);
        }
    }

    vec4 x_fill = vi_fill;
    vec4 x_stroke = vi_stroke;
    {
${shadeStructure.fragmentTransform?.prependIndent("        ") ?: ""}
    }
    float fillCoverage = slugContrastCoverage(windfoilCoverage, x_fill.rgb, u_windfoilContrast);
    float strokeCoverage = slugContrastCoverage(windfoilStrokeCoverage, x_stroke.rgb, u_windfoilContrast);
    // the stroke over the fill, or the fill over the stroke by the paint order, composited premultiplied, final holds
    // the straight-alpha result
    float fillAlpha = x_fill.a * fillCoverage;
    float strokeAlpha = x_stroke.a * strokeCoverage;
    vec4 premultiplied = u_paintOrder == 1
        ? vec4(x_fill.rgb * fillAlpha, fillAlpha) + vec4(x_stroke.rgb * strokeAlpha, strokeAlpha) * (1.0 - fillAlpha)
        : vec4(x_stroke.rgb * strokeAlpha, strokeAlpha) + vec4(x_fill.rgb * fillAlpha, fillAlpha) * (1.0 - strokeAlpha);
    vec4 final = vec4(premultiplied.rgb / max(premultiplied.a, 1e-6), premultiplied.a);
    ${
        if (!shadeStructure.suppressDefaultOutput) """
    o_color = final;
    o_color.rgb *= o_color.a;
    """ else ""
    }
}
"""
}
