#version 330

in vec2 v_texCoord0;
uniform sampler2D tex0;
uniform sampler2D tex1;



out vec4 o_color;
void main() {
    vec4 a = texture(tex0, v_texCoord0);
    vec4 b = texture(tex1, v_texCoord0);

    vec4 c = vec4(
        a.r * b.r, a.g * b.g, a.b * b.b, 1.0
        );


    o_color = c;

    o_color.a = a.a; //min(1.0, a.a);
}