#version 330

// based on https://colinbarrebrisebois.com/2017/04/18/hexagonal-bokeh-blur-revisited/
// changed BlurTexture to overcome artefacts.

layout(location=0) out vec4 o_vertical;
layout(location=1) out vec4 o_diagonal;

uniform vec2 vertical;
uniform vec2 diagonal;
uniform int samples;

in vec2 v_texCoord0;

uniform sampler2D image;

const float PI = 3.14159265322f;

float saturate(float x) {
    return floor(clamp(x, 0.0, 1.0));
}

vec4 BlurTexture(vec4 org, sampler2D tex, vec2 uv, vec2 direction) {
    vec4 finalColor = vec4(0.0);
    float blurAmount = 0.0;
    uv += direction * 0.5;
    float dc = org.a/(samples);
    for (int i = 1; i < samples; ++i) {
        vec4 color = texture(tex, uv + direction * i);
        float chroma = max(0.0, dot(vec3(1.0), color.rgb)) * 0.5 + 0.5;
        blurAmount += color.a * saturate(color.a - i*dc) * chroma;
        finalColor += color * saturate(color.a - i*dc) * chroma;
    }

    if (blurAmount > 1) {
        return (finalColor / blurAmount);
    } else {
        return vec4(org.rgb/org.a, 1.0);
    }
}

void main() {
    vec2 viewport = textureSize(image, 0);
    vec2 invViewDims = 1.0/viewport;

    vec4 org = texture(image, v_texCoord0);
    float coc = org.a;

// CoC-weighted vertical blur.
    vec2 blurDir = (coc/samples) * invViewDims * vec2(cos(PI/2), sin(PI/2));
    vec4 color = BlurTexture(org, image, v_texCoord0, blurDir);

// CoC-weighted diagonal blur.
    vec2 blurDir2 = (coc/samples) * invViewDims * vec2(cos(-PI/6), sin(-PI/6));
    vec4 color2 = BlurTexture(org, image, v_texCoord0, blurDir2);

    o_vertical = vec4(color.rgb * coc, coc);
    o_diagonal = vec4(color2.rgb * coc + color.rgb * coc, coc);

}