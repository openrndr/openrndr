#version 330

/*
    SSAO fragment shader
*/

uniform vec4 poissonSamples[64];
// --- varyings ---
in vec2 v_texCoord0;

// --- G buffer ---
uniform sampler2D colors;
uniform sampler2D normals;
uniform sampler2D positions;

// --- filter parameters ---
uniform float radius;

// --- transforms ---
uniform mat4 projection;

// --- output ---
layout(location = 0) out vec4 o_color;

#define HASHSCALE 443.8975
vec2 hash22(vec2 p) {
	vec3 p3 = fract(vec3(p.xyx) * HASHSCALE);
    p3 += dot(p3, p3.yzx+19.19);
    return fract(vec2((p3.x + p3.y)*p3.z, (p3.x+p3.z)*p3.y));
}

void main() {

    vec2 j2 =hash22(v_texCoord0);

    vec3 viewNormal = normalize(texture(normals, v_texCoord0).xyz);
    vec4 positionData = texture(positions, v_texCoord0);
    vec3 viewPos = positionData.xyz;
    vec3 reflected = normalize(reflect(normalize(viewPos), normalize(viewNormal)));

    vec3 randomVector = normalize( vec3(j2 * 2.0 -1.0,0));
    vec3 normal = viewNormal;
    vec3 tangent = normalize(randomVector - normal * dot(randomVector, normal));
    vec3 bitangent = normalize(cross(normal, tangent));
    mat3 tbn = mat3(tangent, bitangent, normal);

    float occlusion = 0.0;
    float referenceZ = viewPos.z;

    ivec2 size = textureSize(colors, 0);

    for (int i = 0; i < 64; ++i) {
        vec3 sampleV = viewPos + (radius * tbn * poissonSamples[i].xyz);
        vec4 sampleS = projection * (vec4(sampleV,1));
        vec2 sampleP = sampleS.xy / sampleS.w;
        sampleP.xy += vec2(1.0, 1.0);
        sampleP.xy *= size/2.0;

        float sampleDepth = texelFetch(positions, ivec2(sampleP), 0).z;

        if (sampleP.x >0 && sampleP.x < size.x && sampleP.y > 0 && sampleP.y < size.y) {
            float rangeCheck= abs(viewPos.z - sampleDepth) < radius ? 1.0 : 0.0;
            float d = ((sampleDepth <= sampleV.z) ? 1.0 : 0.0);
            occlusion += d;
        } else {
            occlusion += 1.0;
        }
    }
    occlusion /= 64.0;

    float f = positionData.w >= 0 ? occlusion : (-positionData.w) * 1.0 + (1.0+positionData.w) * occlusion;

    o_color.rgba = vec4(f, f, f, 1.0);
 }