#version 330

in vec2 v_texCoord0;
uniform sampler2D tex0;

uniform int window;
uniform float sigma;

out vec4 o_color;
void main() {

    vec2 s = textureSize(tex0, 0).xy;
    s = vec2(1.0/s.x, 1.0/s.y);

    int w = window;

    float sum = 0.0;
    float c = 1.0 / (2*3.1415926535 * sigma * sigma);
    float weight = 0;

    float coc = texture(tex0, v_texCoord0).a;

    for (int y = -w; y<= w; ++y) {
        for (int x = -w; x<= w; ++x) {
            vec2 tc = v_texCoord0 + vec2(x,y) * s;

            float scoc =texture(tex0, tc).a;
            if (coc>scoc || (x==0 && y==0)) {
                float lw = 1.0; //exp(-(x*x+y*y) / (2 * sigma * sigma));
                sum+= scoc * lw;
                weight+=lw;
            }
        }
    }
    o_color.rgb = texture(tex0, v_texCoord0).rgb;
    o_color.a = sum/weight;
}