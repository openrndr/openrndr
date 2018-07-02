#version 330
in vec2 v_texCoord0;
uniform sampler2D tex0;
uniform sampler2D lookup;
uniform float seed;
uniform float noiseGain;


out vec4 o_color;

#define HASHSCALE 443.8975
vec2 hash22(vec2 p) {
	vec3 p3 = fract(vec3(p.xyx) * HASHSCALE);
    p3 += dot(p3, p3.yzx+19.19);
    return fract(vec2((p3.x + p3.y)*p3.z, (p3.x+p3.z)*p3.y));
}

// -- from https://github.com/jeromeetienne/threex.coloradjust/blob/master/threex.coloradjust.js
vec4 sampleAs3DTexture(sampler2D lut, vec3 uv, float width) {
    float sliceSize = 1.0 / width;              // space of 1 slice
    float slicePixelSize = sliceSize / width;           // space of 1 pixel
    float sliceInnerSize = slicePixelSize * (width - 1.0);  // space of width pixels
    float zSlice0 = min(floor(uv.z * width), width - 1.0);
    float zSlice1 = min(zSlice0 + 1.0, width - 1.0);
    float xOffset = slicePixelSize * 0.5 + uv.x * sliceInnerSize;
    float s0 = xOffset + (zSlice0 * sliceSize);
    float s1 = xOffset + (zSlice1 * sliceSize);
    vec4 slice0Color = texture(lut, vec2(s0, 1.0-uv.y));
    vec4 slice1Color = texture(lut, vec2(s1, 1.0-uv.y));
    float zOffset = mod(uv.z * width, 1.0);
    vec4 result = mix(slice0Color, slice1Color, zOffset);
    return result;
}

void main() {
    vec4 color = texture(tex0, v_texCoord0);
    vec3 noise = vec3(hash22(v_texCoord0-vec2(seed)), hash22(-v_texCoord0+vec2(seed)).x);
    vec3 graded = sampleAs3DTexture(lookup, min(vec3(1.0), max(vec3(0.0),color.rgb + noise * noiseGain)), 16.0).rgb;
    o_color.rgb = min(vec3(1.0), max(vec3(0.0), graded));
    o_color.a = color.a;
}