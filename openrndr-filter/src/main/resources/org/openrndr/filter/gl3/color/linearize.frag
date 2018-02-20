#version 330

in vec2 v_texCoord0;
uniform sampler2D tex0;

out vec4 o_color;
void main() {
    const float t = 0.0404482362771082;
    vec4 c = texture(tex0, v_texCoord0);
    vec3 lin =
        vec3(
        c.r <= t ? c.r / 12.92 :  pow( (c.r+0.055)/1.055, 2.4),
        c.g <= t ? c.g / 12.92 :  pow( (c.g+0.055)/1.055, 2.4),
        c.b <= t ? c.b / 12.92 :  pow( (c.b+0.055)/1.055, 2.4));
    o_color = vec4(lin, c.a);

}
