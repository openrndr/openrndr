#version 330

in vec2 v_texCoord0;
uniform sampler2D tex0;
uniform sampler2D velocity;

uniform int step;
uniform int window;
uniform vec4 subtract;
out vec4 o_color;

void main() {

    vec2 s = textureSize(tex0, 0).xy;
    s = vec2(1.0/s.x, 1.0/s.y);


    vec2 blurDirection = texture(velocity, v_texCoord0).xy; //vec2(2.0,cos(v_texCoord0.x*10.0));//texture(tex1, v_texCoord0).xy * 4.0;

    float blurMagnitude = length(blurDirection);

    if (blurMagnitude > 10.0) {
        blurDirection = (blurDirection/blurMagnitude) * 10.0;

    }
    int w = window;


    //vec4 cur = texture(tex0, v_texCoord0);
    float weight = 0;

    vec4 cur = texture(tex0, v_texCoord0 - blurDirection * s * (1.0+step*0.5));
    cur+= texture(tex0, v_texCoord0 + blurDirection * s * (1.0+step*0.5));

    o_color = cur/2.0;

}