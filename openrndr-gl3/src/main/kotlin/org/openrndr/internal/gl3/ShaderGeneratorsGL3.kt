@file:Suppress("RemoveCurlyBracesFromTemplate")

package org.openrndr.internal.gl3

import org.openrndr.draw.ShadeStructure
import org.openrndr.internal.ShaderGenerators

private fun primitiveTypes(type: String) = """
// -- primitiveTypes
#define d_vertex_buffer 0
#define d_image 1
#define d_circle 2
#define d_rectangle 3
#define d_font_image_map 4
#define d_expansion 5
#define d_fast_line 6
#define d_mesh_line 7
#define d_point 8
#define d_primitive $type
"""

fun vertexConstants(instance: String = "gl_InstanceID", element: String = "0") = """
|    int c_instance = $instance;
|    int c_element = $element;
""".trimMargin()

fun fragmentConstants(
        instance: String = "v_instance",
        element: String = "0",
        screenPosition: String = "gl_FragCoord.xy / u_contentScale",
        contourPosition: String = "0",
        boundsPosition: String = "vec3(0.0)",
        boundsSize: String = "vec3(0.0)") = """
|    // -- fragmentConstants
|    int c_instance = $instance;
|    int c_element = $element;
|    vec2 c_screenPosition = $screenPosition;
|    float c_contourPosition = $contourPosition;
|    vec3 c_boundsPosition = $boundsPosition;
|    vec3 c_boundsSize = $boundsSize;
""".trimMargin()

private const val drawerUniforms = """
// -- drawerUniforms    
layout(shared) uniform ContextBlock {
    uniform mat4 u_modelNormalMatrix;
    uniform mat4 u_modelMatrix;
    uniform mat4 u_viewNormalMatrix;
    uniform mat4 u_viewMatrix;
    uniform mat4 u_projectionMatrix;
    uniform float u_contentScale;
    uniform vec2 u_viewDimensions;
};

layout(shared) uniform StyleBlock {
    uniform vec4 u_fill;
    uniform vec4 u_stroke;
    uniform float u_strokeWeight;
    uniform float[25] u_colorMatrix;
};
"""

private const val transformVaryingOut = """
// -- transformVaryingOut    
out vec3 v_worldNormal;
out vec3 v_viewNormal;
out vec3 v_worldPosition;
out vec3 v_viewPosition;
out vec4 v_clipPosition;

flat out mat4 v_modelNormalMatrix;
"""

private const val transformVaryingIn = """
// -- transformVaryingIn
in vec3 v_worldNormal;
in vec3 v_viewNormal;
in vec3 v_worldPosition;
in vec3 v_viewPosition;
in vec4 v_clipPosition;
flat in mat4 v_modelNormalMatrix;
"""

private const val preTransform = """
    // -- preTransform
    mat4 x_modelMatrix = u_modelMatrix;
    mat4 x_viewMatrix = u_viewMatrix;
    mat4 x_modelNormalMatrix = u_modelNormalMatrix;
    mat4 x_viewNormalMatrix = u_viewNormalMatrix;
    mat4 x_projectionMatrix = u_projectionMatrix;
"""

private const val postTransform = """
    // -- postTransform
    v_worldNormal = (x_modelNormalMatrix * vec4(x_normal,0.0)).xyz;
    v_viewNormal = (x_viewNormalMatrix * vec4(v_worldNormal,0.0)).xyz;
    v_worldPosition = (x_modelMatrix * vec4(x_position, 1.0)).xyz;
    v_viewPosition = (x_viewMatrix * vec4(v_worldPosition, 1.0)).xyz;
    v_clipPosition = x_projectionMatrix * vec4(v_viewPosition, 1.0);
    v_modelNormalMatrix = x_modelNormalMatrix;
"""

class ShaderGeneratorsGL3 : ShaderGenerators {
    override fun vertexBufferFragmentShader(shadeStructure: ShadeStructure): String = """|#version 330 core
|${primitiveTypes("d_vertex_buffer")}
|${shadeStructure.uniforms ?: ""}
|layout(origin_upper_left) in vec4 gl_FragCoord;

|uniform sampler2D image;
|$drawerUniforms
|${shadeStructure.varyingIn ?: ""}
|${shadeStructure.outputs ?: ""}
|${transformVaryingIn}

|${if (!shadeStructure.suppressDefaultOutput) "out vec4 o_color;" else ""}

|${shadeStructure.fragmentPreamble ?: ""}
|flat in int v_instance;

|void main(void) {
|    ${fragmentConstants(element = "v_instance")}
|    vec4 x_fill = u_fill;
|    vec4 x_stroke = u_stroke;
|    {
|       ${shadeStructure.fragmentTransform ?: ""}
|    }
     ${if (!shadeStructure.suppressDefaultOutput) """
     |    o_color = x_fill;
     |    o_color.rgb *= o_color.a;
     |""".trimMargin() else ""}
|}""".trimMargin()

    override fun vertexBufferVertexShader(shadeStructure: ShadeStructure): String = """#version 330 core
${primitiveTypes("d_vertex_buffer")}
$drawerUniforms
${shadeStructure.attributes ?: ""}
${shadeStructure.uniforms ?: ""}
${shadeStructure.varyingOut ?: ""}
${transformVaryingOut}
${shadeStructure.vertexPreamble ?: ""}

flat out int v_instance;
void main() {
    int instance = gl_InstanceID; // this will go use c_instance instead
${vertexConstants()}
${shadeStructure.varyingBridge ?: ""}
    vec3 x_normal = vec3(0.0, 0.0, 0.0);
    ${if (shadeStructure.attributes?.contains("vec3 a_normal;") == true) "x_normal = a_normal;" else ""}
    vec3 x_position = a_position;

    ${preTransform}
    {
        ${shadeStructure.vertexTransform ?: ""}
    }
    ${postTransform}

    v_instance = instance;
    gl_Position = v_clipPosition;
}
            """.trimMargin()

    override fun imageFragmentShader(shadeStructure: ShadeStructure): String = """#version 330
${primitiveTypes("d_image")}
${shadeStructure.uniforms ?: ""}
layout(origin_upper_left) in vec4 gl_FragCoord;

uniform sampler2D image;
$drawerUniforms
${shadeStructure.varyingIn ?: ""}
${transformVaryingIn}

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
    ${fragmentConstants(boundsPosition = "v_boundsPosition")}
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
#version 330
${primitiveTypes("d_image")}
$drawerUniforms
uniform int u_flipV;
${shadeStructure.attributes ?: ""}
${shadeStructure.uniforms ?: ""}
${shadeStructure.varyingOut ?: ""}
${transformVaryingOut}

flat out int v_instance;

out vec3 v_boundsPosition;
void main() {
    v_instance = gl_InstanceID;
    ${vertexConstants()}
    ${shadeStructure.varyingBridge ?: ""}
    ${preTransform}
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
    ${postTransform}
    gl_Position = v_clipPosition;
}
"""

    override fun imageArrayTextureFragmentShader(shadeStructure: ShadeStructure): String = """#version 330
${primitiveTypes("d_image")}
${shadeStructure.uniforms ?: ""}
layout(origin_upper_left) in vec4 gl_FragCoord;

uniform sampler2DArray image;
$drawerUniforms
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
    ${fragmentConstants(boundsPosition = "v_boundsPosition")}
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
#version 330
${primitiveTypes("d_image")}
$drawerUniforms
uniform int u_flipV;
${shadeStructure.attributes ?: ""}
${shadeStructure.uniforms ?: ""}
${shadeStructure.varyingOut ?: ""}
${transformVaryingOut}

flat out int v_instance;
flat out int v_layer;

out vec3 v_boundsPosition;
void main() {
    v_instance = gl_InstanceID;
    ${vertexConstants()}
    ${shadeStructure.varyingBridge ?: ""}
    ${preTransform}
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
    ${postTransform}
    gl_Position = v_clipPosition;
}
"""

    override fun pointFragmentShader(shadeStructure: ShadeStructure): String = """#version 330 core
${primitiveTypes("d_circle")}
${shadeStructure.uniforms ?: ""}
layout(origin_upper_left) in vec4 gl_FragCoord;

$drawerUniforms
${shadeStructure.varyingIn ?: ""}
${transformVaryingIn}

${if (!shadeStructure.suppressDefaultOutput) "out vec4 o_color;" else ""}
flat in int v_instance;
in vec3 v_boundsSize;
void main(void) {
    ${fragmentConstants(boundsPosition = "vec3(0.0, 0.0, 0.0)",
            boundsSize = "v_boundsSize")}

    vec4 x_fill = u_fill;
    vec4 x_stroke = u_stroke;
    {
        ${shadeStructure.fragmentTransform ?: ""}
    }
    ${if (!shadeStructure.suppressDefaultOutput) "o_color = x_fill;" else ""}
}

    """.trimMargin()

    override fun pointVertexShader(shadeStructure: ShadeStructure): String = """#version 330 core
${primitiveTypes("d_point")}
$drawerUniforms
${shadeStructure.attributes ?: ""}
${shadeStructure.uniforms ?: ""}
${shadeStructure.varyingOut ?: ""}
${transformVaryingOut}
flat out int v_instance;
out vec3 v_boundsSize;
void main() {
    v_instance = gl_InstanceID;
    ${vertexConstants()}
    ${shadeStructure.varyingBridge ?: ""}

    v_boundsSize = vec3(0, 0.0, 0.0);
    ${preTransform}
    vec3 x_normal = vec3(0.0, 0.0, 1.0);
    vec3 x_position = a_position  + i_offset;
    {
        ${shadeStructure.vertexTransform ?: ""}
    }
    va_position = x_position;
    ${postTransform}
    gl_Position = v_clipPosition;
}
    """


    override fun circleFragmentShader(shadeStructure: ShadeStructure): String = """#version 330 core
${primitiveTypes("d_circle")}
${shadeStructure.uniforms ?: ""}
layout(origin_upper_left) in vec4 gl_FragCoord;

$drawerUniforms
${shadeStructure.varyingIn ?: ""}
${transformVaryingIn}

${if (!shadeStructure.suppressDefaultOutput) "out vec4 o_color;" else ""}
flat in int v_instance;
in vec3 v_boundsSize;
void main(void) {
    ${fragmentConstants(boundsPosition = "vec3(va_texCoord0, 0.0)",
            boundsSize = "v_boundsSize")}
    float smoothFactor = 3.0;

    vec4 x_fill = u_fill;
    vec4 x_stroke = u_stroke;
    {
        ${shadeStructure.fragmentTransform ?: ""}
    }
    float wd = fwidth(length(va_texCoord0 - vec2(0.0)));
    float d = length(va_texCoord0 - vec2(0.5)) * 2;

    float or = smoothstep(0, wd * smoothFactor, 1.0 - d);
    float b = u_strokeWeight / vi_radius;
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

    override fun circleVertexShader(shadeStructure: ShadeStructure): String = """#version 330 core
${primitiveTypes("d_circle")}
$drawerUniforms
${shadeStructure.attributes ?: ""}
${shadeStructure.uniforms ?: ""}
${shadeStructure.varyingOut ?: ""}
${transformVaryingOut}
flat out int v_instance;
out vec3 v_boundsSize;
void main() {
    v_instance = gl_InstanceID;
    ${vertexConstants()}
    ${shadeStructure.varyingBridge ?: ""}

    v_boundsSize = vec3(i_radius, i_radius, 0.0);
    ${preTransform}
    vec3 x_normal = a_normal;
    vec3 x_position = a_position * i_radius + i_offset;
    {
        ${shadeStructure.vertexTransform ?: ""}
    }
    va_position = x_position;
    ${postTransform}
    gl_Position = v_clipPosition;

}
    """

    override fun fontImageMapFragmentShader(shadeStructure: ShadeStructure): String = """#version 330 core
${primitiveTypes("d_font_image_map")}
${shadeStructure.uniforms ?: ""}
layout(origin_upper_left) in vec4 gl_FragCoord;

uniform sampler2D image;
flat in int v_instance;
flat in int v_element;

$drawerUniforms
${shadeStructure.varyingIn ?: ""}
${transformVaryingIn}
${shadeStructure.fragmentPreamble ?: ""}
${if (!shadeStructure.suppressDefaultOutput) "out vec4 o_color;" else ""}

void main(void) {
    ${fragmentConstants(
            element = "v_element",
            instance = "v_instance",
            boundsPosition = "vec3(va_bounds.xy, 0.0)",
            boundsSize = "vec3(va_bounds.zw, 0.0)")}

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

    override fun fontImageMapVertexShader(shadeStructure: ShadeStructure): String = """#version 330 core
${primitiveTypes("d_font_image_map")}

$drawerUniforms

${shadeStructure.attributes ?: ""}
${shadeStructure.uniforms ?: ""}
${shadeStructure.varyingOut ?: ""}
${transformVaryingOut}
${shadeStructure.vertexPreamble ?: ""}
flat out int v_instance;
flat out int v_element;

void main() {
    ${vertexConstants("int(a_position.z)")}
    vec3 decodedPosition = vec3(a_position.xy, 0.0);
    v_element = int(a_position.z);
    v_instance = int(a_instance);

    ${shadeStructure.varyingBridge ?: ""}
    ${preTransform}
    vec3 x_normal = vec3(0.0, 0.0, 1.0);
    vec3 x_position = decodedPosition;
    {
        ${shadeStructure.vertexTransform ?: ""}
    }
    ${postTransform}
    gl_Position = v_clipPosition;
}
            """

    override fun rectangleFragmentShader(shadeStructure: ShadeStructure): String = """#version 330 core
${primitiveTypes("d_rectangle")}

${shadeStructure.uniforms ?: ""}
layout(origin_upper_left) in vec4 gl_FragCoord;

$drawerUniforms
${shadeStructure.varyingIn ?: ""}
${shadeStructure.outputs ?: ""}
${transformVaryingIn}

${shadeStructure.fragmentPreamble ?: ""}
flat in int v_instance;
in vec3 v_boundsSize;
${if (!shadeStructure.suppressDefaultOutput) "out vec4 o_color;" else ""}
void main(void) {
    ${fragmentConstants(
            boundsPosition = "vec3(va_texCoord0, 0.0)",
            boundsSize = "v_boundsSize")}
    vec4 x_fill = u_fill;
    vec4 x_stroke = u_stroke;
    {
        ${shadeStructure.fragmentTransform ?: ""}
    }
    vec2 wd = fwidth(va_texCoord0 - vec2(0.5));
    vec2 d = abs((va_texCoord0 - vec2(0.5)) * 2);

    float irx = smoothstep(0.0, wd.x * 2.5, 1.0-d.x - u_strokeWeight*2.0/vi_dimensions.x);
    float iry = smoothstep(0.0, wd.y * 2.5, 1.0-d.y - u_strokeWeight*2.0/vi_dimensions.y);
    float ir = irx*iry;

    vec4 final = vec4(1.0);
    final.rgb = x_fill.rgb * x_fill.a;
    final.a = x_fill.a;

    float sa = (1.0-ir) * x_stroke.a;
    final.rgb = final.rgb * (1.0-sa) + x_stroke.rgb * sa;
    final.a = final.a * (1.0-sa) + sa;

    ${if (!shadeStructure.suppressDefaultOutput) """
    |   o_color = final;
    """.trimMargin() else ""}
}
"""

    override fun rectangleVertexShader(shadeStructure: ShadeStructure): String = """#version 330 core
${primitiveTypes("d_rectangle")}
$drawerUniforms
${shadeStructure.attributes ?: ""}
${shadeStructure.uniforms ?: ""}
${shadeStructure.varyingOut ?: ""}
${transformVaryingOut}

${shadeStructure.vertexPreamble ?: ""}

flat out int v_instance;
out vec3 v_boundsSize;

void main() {
    v_instance =  gl_InstanceID;
    ${vertexConstants()}
    ${shadeStructure.varyingBridge ?: ""}
    ${preTransform}
    vec3 x_normal = vec3(0.0, 0.0, 1.0);
    vec3 x_position = a_position * vec3(i_dimensions,1.0) + i_offset;
    v_boundsSize = vec3(i_dimensions, 1.0);
    {
        ${shadeStructure.vertexTransform ?: ""}
    }
    ${postTransform}
    gl_Position = v_clipPosition;
    }
    """

    override fun expansionFragmentShader(shadeStructure: ShadeStructure): String = """#version 330 core
${primitiveTypes("d_expansion")}

${shadeStructure.uniforms ?: ""}
layout(origin_upper_left) in vec4 gl_FragCoord;
$drawerUniforms
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

float strokeMask() {
	return min(1.0, (1.0-abs(v_ftcoord.x*2.0-1.0))*strokeMult) * min(1.0, v_ftcoord.y);
}

void main(void) {
    ${fragmentConstants(boundsPosition = "vec3(v_objectPosition.xy - bounds.xy, 0.0) / vec3(bounds.zw,1.0)",
            boundsSize = "vec3(bounds.zw, 0.0)",
            contourPosition = "va_vertexOffset"
    )}

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

    override fun expansionVertexShader(shadeStructure: ShadeStructure): String = """#version 330 core
${primitiveTypes("d_expansion")}
$drawerUniforms
${shadeStructure.uniforms ?: ""}
${shadeStructure.attributes}
${shadeStructure.varyingOut ?: ""}
${transformVaryingOut}

out vec2 v_ftcoord;
out float v_offset;

out vec3 v_objectPosition;
flat out int v_instance;

void main() {
    v_instance = 0;
    ${vertexConstants()}
    ${shadeStructure.varyingBridge ?: ""}
    v_objectPosition = vec3(a_position, 0.0);
    v_ftcoord = a_texCoord0;

    vec3 x_position = vec3(a_position, 0.0);
    vec3 x_normal = vec3(0.0, 0.0, 1.0);
    $preTransform
    {
        ${shadeStructure.vertexTransform ?: ""}
    }
    $postTransform

    gl_Position = v_clipPosition;
}
"""

    override fun fastLineFragmentShader(shadeStructure: ShadeStructure): String = """#version 330 core
${primitiveTypes("d_fast_line")}
${shadeStructure.uniforms ?: ""}
layout(origin_upper_left) in vec4 gl_FragCoord;

uniform sampler2D image;
$drawerUniforms
${shadeStructure.varyingIn ?: ""}
$transformVaryingIn
flat in int v_instance;
${if (!shadeStructure.suppressDefaultOutput) "out vec4 o_color;" else ""}

void main(void) {
    ${fragmentConstants()}
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

    override fun fastLineVertexShader(shadeStructure: ShadeStructure): String = """#version 330 core
${primitiveTypes("d_fast_line")}

$drawerUniforms
${shadeStructure.attributes ?: ""}
${shadeStructure.uniforms ?: ""}
${shadeStructure.varyingOut ?: ""}
${transformVaryingOut}
flat out int v_instance;

void main() {
    v_instance = gl_InstanceID;
    ${vertexConstants()}
    ${shadeStructure.varyingBridge ?: ""}
    $preTransform
    vec3 x_normal = vec3(0.0, 0.0, 1.0);
    vec3 x_position = a_position;
    {
        ${shadeStructure.vertexTransform ?: ""}
    }
    $postTransform
    gl_Position = v_clipPosition;
}
"""

    override fun meshLineFragmentShader(shadeStructure: ShadeStructure): String = """
        |#version 330 core
        |${primitiveTypes("d_mesh_line")}
        |${shadeStructure.outputs ?: ""}
        |${shadeStructure.uniforms ?: ""}
        |layout(origin_upper_left) in vec4 gl_FragCoord;
        |
        |uniform sampler2D image;
        |${shadeStructure.fragmentPreamble ?: ""}
        |$drawerUniforms
        |${shadeStructure.varyingIn ?: ""}
        |$transformVaryingIn
        |flat in int v_instance;
        |${if (!shadeStructure.suppressDefaultOutput) "out vec4 o_color;" else ""}
        |void main(void) {
        |   ${fragmentConstants()}
        |   vec4 x_fill = u_fill;
        |   vec4 x_stroke = va_color;
        |   {
        |       ${shadeStructure.fragmentTransform ?: ""}
        |   }
        |${if (!shadeStructure.suppressDefaultOutput) """
            |o_color = x_stroke;
            |o_color.rgb *= o_color.a;
            """.trimMargin() else ""}
        |}
        """.trimMargin()

    override fun meshLineVertexShader(shadeStructure: ShadeStructure): String = """
        |#version 330 core
        |${primitiveTypes("d_mesh_line")}
        |$drawerUniforms
        |${shadeStructure.attributes ?: ""}
        |${shadeStructure.uniforms ?: ""}
        |${shadeStructure.varyingOut ?: ""}
        |$transformVaryingOut
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
        |   ${vertexConstants(element = "int(a_element)")}
        |   ${shadeStructure.varyingBridge ?: ""}
        |   $preTransform
        |   vec3 x_normal = vec3(0.0, 0.0, 1.0);
        |   vec3 x_position = a_position;
        |
        |   float aspect = u_viewDimensions.x / u_viewDimensions.y;
        |   float pixelWidthRatio = 1. / (u_viewDimensions.x * u_projectionMatrix[0][0]);
        |   mat4 pvm = u_projectionMatrix * u_viewMatrix * u_modelMatrix;
        |   vec4 finalPosition = pvm * vec4(a_position, 1.0);
        |   vec4 prevPosition = pvm * vec4(a_previous, 1.0);
        |   vec4 nextPosition = pvm * vec4(a_next, 1.0);

        |   vec2 currentP = fix(finalPosition, aspect);
        |   vec2 prevP = fix(prevPosition, aspect);
        |   vec2 nextP = fix(nextPosition, aspect);

        |   float pixelWidth = finalPosition.w * pixelWidthRatio;
        |   float w = 1.8 * pixelWidth * a_width;

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
        |   vec2 normal = vec2(-dir.y, dir.x);
        |   normal.x /= aspect;
        |   normal *= .5 * w;
        |   vec4 offset = vec4(normal * a_side, 0.0, 1.0);
        |
        |   finalPosition.xy += offset.xy;
        |   v_clipPosition = finalPosition;
        |   gl_Position = finalPosition;
        |   {
        |       ${shadeStructure.vertexTransform ?: ""}
        |   }
        |   $postTransform
        |}
        """.trimMargin()

    override fun filterVertexShader(shadeStructure: ShadeStructure): String = """
        |// -- ShaderGeneratorsGL3.filterVertexShader
        |#version 330 core
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
        |#version 330 core
        |in vec2 v_texCoord0;
        |// -- drawerUniforms
        |${drawerUniforms}
        |// -- shadeStructure.outputs
        |${shadeStructure.outputs ?: ""}
        |${if (!shadeStructure.suppressDefaultOutput) "out vec4 o_color;" else ""}
        |// -- shadeStructure.uniforms
        |${shadeStructure.uniforms ?: ""}
        |// -- shadeStructure.fragmentPreamble
        |${shadeStructure.fragmentPreamble ?: ""}
        |void main() {
        |   ${fragmentConstants(instance = "0", screenPosition = "v_texCoord0")}
        |   vec4 x_fill = p_fill;
        |   vec4 x_stroke = vec4(0.0);
        |   {
        |       // -- shadeStructure.fragmentTransform
        |       ${shadeStructure.fragmentTransform ?: ""}
        |   }
        |${if (!shadeStructure.suppressDefaultOutput) """
            |o_color = x_fill;
            |o_color.rgb *= o_color.a;
            """.trimMargin() else ""}
        |}
        |
        
    """.trimMargin()


}