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


float coc(vec2 uv) {
    float eyeZ = -texture(position, uv).z;
    float a = aperture;
    float size = a * abs(1.0 - focalPlane/eyeZ);
    size = floor(clamp(size, minCoc, maxCoc ));
    return size;
}

void main() {
    vec2 step = 1.0 / textureSize(image, 0);


    float size = 0.0;
    float w = 0.0;
    for (int j = -1; j <= 1; ++j) {
        for (int i = -1; i <= 1; ++i) {
            size += coc(v_texCoord0 + step * vec2(i,j));
            w += 1.0;
        }
    }
    size = min(coc(v_texCoord0), size/w);
    vec3 color = texture(image, v_texCoord0).rgb;

    o_output = vec4(color*exposure*size, size);
}