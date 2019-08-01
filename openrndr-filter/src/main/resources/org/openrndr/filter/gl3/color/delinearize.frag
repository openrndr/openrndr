#version 330

in vec2 v_texCoord0;
uniform sampler2D tex0;

out vec4 o_color;
void main() {
    const float t = 0.00313066844250063;
    vec4 c = texture(tex0, v_texCoord0);
    if (c.a > 0.0)
    c.rgb /= c.a;
    vec3 del =
        vec3(
        c.r <= t ? c.r * 12.92 : 1.055*pow(c.r,1/2.4)-0.055,
        c.g <= t ? c.g * 12.92 :  1.055*pow(c.g,1/2.4)-0.055,
        c.b <= t ? c.b * 12.92 :  1.055*pow(c.b,1/2.4)-0.055);
    o_color = vec4(del * c.a  , c.a);

}