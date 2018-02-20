#version 330

layout(location=0) out vec4 o_output;

uniform sampler2D image;
uniform sampler2D position;
in vec2 v_texCoord0;

uniform float minCoc;
uniform float maxCoc;

uniform float focus;
uniform float blurScale;

void main() {

    vec3 color = texture(image, v_texCoord0).rgb  * texture(image, v_texCoord0).a;;
    float depth = -texture(position, v_texCoord0).z;

    //float size =  (textureSize(image,0).y/960.0) * 16.0*abs(10.0 - (10.0/depth));

    float size =  32.0*abs(1.0 - (focus/depth));
    //     size += length(v_texCoord0-vec2(0.5)*2)*0.125;
    size = clamp(size, minCoc, maxCoc);
    o_output = vec4(color, size);
}