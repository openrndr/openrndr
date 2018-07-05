#version 330

// based on https://colinbarrebrisebois.com/2017/04/18/hexagonal-bokeh-blur-revisited/
// changed BlurTexture to overcome artefacts.

layout(location=0) out vec4 o_vertical;
layout(location=1) out vec4 o_diagonal;

uniform vec2 vertical;
uniform vec2 diagonal;
uniform int samples;
uniform float phase;

in vec2 v_texCoord0;

uniform sampler2D image;

const float PI = 3.14159265322f;

float saturate(float x) {
    return (clamp(x, 0.0, 1.0));
}

#define HASHSCALE 443.8975
vec2 hash22(vec2 p) {
	vec3 p3 = fract(vec3(p.xyx) * HASHSCALE);
    p3 += dot(p3, p3.yzx+19.19);
    return fract(vec2((p3.x + p3.y)*p3.z, (p3.x+p3.z)*p3.y));
}

vec4 BlurTexture(vec4 org, sampler2D tex, vec2 uv, vec2 direction) {
    vec2 dt = 1.0/textureSize(image, 0);
    vec4 finalColor = vec4(0.0);
    float coc = org.a;
    vec2 n = hash22(uv);
    float blurAmount = 0.0;
    uv += direction * (0.5);

    for (int i = 0; i < samples; ++i) {
        vec2 c = uv + direction * i;
        vec2 dcdx = dFdx(c);
        vec2 dcdy = dFdy(c);
        vec4 color = textureGrad(tex, c, dcdx, dcdy);

        float orgCoc = color.a;
        float chroma = 1.0; //max(0.0, 1.0-dot(vec3(1.0), color.rgb)/3.0)*0.5+0.5;
        blurAmount += color.a * saturate(-1.5+color.a - i) * saturate(-1.5+coc-i) * chroma;
        finalColor += color * saturate(-1.5+color.a - i) *saturate(-1.5+coc-i) * chroma;
    }

    if (blurAmount > 0) {
        return (finalColor / blurAmount);
    } else {
        return vec4(1.0, 0.0, 0.0, 1.0);
    }
}

void main() {
    vec2 viewport = textureSize(image, 0);
    vec2 invViewDims = 1.0/viewport;

    vec4 org = texture(image, v_texCoord0);
    float coc = org.a;

// CoC-weighted vertical blur.
    vec2 blurDir = invViewDims * vec2(cos(PI/2 + phase ), sin(PI/2 + phase));
    vec4 color = BlurTexture(org, image, v_texCoord0, blurDir);

// CoC-weighted diagonal blur.
    vec2 blurDir2 = invViewDims * vec2(cos(-PI/6 + phase), sin(-PI/6 + phase));
    vec4 color2 = BlurTexture(org, image, v_texCoord0, blurDir2);

    o_vertical = vec4(color.rgb * coc, coc);
    o_diagonal = vec4((color2.rgb * coc + color.rgb * coc), coc);
}