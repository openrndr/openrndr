#version 330

in vec2 v_texCoord0;
uniform sampler2D tex0;
uniform sampler2D velocity;

uniform int step;
uniform float maxMagnitude;
uniform float magnitudeGain;

out vec4 o_color;

void main() {
    vec2 s = 1.0 / textureSize(tex0, 0).xy;
    vec2 blurDirection = texture(velocity, v_texCoord0).xy;
    float blurMagnitude = length(blurDirection) * magnitudeGain;

    if (blurMagnitude > maxMagnitude) {
        blurDirection = (blurDirection/blurMagnitude) * maxMagnitude;
    }
    vec4 cur = texture(tex0, v_texCoord0 - blurDirection * s * (1.0+step*0.5));
    cur += texture(tex0, v_texCoord0 + blurDirection * s * (1.0+step*0.5));
    o_color = cur/2.0;
}