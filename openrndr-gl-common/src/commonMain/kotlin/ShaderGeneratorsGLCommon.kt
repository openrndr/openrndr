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
void main() {
    int instance = gl_InstanceID; // this will go use c_instance instead
${vertexMainConstants()}
${shadeStructure.varyingBridge ?: ""}
    vec3 x_normal = vec3(0.0, 0.0, 0.0);
    ${if (shadeStructure.attributes?.contains("vec3 a_normal;") == true) "x_normal = a_normal;" else ""}
    vec3 x_position = a_position.xyz;
    float x_pointSize = 1.0;

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
${
        fragmentMainConstants(
            boundsPosition = "vec3(va_texCoord0, 0.0)", boundsSize = "v_boundsSize"
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

    // the stroke is painted over the fill
    float sa = strokeCoverage * x_stroke.a;
    float fa = fillCoverage * x_fill.a;
    vec4 final = vec4(0.0);
    final.rgb = x_stroke.rgb * sa + x_fill.rgb * fa * (1.0 - sa);
    final.a = sa + fa * (1.0 - sa);
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
// 1.0 for anti-aliased edges, 0.0 for hard edges
uniform float u_smooth;

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

    // the stroke is painted over the fill
    float sa = strokeCoverage * x_stroke.a;
    float fa = fillCoverage * x_fill.a;
    vec4 final = vec4(0.0);
    final.rgb = x_stroke.rgb * sa + x_fill.rgb * fa * (1.0 - sa);
    final.a = sa + fa * (1.0 - sa);

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

in vec3 v_objectPosition;
in vec2 v_ftcoord;

#ifndef OUTPUT_color
${if (!shadeStructure.suppressDefaultOutput) "out vec4 o_color;" else ""}
#endif

${shadeStructure.fragmentPreamble ?: ""}

float strokeMask() {
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

${vertexMainConstants()}
${shadeStructure.vertexPreamble ?: ""}

flat out int v_instance;

void main() {
    v_instance = gl_InstanceID;

    ${shadeStructure.varyingBridge ?: ""}
    $preVertexTransform
    vec3 x_normal = vec3(0.0, 0.0, 1.0);
    vec3 x_position = a_position.xyz;
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
        |flat in int v_instance;
        |#ifndef OUTPUT_color
        |${if (!shadeStructure.suppressDefaultOutput) "out vec4 o_color;" else ""}
        |#endif
        |   ${fragmentMainConstants()}
        |void main(void) {
        |   vec4 x_fill = u_fill;
        |   vec4 x_stroke = va_color;
        |   {
${shadeStructure.fragmentTransform?.prependIndent("        ") ?: ""}
        |   }
        |${
        if (!shadeStructure.suppressDefaultOutput) """
            |o_color = x_stroke;
            |o_color.rgb *= o_color.a;
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
        |${vertexMainConstants(element = "int(a_element)")}
        |${shadeStructure.vertexPreamble ?: ""}
        |flat out int v_instance;
        |
        |vec2 fix(vec4 i, float aspect) {
        |   vec2 res = i.xy / i.w;
        |   res.x *= aspect;
        |   return res;
        |}
        |
        |void main() {
        |   v_instance = gl_InstanceID;
        |   ${shadeStructure.varyingBridge ?: ""}
        |   $preVertexTransform
        |   vec3 x_normal = vec3(0.0, 0.0, 1.0);
        |   vec3 x_position = a_position.xyz;
        |   {
${shadeStructure.vertexTransform?.prependIndent("        ") ?: ""}
        |   }
        |   $postVertexTransform
        |   float aspect = u_viewDimensions.x / u_viewDimensions.y;
        |   vec2 pixelWidthRatio = 1.0 / (u_viewDimensions);
        |   mat4 pvm = x_projectionMatrix * x_viewMatrix * x_modelMatrix;
        |   vec4 finalPosition = pvm * vec4(a_position, 1.0);
        |   vec4 prevPosition = pvm * vec4(a_previous, 1.0);
        |   vec4 nextPosition = pvm * vec4(a_next, 1.0);
        |   vec2 currentP = fix(finalPosition, aspect);
        |   vec2 prevP = fix(prevPosition, aspect);
        |   vec2 nextP = fix(nextPosition, aspect);
        |
        |   vec2 w = max(pixelWidthRatio*finalPosition.w, (pixelWidthRatio) * a_width);
        |   vec2 dir;
        |   if (nextP == currentP) {
        |       dir = normalize(currentP - prevP);
        |   } else if(prevP == currentP) {
        |       dir = normalize( nextP - currentP );
        |   } else {
        |       vec2 dir1 = normalize(currentP - prevP);
        |       vec2 dir2 = normalize(nextP - currentP);
        |       dir = normalize(dir1 + dir2);
        |   }
        |   x_normal = ( cross( vec3( dir, 0. ), vec3( 0., 0., 1. ) ) );
        |   vec2 normal = vec2(-dir.y, dir.x) * w;
        |   vec4 offset = vec4(normal * a_side, 0.0, 1.0);
        |
        |   finalPosition.xy += offset.xy;
        |   v_clipPosition = finalPosition;
        |   gl_Position = finalPosition;
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
        |   vec2 transformed = a_position * (targetSize - 2*padding) + padding;
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
            boundsSize = "vec3(targetSize, 0.0)"
        )
    }
        |${shadeStructure.fragmentPreamble ?: ""}
        |void main() {
        |   vec4 x_fill = texture(tex0, v_texCoord0);
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

${slugBezierDistance}
${slugCoverageFunctions}
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
    // the stroke over the fill, composited premultiplied, final holds the straight-alpha result
    float fillAlpha = x_fill.a * fillCoverage;
    float strokeAlpha = x_stroke.a * strokeCoverage;
    vec4 premultiplied = vec4(x_stroke.rgb * strokeAlpha, strokeAlpha) + vec4(x_fill.rgb * fillAlpha, fillAlpha) * (1.0 - strokeAlpha);
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

${slugBezierDistance}
${slugContrastFunctions}
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
            windfoilStrokeCoverage = smoothstep(vi_strokeWeight / 2.0 + w, vi_strokeWeight / 2.0 - w, minDist);
        } else if (vi_strokeMode == 1) { // INSIDE
            windfoilStrokeCoverage = smoothstep(vi_strokeWeight + w, vi_strokeWeight - w, minDist) * windfoilCoverage;
        } else if (vi_strokeMode == 2) { // OUTSIDE
            windfoilStrokeCoverage = smoothstep(vi_strokeWeight + w, vi_strokeWeight - w, minDist) * (1.0 - windfoilCoverage);
        } else if (vi_strokeMode == 3) { // ERODE
            float erosion = smoothstep(vi_strokeWeight + w, vi_strokeWeight - w, minDist);
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
    // the stroke over the fill, composited premultiplied, final holds the straight-alpha result
    float fillAlpha = x_fill.a * fillCoverage;
    float strokeAlpha = x_stroke.a * strokeCoverage;
    vec4 premultiplied = vec4(x_stroke.rgb * strokeAlpha, strokeAlpha) + vec4(x_fill.rgb * fillAlpha, fillAlpha) * (1.0 - strokeAlpha);
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
