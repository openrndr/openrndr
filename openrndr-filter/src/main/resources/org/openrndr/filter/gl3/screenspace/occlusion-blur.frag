#version 330
// --- varyings ---
in vec2 v_texCoord0;

// --- GBuffer --
uniform sampler2D occlusion;
uniform sampler2D normals;
uniform sampler2D positions;

layout(location = 0) out vec4 o_color;


float _filter(vec2 pos, int window) {

    float depthRef = texture(positions, pos).z;
    float w = 0.0;

    vec2 step = 1.0 / textureSize(occlusion, 0);

    float sum = 0.0;

    for (int j = -window; j <= window; ++j) {
        for (int i = -window; i<=window; ++i) {
            float o = texture(occlusion, pos + vec2(i,j)*step).r;
            float depth = texture(positions, pos + vec2(i,j)*step).z;
            float lw = smoothstep(0.5, 0.4999, abs(depthRef - depth));
            sum += o * lw;
            w += lw;

        }
    }
    return sum / w;
}


void main() {

    o_color = vec4(_filter(v_texCoord0, 2));
    o_color.a = 1.0;

}