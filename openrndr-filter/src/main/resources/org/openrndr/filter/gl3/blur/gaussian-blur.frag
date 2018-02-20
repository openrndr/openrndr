#version 150

in vec2 v_texCoord0;
uniform sampler2D tex0;

uniform int window;
uniform float sigma;
uniform float spread;


out vec4 o_color;
void main() {

    vec2 s = textureSize(tex0, 0).xy;
    s = vec2(1.0/s.x, 1.0/s.y);

    int w = window;

    vec4 sum = vec4(0,0,0,0);
    float weight = 0;
    for (int y = -w; y<= w; ++y) {
        for (int x = -w; x<= w; ++x) {
           float lw = exp(-(x*x+y*y) / (2 * sigma * sigma));
            sum+=texture(tex0, v_texCoord0 + vec2(x,y) * s * spread) * lw;
            weight+=lw;
        }
    }

    o_color = (sum / weight) * gain;
    o_color.a = 1.0;

}