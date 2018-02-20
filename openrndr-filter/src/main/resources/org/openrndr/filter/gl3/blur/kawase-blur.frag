#version 330

out vec4 o_color;
in vec2 v_texCoord0;
uniform sampler2D tex0;
uniform int iteration;
uniform float spread;

void main() {
    ivec2 size = textureSize(tex0, 0);
    vec2 pixelSize = vec2(1.0/size.x, 1.0/size.y);
    vec2 halfPixelSize = pixelSize / 2.0f;
    vec2 d = ( pixelSize.xy * vec2( iteration, iteration ) ) + halfPixelSize.xy;
    d *= spread;

    vec4 dec = vec4(2.2);
    vec4 enc = vec4(1.0/2.2);

    vec4 cOut = pow(texture( tex0, v_texCoord0+ vec2(-1,1)*d ), dec);
    cOut += pow(texture( tex0, v_texCoord0 + vec2(1,1)*d), dec);
    cOut += pow(texture( tex0, v_texCoord0 + vec2(1,-1)*d), dec);
    cOut += pow(texture( tex0, v_texCoord0+ vec2(-1,-1)*d ), dec);

    o_color = pow(cOut/4.0, enc);
}