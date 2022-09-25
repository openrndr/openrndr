@file:Suppress("RemoveCurlyBracesFromTemplate")

package org.openrndr.internal.gl3

import org.intellij.lang.annotations.Language
import org.openrndr.draw.ShadeStructure
import org.openrndr.draw.ShadeStyleGLSL.Companion.drawerUniforms
import org.openrndr.draw.ShadeStyleGLSL.Companion.fragmentMainConstants
import org.openrndr.draw.ShadeStyleGLSL.Companion.postVertexTransform
import org.openrndr.draw.ShadeStyleGLSL.Companion.preVertexTransform
import org.openrndr.draw.ShadeStyleGLSL.Companion.primitiveTypes
import org.openrndr.draw.ShadeStyleGLSL.Companion.transformVaryingIn
import org.openrndr.draw.ShadeStyleGLSL.Companion.transformVaryingOut
import org.openrndr.draw.ShadeStyleGLSL.Companion.vertexMainConstants
import org.openrndr.internal.Driver
import org.openrndr.internal.ShaderGenerators

fun glslVersion(): String = (Driver.instance as DriverGL3).version.glslVersion

@Language("GLSL")
private val rotate2 = """mat2 rotate2(float rotationInDegrees) {
    float r = radians(rotationInDegrees);
    float cr = cos(r);
    float sr = sin(r);
    return mat2(vec2(cr, sr), vec2(-sr, cr));
}
""".trimIndent()


class ShaderGeneratorsGL3 : ShaderGenerators {
    override fun vertexBufferFragmentShader(shadeStructure: ShadeStructure): String = """|#version ${glslVersion()}
|${primitiveTypes("d_vertex_buffer")}
|${shadeStructure.buffers ?: ""}
|${shadeStructure.uniforms ?: ""}
|layout(origin_upper_left) in vec4 gl_FragCoord;

|uniform sampler2D image;
|${drawerUniforms()}
|${shadeStructure.varyingIn ?: ""}
|${shadeStructure.outputs ?: ""}
|${transformVaryingIn}

|${if (!shadeStructure.suppressDefaultOutput) "out vec4 o_color;" else ""}

|${shadeStructure.fragmentPreamble ?: ""}
|flat in int v_instance;

|void main(void) {
|    ${fragmentMainConstants(element = "v_instance")}
|    vec4 x_fill = u_fill;
|    vec4 x_stroke = u_stroke;
|    {
|       ${shadeStructure.fragmentTransform ?: ""}
|    }
     ${
        if (!shadeStructure.suppressDefaultOutput) """
     |    o_color = x_fill;
     |    o_color.rgb *= o_color.a;
     |""".trimMargin() else ""
    }
|}""".trimMargin()

    override fun vertexBufferVertexShader(shadeStructure: ShadeStructure): String = """#version ${glslVersion()}
${primitiveTypes("d_vertex_buffer")}
${shadeStructure.buffers ?: ""}
${drawerUniforms()}
${shadeStructure.attributes ?: ""}
${shadeStructure.uniforms ?: ""}
${shadeStructure.varyingOut ?: ""}
${transformVaryingOut}
${shadeStructure.vertexPreamble ?: ""}

flat out int v_instance;
void main() {
    int instance = gl_InstanceID; // this will go use c_instance instead
${vertexMainConstants()}
${shadeStructure.varyingBridge ?: ""}
    vec3 x_normal = vec3(0.0, 0.0, 0.0);
    ${if (shadeStructure.attributes?.contains("vec3 a_normal;") == true) "x_normal = a_normal;" else ""}
    vec3 x_position = a_position;

    ${preVertexTransform}
    {
        ${shadeStructure.vertexTransform ?: ""}
    }
    ${postVertexTransform}

    v_instance = instance;
    gl_Position = v_clipPosition;
}
            """.trimMargin()

    override fun imageFragmentShader(shadeStructure: ShadeStructure): String = """#version ${glslVersion()}
${primitiveTypes("d_image")}
${shadeStructure.buffers ?: ""}
${shadeStructure.uniforms ?: ""}
layout(origin_upper_left) in vec4 gl_FragCoord;

uniform sampler2D image;
${drawerUniforms()}
${shadeStructure.varyingIn ?: ""}
${transformVaryingIn}

${shadeStructure.outputs ?: ""}

${if (!shadeStructure.suppressDefaultOutput) "out vec4 o_color;" else ""}

${shadeStructure.fragmentPreamble ?: ""}

in vec3 v_boundsPosition;
flat in int v_instance;
vec4 colorTransform(vec4 color, float[25] matrix) {
    float r = color.r * matrix[0] + color.g * matrix[5] + color.b * matrix[10] + color.a * matrix[15] + matrix[20];
    float g = color.r * matrix[1] + color.g * matrix[6] + color.b * matrix[11] + color.a * matrix[16] + matrix[21];
    float b = color.r * matrix[2] + color.g * matrix[7] + color.b * matrix[12] + color.a * matrix[17] + matrix[22];
    float a = color.r * matrix[3] + color.g * matrix[8] + color.b * matrix[13] + color.a * matrix[18] + matrix[23];
    return vec4(r, g, b, a);
}

void main(void) {
    ${fragmentMainConstants(boundsPosition = "v_boundsPosition")}
    vec4 x_fill = texture(image, va_texCoord0);
    vec4 x_stroke = u_stroke;
    {
        ${shadeStructure.fragmentTransform ?: ""}
    }
    float div = x_fill.a != 0.0 ? x_fill.a : 1.0;
    x_fill.rgb /= div;
    x_fill = colorTransform(x_fill, u_colorMatrix);
    x_fill.rgb *= x_fill.a;
    ${if (!shadeStructure.suppressDefaultOutput) "o_color = x_fill;" else ""}
}"""

    override fun imageVertexShader(shadeStructure: ShadeStructure): String = """
#version ${glslVersion()}
${primitiveTypes("d_image")}
${shadeStructure.buffers ?: ""}
${drawerUniforms()}
uniform int u_flipV;
${shadeStructure.attributes ?: ""}
${shadeStructure.uniforms ?: ""}
${shadeStructure.varyingOut ?: ""}
${transformVaryingOut}

${shadeStructure.vertexPreamble ?: ""}

flat out int v_instance;

out vec3 v_boundsPosition;
void main() {
    v_instance = gl_InstanceID;
    ${vertexMainConstants()}
    ${shadeStructure.varyingBridge ?: ""}
    ${preVertexTransform}
    vec3 x_normal = a_normal;
    vec3 x_position = a_position;
    x_position.xy = a_position.xy * i_target.zw + i_target.xy;
    v_boundsPosition = vec3(a_texCoord0.xy, 1.0);
    va_texCoord0.xy = a_texCoord0.xy * i_source.zw + i_source.xy;
    if (u_flipV == 0) {
        va_texCoord0.y = 1.0 - va_texCoord0.y;
    }
    {
        ${shadeStructure.vertexTransform ?: ""}
    }
    ${postVertexTransform}
    gl_Position = v_clipPosition;
}
"""

    override fun imageArrayTextureFragmentShader(shadeStructure: ShadeStructure): String = """#version ${glslVersion()}
${primitiveTypes("d_image")}
${shadeStructure.buffers ?: ""}
${shadeStructure.uniforms ?: ""}
layout(origin_upper_left) in vec4 gl_FragCoord;

uniform sampler2DArray image;
${drawerUniforms()}
${shadeStructure.varyingIn ?: ""}
${transformVaryingIn}

${if (!shadeStructure.suppressDefaultOutput) "out vec4 o_color;" else ""}

${shadeStructure.fragmentPreamble ?: ""}

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

void main(void) {
    ${fragmentMainConstants(boundsPosition = "v_boundsPosition")}
    vec4 x_fill = texture(image, vec3(va_texCoord0, v_layer*1.0));
    vec4 x_stroke = u_stroke;
    {
        ${shadeStructure.fragmentTransform ?: ""}
    }
    float div = x_fill.a != 0.0 ? x_fill.a : 1.0;
    x_fill.rgb /= div;
    x_fill = colorTransform(x_fill, u_colorMatrix);
    x_fill.rgb *= x_fill.a;
    ${if (!shadeStructure.suppressDefaultOutput) "o_color = x_fill;" else ""}
}"""

    override fun imageArrayTextureVertexShader(shadeStructure: ShadeStructure): String = """
#version ${glslVersion()}
${primitiveTypes("d_image")}
${shadeStructure.buffers ?: ""}
${drawerUniforms()}
uniform int u_flipV;
${shadeStructure.attributes ?: ""}
${shadeStructure.uniforms ?: ""}
${shadeStructure.varyingOut ?: ""}
${transformVaryingOut}

${shadeStructure.vertexPreamble ?: ""}

flat out int v_instance;
flat out int v_layer;

out vec3 v_boundsPosition;
void main() {
    v_instance = gl_InstanceID;
    ${vertexMainConstants()}
    ${shadeStructure.varyingBridge ?: ""}
    ${preVertexTransform}
    vec3 x_normal = a_normal;
    vec3 x_position = a_position;
    x_position.xy = a_position.xy * i_target.zw + i_target.xy;
    v_boundsPosition = vec3(a_texCoord0.xy, 1.0);
    va_texCoord0.xy = a_texCoord0.xy * i_source.zw + i_source.xy;
    v_layer = int(floor(i_layer+0.5));
    if (u_flipV == 0) {
        va_texCoord0.y = 1.0 - va_texCoord0.y;
    }
    {
        ${shadeStructure.vertexTransform ?: ""}
    }
    ${postVertexTransform}
    gl_Position = v_clipPosition;
}
"""

    override fun pointFragmentShader(shadeStructure: ShadeStructure): String = """#version ${glslVersion()}
${primitiveTypes("d_circle")}
${shadeStructure.buffers ?: ""}
${shadeStructure.uniforms ?: ""}
layout(origin_upper_left) in vec4 gl_FragCoord;

${drawerUniforms(styleBlock = false)}
${shadeStructure.varyingIn ?: ""}
${transformVaryingIn}

${if (!shadeStructure.suppressDefaultOutput) "out vec4 o_color;" else ""}

${shadeStructure.fragmentPreamble ?: ""}

flat in int v_instance;
in vec3 v_boundsSize;
void main(void) {
    ${
        fragmentMainConstants(boundsPosition = "vec3(0.0, 0.0, 0.0)",
                boundsSize = "v_boundsSize")
    }

    vec4 x_fill = vi_fill;
    vec4 x_stroke = vi_stroke;
    {
        ${shadeStructure.fragmentTransform ?: ""}
    }
    x_fill.rgb *= x_fill.a;
    ${if (!shadeStructure.suppressDefaultOutput) "o_color = x_fill;" else ""}
}

    """.trimMargin()

    override fun pointVertexShader(shadeStructure: ShadeStructure): String = """#version ${glslVersion()}
${primitiveTypes("d_point")}
${shadeStructure.buffers ?: ""}
${drawerUniforms(styleBlock = false)}
${shadeStructure.attributes ?: ""}
${shadeStructure.uniforms ?: ""}
${shadeStructure.varyingOut ?: ""}
${transformVaryingOut}

${shadeStructure.vertexPreamble ?: ""}

flat out int v_instance;
out vec3 v_boundsSize;
void main() {
    v_instance = gl_InstanceID;
    ${vertexMainConstants()}
    ${shadeStructure.varyingBridge ?: ""}

    v_boundsSize = vec3(0, 0.0, 0.0);
    ${preVertexTransform}
    vec3 x_normal = vec3(0.0, 0.0, 1.0);
    vec3 x_position = a_position  + i_offset;
    {
        ${shadeStructure.vertexTransform ?: ""}
    }
    va_position = x_position;
    ${postVertexTransform}
    gl_Position = v_clipPosition;
}"""


    override fun circleFragmentShader(shadeStructure: ShadeStructure): String = """#version ${glslVersion()}
${primitiveTypes("d_circle")}
${shadeStructure.uniforms ?: ""}
${shadeStructure.buffers ?: ""}
layout(origin_upper_left) in vec4 gl_FragCoord;

${drawerUniforms(styleBlock = false)}
${shadeStructure.varyingIn ?: ""}
${transformVaryingIn}

${if (!shadeStructure.suppressDefaultOutput) "out vec4 o_color;" else ""}

${shadeStructure.fragmentPreamble ?: ""}

flat in int v_instance;
in vec3 v_boundsSize;
void main(void) {
    ${
        fragmentMainConstants(boundsPosition = "vec3(va_texCoord0, 0.0)",
                boundsSize = "v_boundsSize")
    }
    float smoothFactor = 3.0;

    vec4 x_fill = vi_fill;
    vec4 x_stroke = vi_stroke;
    float x_strokeWeight = vi_strokeWeight;
    
    {
        ${shadeStructure.fragmentTransform ?: ""}
    }
    float wd = fwidth(length(va_texCoord0 - vec2(0.0)));
    float d = length(va_texCoord0 - vec2(0.5)) * 2;

    float or = smoothstep(0, wd * smoothFactor, 1.0 - d);
    float b = x_strokeWeight / vi_radius.x;
    float ir = smoothstep(0, wd * smoothFactor, 1.0 - b - d);

    vec4 final = vec4(0.0);
    final.rgb =  x_stroke.rgb;
    final.a = or * (1.0 - ir) * x_stroke.a;
    final.rgb *= final.a;

    final.rgb += x_fill.rgb * ir * x_fill.a;
    final.a += ir * x_fill.a;
    ${if (!shadeStructure.suppressDefaultOutput) "o_color = final;" else ""}
}
"""

    override fun circleVertexShader(shadeStructure: ShadeStructure): String = """#version ${glslVersion()}
// -- circle vertex shader        
${primitiveTypes("d_circle")}
${shadeStructure.buffers ?: ""}
${drawerUniforms(styleBlock = false)}
${shadeStructure.attributes ?: ""}
${shadeStructure.uniforms ?: ""}
${shadeStructure.varyingOut ?: ""}
${transformVaryingOut}

${shadeStructure.vertexPreamble ?: ""}

flat out int v_instance;
out vec3 v_boundsSize;
void main() {
    v_instance = gl_InstanceID;
    ${vertexMainConstants()}
    ${shadeStructure.varyingBridge ?: ""}

    vec2 effectiveRadius = i_radius.xy + vec2(1.25) / (u_modelViewScalingFactor);

    v_boundsSize = vec3(effectiveRadius.xy, 0.0);
    ${preVertexTransform}
    vec3 x_normal = a_normal;
    vec3 x_position = vec3(a_position.xy * effectiveRadius, 0.0) + i_offset;
    {
        ${shadeStructure.vertexTransform ?: ""}
    }
    va_position = x_position;
    ${postVertexTransform}
    gl_Position = v_clipPosition;

}
    """

    override fun fontImageMapFragmentShader(shadeStructure: ShadeStructure): String = """#version ${glslVersion()}
${primitiveTypes("d_font_image_map")}
${shadeStructure.buffers ?: ""}
${shadeStructure.uniforms ?: ""}
layout(origin_upper_left) in vec4 gl_FragCoord;

uniform sampler2D image;
flat in int v_instance;
flat in int v_element;

${drawerUniforms()}
${shadeStructure.varyingIn ?: ""}
${transformVaryingIn}

${if (!shadeStructure.suppressDefaultOutput) "out vec4 o_color;" else ""}

${shadeStructure.fragmentPreamble ?: ""}

void main(void) {
    ${
        fragmentMainConstants(
                element = "v_element",
                instance = "v_instance",
                boundsPosition = "vec3(va_bounds.xy, 0.0)",
                boundsSize = "vec3(va_bounds.zw, 0.0)")
    }

    float imageMap = texture(image, va_texCoord0).r;
    
    vec4 x_fill = vec4(u_fill.rgb,u_fill.a * imageMap);
    vec4 x_stroke = u_stroke;
    {
        ${shadeStructure.fragmentTransform ?: ""}
    }
    vec4 final = x_fill;
    final.rgb *= final.a;
    ${if (!shadeStructure.suppressDefaultOutput) "o_color = final;" else ""}
}
"""

    override fun fontImageMapVertexShader(shadeStructure: ShadeStructure): String = """#version ${glslVersion()}
${primitiveTypes("d_font_image_map")}
${shadeStructure.buffers ?: ""}
${drawerUniforms()}

${shadeStructure.attributes ?: ""}
${shadeStructure.uniforms ?: ""}
${shadeStructure.varyingOut ?: ""}
${transformVaryingOut}
${shadeStructure.vertexPreamble ?: ""}
flat out int v_instance;
flat out int v_element;

void main() {
    ${vertexMainConstants("int(a_position.z)")}
    vec3 decodedPosition = vec3(a_position.xy, 0.0);
    v_element = int(a_position.z);
    v_instance = int(a_instance);

    ${shadeStructure.varyingBridge ?: ""}
    ${preVertexTransform}
    vec3 x_normal = vec3(0.0, 0.0, 1.0);
    vec3 x_position = decodedPosition;
    {
        ${shadeStructure.vertexTransform ?: ""}
    }
    ${postVertexTransform}
    gl_Position = v_clipPosition;
}
            """

    override fun rectangleFragmentShader(shadeStructure: ShadeStructure): String = """#version ${glslVersion()}
${primitiveTypes("d_rectangle")}
${shadeStructure.buffers ?: ""}
${shadeStructure.uniforms ?: ""}
layout(origin_upper_left) in vec4 gl_FragCoord;

${drawerUniforms(styleBlock = false)}
${shadeStructure.varyingIn ?: ""}
${shadeStructure.outputs ?: ""}
${transformVaryingIn}

${if (!shadeStructure.suppressDefaultOutput) "out vec4 o_color;" else ""}

${shadeStructure.fragmentPreamble ?: ""}
flat in int v_instance;
in vec3 v_boundsSize;

void main(void) {
    ${
        fragmentMainConstants(
                boundsPosition = "vec3(va_texCoord0, 0.0)",
                boundsSize = "v_boundsSize")
    }
    vec4 x_fill = vi_fill;
    vec4 x_stroke = vi_stroke;
    {
        ${shadeStructure.fragmentTransform ?: ""}
    }
    vec2 wd = fwidth(va_texCoord0 - vec2(0.5));
    vec2 d = abs((va_texCoord0 - vec2(0.5)) * 2);

    float irx = smoothstep(0.0, wd.x * 2.5, 1.0-d.x - vi_strokeWeight*2.0/vi_dimensions.x);
    float iry = smoothstep(0.0, wd.y * 2.5, 1.0-d.y - vi_strokeWeight*2.0/vi_dimensions.y);
    float ir = irx*iry;

    vec4 final = vec4(1.0);
    final.rgb = x_fill.rgb * x_fill.a;
    final.a = x_fill.a;

    float sa = (1.0-ir) * x_stroke.a;
    final.rgb = final.rgb * (1.0-sa) + x_stroke.rgb * sa;
    final.a = final.a * (1.0-sa) + sa;

    ${
        if (!shadeStructure.suppressDefaultOutput) """
    |   o_color = final;
    """.trimMargin() else ""
    }
}
"""

    override fun rectangleVertexShader(shadeStructure: ShadeStructure): String = """#version ${glslVersion()}
${primitiveTypes("d_rectangle")}
${shadeStructure.buffers ?: ""}
${drawerUniforms(styleBlock = false)}
${shadeStructure.attributes ?: ""}
${shadeStructure.uniforms ?: ""}
${shadeStructure.varyingOut ?: ""}
${transformVaryingOut}

${shadeStructure.vertexPreamble ?: ""}

flat out int v_instance;
out vec3 v_boundsSize;
${rotate2}

void main() {
    v_instance =  gl_InstanceID;
    ${vertexMainConstants()}
    ${shadeStructure.varyingBridge ?: ""}
    ${preVertexTransform}
    vec3 x_normal = vec3(0.0, 0.0, 1.0);
    vec2 rotatedPosition = rotate2(i_rotation) * (( a_position.xy - vec2(0.5) ) * i_dimensions) + vec2(0.5) * i_dimensions;
      
    vec3 x_position = vec3(rotatedPosition, 0.0) + i_offset;
    v_boundsSize = vec3(i_dimensions, 1.0);
    {
        ${shadeStructure.vertexTransform ?: ""}
    }
    ${postVertexTransform}
    gl_Position = v_clipPosition;
    }
    """

    override fun expansionFragmentShader(shadeStructure: ShadeStructure): String = """#version ${glslVersion()}
${primitiveTypes("d_expansion")}
${shadeStructure.buffers ?: ""}
${shadeStructure.uniforms ?: ""}
layout(origin_upper_left) in vec4 gl_FragCoord;
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
${if (!shadeStructure.suppressDefaultOutput) "out vec4 o_color;" else ""}

${shadeStructure.fragmentPreamble ?: ""}

float strokeMask() {
	return min(1.0, (1.0-abs(v_ftcoord.x*2.0-1.0))*strokeMult) * min(1.0, v_ftcoord.y);
	//return pow(min(1.0, (1.0-abs(v_ftcoord.x*2.0-1.0)*strokeMult)) * min(1.0, v_ftcoord.y), 1.0);
    //return smoothstep(0.0, 1.0, (1.0-abs(v_ftcoord.x*2.0-1.0))*strokeMult) * smoothstep(0.0, 1.0, v_ftcoord.y);
}

void main(void) {
    ${
        fragmentMainConstants(boundsPosition = "vec3(v_objectPosition.xy - bounds.xy, 0.0) / vec3(bounds.zw,1.0)",
                boundsSize = "vec3(bounds.zw, 0.0)",
                contourPosition = "va_vertexOffset"
        )
    }

	float strokeAlpha = strokeMask();

    vec4 x_stroke = u_stroke;
    vec4 x_fill = u_fill;

    { ${shadeStructure.fragmentTransform ?: ""} }

    vec4 color = mix(x_stroke, x_fill, strokeFillFactor)  * vec4(1, 1, 1, strokeAlpha);
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

    override fun expansionVertexShader(shadeStructure: ShadeStructure): String = """#version ${glslVersion()}
${primitiveTypes("d_expansion")}
${shadeStructure.buffers ?: ""}
${drawerUniforms()}
${shadeStructure.uniforms ?: ""}
${shadeStructure.attributes}
${shadeStructure.varyingOut ?: ""}
${transformVaryingOut}

${shadeStructure.vertexPreamble ?: ""}

out vec2 v_ftcoord;
out float v_offset;

out vec3 v_objectPosition;
flat out int v_instance;

void main() {
    v_instance = 0;
    ${vertexMainConstants()}
    ${shadeStructure.varyingBridge ?: ""}
    v_objectPosition = vec3(a_position, 0.0);
    v_ftcoord = a_texCoord0;

    vec3 x_position = vec3(a_position, 0.0);
    vec3 x_normal = vec3(0.0, 0.0, 1.0);
    $preVertexTransform
    {
        ${shadeStructure.vertexTransform ?: ""}
    }
    $postVertexTransform

    gl_Position = v_clipPosition;
}
"""

    override fun fastLineFragmentShader(shadeStructure: ShadeStructure): String = """#version ${glslVersion()}
${primitiveTypes("d_fast_line")}
${shadeStructure.buffers ?: ""}
${shadeStructure.uniforms ?: ""}
layout(origin_upper_left) in vec4 gl_FragCoord;

uniform sampler2D image;
${drawerUniforms()}
${shadeStructure.varyingIn ?: ""}
$transformVaryingIn
flat in int v_instance;
${if (!shadeStructure.suppressDefaultOutput) "out vec4 o_color;" else ""}

${shadeStructure.fragmentPreamble ?: ""}

void main(void) {
    ${fragmentMainConstants()}
    vec4 x_fill = u_fill;
    vec4 x_stroke = u_stroke;
    {
        ${shadeStructure.fragmentTransform ?: ""}
    }


    vec4 final = x_stroke;
    final = x_stroke;
    final.rgb *= final.a;
    ${if (!shadeStructure.suppressDefaultOutput) "o_color = final;" else ""}
}
"""

    override fun fastLineVertexShader(shadeStructure: ShadeStructure): String = """#version ${glslVersion()}
${primitiveTypes("d_fast_line")}
${shadeStructure.buffers ?: ""}
${drawerUniforms()}
${shadeStructure.attributes ?: ""}
${shadeStructure.uniforms ?: ""}
${shadeStructure.varyingOut ?: ""}
${transformVaryingOut}

${shadeStructure.vertexPreamble ?: ""}

flat out int v_instance;

void main() {
    v_instance = gl_InstanceID;
    ${vertexMainConstants()}
    ${shadeStructure.varyingBridge ?: ""}
    $preVertexTransform
    vec3 x_normal = vec3(0.0, 0.0, 1.0);
    vec3 x_position = a_position;
    {
        ${shadeStructure.vertexTransform ?: ""}
    }
    $postVertexTransform
    gl_Position = v_clipPosition;
}
"""

    override fun meshLineFragmentShader(shadeStructure: ShadeStructure): String = """
        |#version ${glslVersion()}
        |${primitiveTypes("d_mesh_line")}
        |${shadeStructure.buffers ?: ""}
        |${shadeStructure.outputs ?: ""}
        |${shadeStructure.uniforms ?: ""}
        |layout(origin_upper_left) in vec4 gl_FragCoord;
        |
        |uniform sampler2D image;
        |${shadeStructure.fragmentPreamble ?: ""}
        |${drawerUniforms()}
        |${shadeStructure.varyingIn ?: ""}
        |$transformVaryingIn
        |flat in int v_instance;
        |${if (!shadeStructure.suppressDefaultOutput) "out vec4 o_color;" else ""}
        |void main(void) {
        |   ${fragmentMainConstants()}
        |   vec4 x_fill = u_fill;
        |   vec4 x_stroke = va_color;
        |   {
        |       ${shadeStructure.fragmentTransform ?: ""}
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
        |#version ${glslVersion()}
        |${shadeStructure.buffers ?: ""}
        |${primitiveTypes("d_mesh_line")}
        |${drawerUniforms()}
        |${shadeStructure.attributes ?: ""}
        |${shadeStructure.uniforms ?: ""}
        |${shadeStructure.varyingOut ?: ""}
        |$transformVaryingOut
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
        |   ${vertexMainConstants(element = "int(a_element)")}
        |   ${shadeStructure.varyingBridge ?: ""}
        |   $preVertexTransform
        |   vec3 x_normal = vec3(0.0, 0.0, 1.0);
        |   vec3 x_position = a_position;
        |   {
        |       ${shadeStructure.vertexTransform ?: ""}
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
        |${shadeStructure.buffers ?: ""}
        |#version ${glslVersion()}
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
        |   ${shadeStructure.vertexTransform ?: ""}
        |}
    """.trimMargin()

    override fun filterFragmentShader(shadeStructure: ShadeStructure): String = """
        |// -- ShaderGeneratorsGL3.filterFragmentShader
        |${shadeStructure.buffers ?: ""}
        |#version ${glslVersion()}
        |in vec2 v_texCoord0;
        |uniform sampler2D tex0;
        |uniform sampler2D tex1;
        |uniform sampler2D tex2;
        |uniform sampler2D tex3;
        |uniform sampler2D tex4;
        |uniform vec2 targetSize;
        |// -- drawerUniforms
        |${drawerUniforms()}
        |// -- shadeStructure.outputs
        |${shadeStructure.outputs ?: ""}
        |${if (!shadeStructure.suppressDefaultOutput) "out vec4 o_color;" else ""}
        |// -- shadeStructure.uniforms
        |${shadeStructure.uniforms ?: ""}
        |// -- shadeStructure.fragmentPreamble
        |${shadeStructure.fragmentPreamble ?: ""}
        |void main() {
        |   ${fragmentMainConstants(instance = "0", screenPosition = "v_texCoord0", boundsPosition = "vec3(v_texCoord0, 0.0)", boundsSize = "vec3(targetSize, 0.0)")}
        |   vec4 x_fill = texture(tex0, v_texCoord0);
        |   vec4 x_stroke = vec4(0.0);
        |   {
        |       // -- shadeStructure.fragmentTransform
        |       ${shadeStructure.fragmentTransform ?: ""}
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


}