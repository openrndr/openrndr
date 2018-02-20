#version 330

in vec2 v_texCoord0;
uniform sampler2D tex0;
uniform sampler2D tex1;

out vec4 o_color;
void main() {
    vec4 a = texture(tex0, v_texCoord0);
    vec4 b = texture(tex1, v_texCoord0);

    vec4 c = vec4(
        b.r / (1.0 - a.r),
        b.g / (1.0 - a.g),
        b.b / (1.0 - a.b),

        1.0
        );

     vec4 d = a * (1.0 - b.a) + c * b.a;
     o_color = d;

    o_color.a = 1.0;
}