#version 330

in vec2 v_texCoord0;
uniform sampler2D tex0;
uniform vec4 constant;

out vec4 o_color;
void main() {
    vec2 uv = v_texCoord0;
    uv.y = 1.0 - uv.y;
    o_color = texture(tex0, uv);
}
