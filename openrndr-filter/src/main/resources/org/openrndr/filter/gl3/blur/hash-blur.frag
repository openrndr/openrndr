#version 330

// based on Hashed blur by David Hoskins.
// License Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License.


uniform float radius;
in vec2 v_texCoord0;
uniform sampler2D tex0;
uniform float time;
uniform int samples;
uniform float gain;
out vec4 o_color;

#define TAU  6.28318530718

//-------------------------------------------------------------------------------------------
// Use last part of hash function to generate new random radius and angle...
vec2 circularSampling(inout vec2 r) {
    r = fract(r * vec2(33.3983, 43.4427));
    //return r-.5;
    return sqrt(r.x+.001) * vec2(sin(r.y * TAU), cos(r.y * TAU))*.5; // <<=== circular sampling.
}

//-------------------------------------------------------------------------------------------
#define HASHSCALE 443.8975
vec2 hash22(vec2 p) {
	vec3 p3 = fract(vec3(p.xyx) * HASHSCALE);
    p3 += dot(p3, p3.yzx+19.19);
    return fract(vec2((p3.x + p3.y)*p3.z, (p3.x+p3.z)*p3.y));
}

//-------------------------------------------------------------------------------------------
vec4 blur(vec2 uv, float radius) {
    vec2 circle = vec2(radius) * (vec2(1.0) / textureSize(tex0, 0));
	vec2 random = hash22(uv + vec2(time));

	vec4 acc = vec4(0.0);
	for (int i = 0; i < samples; i++) {
		acc += texture(tex0, uv + circle * circularSampling(random));
    }
	return acc / float(samples);
}

//-------------------------------------------------------------------------------------------
void main() {
	vec2 uv = v_texCoord0;
    float radiusSqr = pow(radius, 2.0);
	o_color = blur(uv, radiusSqr);
	o_color.rgb *= gain;
}