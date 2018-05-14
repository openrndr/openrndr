#version 330
in vec2 v_texCoord0;
uniform sampler2D tex0;
uniform sampler2D lookup;
out vec4 o_color;

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
    vec3 graded = sampleAs3DTexture(lookup, min(vec3(1.0), max(vec3(0.0),color.rgb)), 16.0).rgb;
    o_color.rgb = min(vec3(1.0), max(vec3(0.0), graded));
    o_color.a = color.a;
}