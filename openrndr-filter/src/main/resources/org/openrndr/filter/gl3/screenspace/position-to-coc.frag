#version 330

layout(location=0) out vec4 o_output;

uniform sampler2D image;
uniform sampler2D position;
in vec2 v_texCoord0;

uniform float minCoc;
uniform float maxCoc;
uniform float focalPlane;
uniform float zFar;
uniform float zNear;
uniform float focalLength;
uniform float aperture;
uniform float exposure;

#define HASHSCALE 443.8975
vec2 hash22(vec2 p) {
	vec3 p3 = fract(vec3(p.xyx) * HASHSCALE);
    p3 += dot(p3, p3.yzx+19.19);
    return fract(vec2((p3.x + p3.y)*p3.z, (p3.x+p3.z)*p3.y));
}


void main() {

    vec2 noise = hash22(v_texCoord0);
    vec3 color = texture(image, v_texCoord0).rgb;
    float eyeZ = -texture(position, v_texCoord0).z;
    float z = (eyeZ - zNear) / (zFar-zNear);

    float a = aperture;
    float cocScale = (a * focalLength * focalPlane * (zFar - zNear)) / ((focalPlane-focalLength)*zNear*zFar);
    float cocBias = (a * focalLength * (zNear - focalPlane)) / ((focalPlane *focalLength) * zNear);

    float size = abs(z * cocScale + cocBias);
    size = clamp(size, minCoc, maxCoc );

    o_output = vec4(color*exposure*size, size);
}