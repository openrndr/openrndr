package org.openrndr.internal.gl3

import org.openrndr.draw.ShadeStructure
import org.openrndr.internal.ShaderGenerators

private val drawerUniforms = """
uniform mat4 u_normalMatrix;
uniform mat4 u_viewMatrix;
uniform mat4 u_projectionMatrix;
uniform mat4 u_viewProjectionMatrix;
uniform vec4 u_fill;
uniform vec4 u_stroke;
uniform float u_strokeWeight;
uniform float[25] u_colorMatrix;
"""

class ShaderGeneratorsGL3 : ShaderGenerators {
    override fun vertexBufferFragmentShader(shadeStructure: ShadeStructure): String = """#version 330

${shadeStructure.uniforms?:""}


layout(origin_upper_left) in vec4 gl_FragCoord;

uniform sampler2D image;
$drawerUniforms
${shadeStructure.varyingIn?:""}
${shadeStructure.outputs?:""}

out vec4 o_color;
struct VertexData {
    vec3 position;
    vec3 normal;
};

${shadeStructure.fragmentPreamble?:""}

flat in int v_instance;

in VertexData world;
in VertexData view;
in VertexData object;
in VertexData clip;

void main(void) {
    vec4 x_fill = u_fill;
    vec4 x_stroke = u_stroke;
    {
        ${shadeStructure.fragmentTransform?:""}
    }
    o_color = x_fill;
    o_color.rgb *= o_color.a;
}
    """.trimMargin()

    override fun vertexBufferVertexShader(shadeStructure: ShadeStructure): String = """#version 330

$drawerUniforms
${shadeStructure.attributes?:""}
${shadeStructure.uniforms?:""}
${shadeStructure.varyingOut?:""}

struct VertexData {
    vec3 position;
    vec3 normal;
};

out VertexData object;
out VertexData view;
out VertexData clip;

${shadeStructure.vertexPreamble?:""}

flat out int v_instance;
void main() {

    int instance = gl_InstanceID;
    ${shadeStructure.varyingBridge?:""}

    vec3 x_normal = vec3(0.0, 0.0, 0.0);

    ${if (shadeStructure.attributes?.contains("vec3 a_normal;") == true) "x_normal = a_normal;" else ""}

    vec3 x_position = a_position;

    mat4 x_normalMatrix = u_normalMatrix;
    mat4 x_viewMatrix = u_viewMatrix;
    mat4 x_projectionMatrix = u_projectionMatrix;
    {
        ${shadeStructure.vertexTransform?:""}
    }

    object.position = x_position;
    object.normal = x_normal;

    vec4 viewPosition = x_viewMatrix * vec4( x_position, 1.0);
    view.position = viewPosition.xyz;
    view.normal = (x_normalMatrix * vec4(x_normal, 0.0)).xyz;

    vec4 clipPosition = x_projectionMatrix * viewPosition;
    clip.position = clipPosition.xyz/clipPosition.w;
    clip.normal = vec3(0.0);

    v_instance = instance;
    gl_Position = clipPosition;
}
            """.trimMargin()

    override fun imageFragmentShader(shadeStructure: ShadeStructure): String = """#version 330
${shadeStructure.uniforms?:""}

layout(origin_upper_left) in vec4 gl_FragCoord;

uniform sampler2D image;
$drawerUniforms
${shadeStructure.varyingIn?:""}

out vec4 o_color;

vec4 colorTransform(vec4 color, float[25] matrix) {
    float r = color.r * matrix[0] + color.g * matrix[5] + color.b * matrix[10] + color.a * matrix[15] + matrix[20];
    float g = color.r * matrix[1] + color.g * matrix[6] + color.b * matrix[11] + color.a * matrix[16] + matrix[21];
    float b = color.r * matrix[2] + color.g * matrix[7] + color.b * matrix[12] + color.a * matrix[17] + matrix[22];
    float a = color.r * matrix[3] + color.g * matrix[8] + color.b * matrix[13] + color.a * matrix[18] + matrix[23];
    return vec4(r, g, b, a);
}

void main(void) {
    vec4 x_fill = texture(image, va_texCoord0);
    vec4 x_stroke = u_stroke;
    {
        ${shadeStructure.fragmentTransform?:""}
    }

    x_fill = colorTransform(x_fill, u_colorMatrix);

    o_color = x_fill;
}"""

    override fun imageVertexShader(shadeStructure: ShadeStructure): String = """
#version 330

$drawerUniforms
${shadeStructure.attributes?:""}
${shadeStructure.uniforms?:""}
${shadeStructure.varyingOut?:""}

void main() {

    ${shadeStructure.varyingBridge?:""}

    vec3 x_position = a_position;
    {
        ${shadeStructure.vertexTransform?:""}
    }

    vec4 transformed = u_viewProjectionMatrix * vec4( vec3(x_position), 1.0);
    gl_Position = transformed;
}
"""

    override fun circleFragmentShader(shadeStructure: ShadeStructure): String = """#version 330
${shadeStructure.uniforms?:""}
layout(origin_upper_left) in vec4 gl_FragCoord;

$drawerUniforms
${shadeStructure.varyingIn?:""}

out vec4 o_color;

void main(void) {
    vec4 x_fill = u_fill;
    vec4 x_stroke = u_stroke;
    {
        ${shadeStructure.fragmentTransform?:""}
    }
    float wd = fwidth(length(va_texCoord0 - vec2(0.5)));
    float d = length(va_texCoord0 - vec2(0.5)) * 2;

    float f = smoothstep(0, wd * 2.5, 1.0 - d);
    float b = 0.1;
    float f2 = smoothstep(0, wd * 2.5, 1.0 - b - d) * f;

    o_color = vec4(x_fill.rgb*f2 + x_stroke.rgb*(1.0-f2), (x_fill.a*f) * f2 + (x_stroke.a*f) * (1.0-f2) );
    o_color.rgb *= o_color.a;
}
        """

    override fun circleVertexShader(shadeStructure: ShadeStructure): String = """#version 330

$drawerUniforms
${shadeStructure.attributes?:""}
${shadeStructure.uniforms?:""}
${shadeStructure.varyingOut?:""}

void main() {

    ${shadeStructure.varyingBridge?:""}

    vec3 x_position = a_position * i_radius + i_offset;
    {
        ${shadeStructure.vertexTransform?:""}
    }

    vec4 transformed = u_viewProjectionMatrix * vec4( vec3(x_position), 1.0);
    gl_Position = transformed;
}
    """

    override fun fontImageMapFragmentShader(shadeStructure: ShadeStructure): String = """#version 330

${shadeStructure.uniforms?:""}

layout(origin_upper_left) in vec4 gl_FragCoord;

uniform sampler2D image;
flat in int v_instance;

$drawerUniforms
${shadeStructure.varyingIn?:""}

out vec4 o_color;

void main(void) {

    int instance = v_instance;
    vec3 boundsPosition = vec3(va_bounds.xy, 0.0);
    vec3 boundsSize = vec3(va_bounds.zw, 0.0);

    float imageMap = texture(image, va_texCoord0).r;
    vec4 x_fill = u_fill * imageMap;
    vec4 x_stroke = u_stroke;
    {
        ${shadeStructure.fragmentTransform?:""}
    }
    o_color = x_fill;
}
"""

    override fun fontImageMapVertexShader(shadeStructure: ShadeStructure): String = """#version 330

$drawerUniforms
${shadeStructure.attributes?:""}
${shadeStructure.uniforms?:""}
${shadeStructure.varyingOut?:""}

flat out int v_instance;

void main() {
    vec3 decodedPosition = vec3(a_position.xy, 0.0);
    v_instance = int(a_position.z);

    ${shadeStructure.varyingBridge?:""}

    vec3 x_position = decodedPosition;
    {
        ${shadeStructure.vertexTransform?:""}
    }

    vec4 transformed = u_viewProjectionMatrix * vec4( vec3(x_position), 1.0);
    gl_Position = transformed;
}
            """

    override fun rectangleFragmentShader(shadeStructure: ShadeStructure): String = """#version 330

$drawerUniforms
${shadeStructure.uniforms?:""}
${shadeStructure.fragmentPreamble?:""}
${shadeStructure.varyingIn?:""}
${shadeStructure.outputs?:""}
out vec4 o_color;

void main(void) {
    vec3 boundsPosition = vec3(va_texCoord0, 0.0);
    vec4 x_fill = u_fill;
    {
        ${shadeStructure.fragmentTransform?:""}
    }
    o_color = x_fill;
    o_color.rgb *= o_color.a;
}
        """

    override fun rectangleVertexShader(shadeStructure: ShadeStructure): String = """#version 330

$drawerUniforms
${shadeStructure.attributes?:""}
${shadeStructure.uniforms?:""}
${shadeStructure.varyingOut?:""}
${shadeStructure.vertexPreamble?:""}

void main() {
    ${shadeStructure.varyingBridge?:""}
    vec4 transformed = u_viewProjectionMatrix * vec4( vec3(a_position.xy, 0), 1.0);
    gl_Position = transformed;
}
        """

    override fun expansionFragmentShader(shadeStructure: ShadeStructure): String = """#version 330

${shadeStructure.uniforms?:""}

$drawerUniforms
${shadeStructure.varyingIn?:""}
uniform float strokeMult;
uniform float strokeThr;
uniform float strokeFillFactor;

uniform sampler2D tex;
uniform vec4 bounds;

in vec3 v_objectPosition;

in vec2 v_ftcoord;

out vec4 o_color;

float strokeMask() {
	return min(1.0, (1.0-abs(v_ftcoord.x*2.0-1.0))*strokeMult) * min(1.0, v_ftcoord.y);
}

void main(void) {

    vec3 boundsPosition = vec3(v_objectPosition.xy - bounds.xy, 0.0) / vec3(bounds.zw,1.0);
    vec3 boundsSize = vec3(bounds.zw, 0.0);

    float strokeOffset = va_vertexOffset;
	float strokeAlpha = strokeMask();

    vec4 x_stroke = u_stroke;
    vec4 x_fill = u_fill;

    { ${shadeStructure.fragmentTransform?:""} }

    vec4 color = mix(x_stroke, x_fill, strokeFillFactor)  * vec4(1, 1, 1, strokeAlpha);
    vec4 result = color;

    if (strokeAlpha < strokeThr) {
	    discard;
	}

	o_color = result;
	o_color.rgb *= o_color.a;
}
        """

    override fun expansionVertexShader(shadeStructure: ShadeStructure): String = """#version 330
$drawerUniforms
${shadeStructure.attributes}
${shadeStructure.varyingOut?:""}

out vec2 v_ftcoord;
out float v_offset;

out vec3 v_objectPosition;

void main() {
    ${shadeStructure.varyingBridge?:""}
    v_objectPosition = vec3(a_position, 0.0);



    v_ftcoord = a_texCoord0;
    gl_Position = u_projectionMatrix * u_viewMatrix * vec4(a_position,0,1);
}
"""

    override fun fastLineFragmentShader(shadeStructure: ShadeStructure): String = """#version 330
${shadeStructure.uniforms?:""}

layout(origin_upper_left) in vec4 gl_FragCoord;

uniform sampler2D image;
$drawerUniforms
${shadeStructure.varyingIn?:""}

out vec4 o_color;

void main(void) {
    vec4 x_fill = u_fill;
    vec4 x_stroke = u_stroke;
    {
        ${shadeStructure.fragmentTransform?:""}
    }
    o_color = x_fill;
}
        """

    override fun fastLineVertexShader(shadeStructure: ShadeStructure): String = """#version 330

$drawerUniforms
${shadeStructure.attributes?:""}
${shadeStructure.uniforms?:""}

void main() {
    vec3 x_position = a_position;
    {
        ${shadeStructure.vertexTransform?:""}
    }
    vec4 transformed = u_viewProjectionMatrix * vec4( vec3(x_position), 1.0);
    gl_Position = transformed;
}
        """
}