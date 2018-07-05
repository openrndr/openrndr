#version 330

layout(location=0) out vec4 o_output;

uniform int samples;

in vec2 v_texCoord0;

uniform sampler2D vertical;
uniform sampler2D diagonal;
uniform sampler2D original;

uniform float phase;
uniform vec2 direction0;
uniform vec2 direction1;

const float PI = 3.14159265322f;

float saturate(float x) {
    return (clamp(x, 0.0, 1.0));
}

vec4 BlurTexture(float coc, sampler2D tex, vec2 uv, vec2 direction)
{
    vec2 dt = 1.0/textureSize(vertical, 0);
    vec4 finalColor = vec4(0.0);
    float blurAmount = 0.0;
    uv += direction * (0.5);

    for (int i = 0; i < samples; ++i) {
        vec2 c = uv + direction * i;
        vec2 dcdx = dFdx(c);
        vec2 dcdy = dFdy(c);
        vec4 color = textureGrad(tex, c, dcdx, dcdy);

        float orgCoc = color.a;
        float chroma = 1.0;//max(0.0, 1.0-dot(vec3(1.0), color.rgb)/3.0)*0.5+0.5;
        blurAmount += color.a * saturate(-1.5+orgCoc-i) * saturate(-1.5+coc-i) * chroma;
        finalColor += color * saturate(-1.5+orgCoc-i)* saturate(-1.5+coc-i) * chroma;
    }

    if (blurAmount > 0.0) {
        return (finalColor / blurAmount);
    } else {
        return vec4(1.0, 0.0, 0.0, 1.0);
    }
}


void main() {
    vec2 viewport = textureSize(vertical, 0);
    vec2 invViewDims = 1.0/viewport;

    vec4 org = texture(vertical, v_texCoord0);
    vec4 org2 = texture(diagonal, v_texCoord0);

    float coc = org.a;
    float coc2 = org2.a;

    // Sample the vertical blur (1st MRT) texture with this new blur direction
    vec2 blurDir =  invViewDims * vec2(cos(-PI/6 + phase), sin(-PI/6 + phase));
    vec4 color = BlurTexture(coc, vertical, v_texCoord0, blurDir);

    // Sample the diagonal blur (2nd MRT) texture with this new blur direction
    vec2 blurDir2 =  invViewDims * vec2(cos(-5*PI/6 + phase), sin(-5*PI/6 + phase));
    vec4 color2 = BlurTexture(coc2, diagonal, v_texCoord0, blurDir2);

    o_output = vec4((color.rgb + color2.rgb) * 0.5f, 1.0);
}