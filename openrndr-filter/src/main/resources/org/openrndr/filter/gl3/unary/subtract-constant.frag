#version 330

in vec2 v_texCoord0;
uniform sampler2D tex0;
uniform vec4 constant;

out vec4 o_color;
void main() {
    vec4 c = texture(tex0, v_texCoord0);
    o_color = max(vec4(0.0), c - constant);

}
