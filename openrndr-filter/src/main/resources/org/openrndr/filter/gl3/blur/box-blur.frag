// openrndr - gl3 - box-blur

#version 330

in vec2 v_texCoord0;
uniform sampler2D tex0;
uniform vec2 blurDirection;

uniform int window;
uniform float sigma;
uniform float gain;
uniform vec4 subtract;
uniform float spread;
out vec4 o_color;
void main() {
    vec2 s = textureSize(tex0, 0).xy;
    s = vec2(1.0/s.x, 1.0/s.y);

    int w = window;

    vec4 sum = vec4(0, 0, 0, 0);
    float weight = 0;
    for (int x = -w; x<= w; ++x) {
        float lw = 1.0;
        sum += texture(tex0, v_texCoord0 + x * blurDirection * s * spread);
        weight += lw;
    }

    o_color = (sum / weight) * gain;
//    o_color.a = 1.0;
}