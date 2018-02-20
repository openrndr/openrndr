#version 150

in vec2 v_texCoord0;
uniform sampler2D tex0;
uniform sampler2D velocity;

uniform int window;
uniform vec4 subtract;
out vec4 o_color;

float rand(vec2 co){
  return fract(sin(dot(co.xy ,vec2(12.9898,78.233))) * 43758.5453);
}
void main() {

    vec2 s = textureSize(tex0, 0).xy;
    s = vec2(1.0/s.x, 1.0/s.y);


    vec2 blurDirection = texture(velocity, v_texCoord0).xy; //vec2(2.0,cos(v_texCoord0.x*10.0));//texture(tex1, v_texCoord0).xy * 4.0;

    float blurMagnitude = length(blurDirection);

    if (blurMagnitude > 10.0) {
        blurDirection = (blurDirection/blurMagnitude) * 10.0;

    }
    int w = window;

    vec4 sum = vec4(0,0,0,0);
    float weight = 0;
    for (int x = -w; x<= w; ++x) {
        float lw = 1.0;

        vec2 r = 2.0 * vec2(rand(v_texCoord0+vec2(w,0.3)),rand(v_texCoord0+vec2(0.0,w))) - vec2(1,1);

        sum+= texture(tex0, v_texCoord0 + x * blurDirection * s);
        weight+=lw;
    }

    o_color = (sum / weight);// * amplitude;

}