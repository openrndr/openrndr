#version 330
// --- varyings ---
in vec2 v_texCoord0;

// --- G buffer ---
uniform sampler2D colors;
uniform sampler2D normals;
uniform sampler2D positions;

// --- transforms ---
uniform mat4 projection;

// --- output ---
layout(location = 0) out vec4 o_color;


// --- parameters ---
uniform float jitterGain;
uniform int iterationLimit;
uniform float distanceLimit;
uniform float gain;
uniform float borderWidth;

float distanceSquared(vec2 a, vec2 b) {
    vec2 d = b-a;
    return dot(d,d);
}

#define HASHSCALE 443.8975
vec2 hash22(vec2 p) {
	vec3 p3 = fract(vec3(p.xyx) * HASHSCALE);
    p3 += dot(p3, p3.yzx+19.19);
    return fract(vec2((p3.x + p3.y)*p3.z, (p3.x+p3.z)*p3.y));
}

// this is from http://casual-effects.blogspot.nl/2014/08/screen-space-ray-tracing.html

// Returns true if the ray hit something
bool traceScreenSpaceRay1(
 // Camera-space ray origin, which must be within the view volume
 vec3 csOrig,

 // Unit length camera-space ray direction
 vec3 csDir,

 // A projection matrix that maps to pixel coordinates (not [-1, +1]
 // normalized device coordinates)
 mat4x4 proj,

 // The camera-space Z buffer (all negative values)
 sampler2D csZBuffer,

 // Dimensions of csZBuffer
 vec2 csZBufferSize,

 // Camera space thickness to ascribe to each pixel in the depth buffer
 float zThickness,

 // (Negative number)
 float nearPlaneZ,

 // Step in horizontal or vertical pixels between samples. This is a float
 // because integer math is slow on GPUs, but should be set to an integer >= 1
 float stride,

 // Number between 0 and 1 for how far to bump the ray in stride units
 // to conceal banding artifacts
 float jitter,

 // Maximum number of iterations. Higher gives better images but may be slow
 const float maxSteps,

 // Maximum camera-space distance to trace before returning a miss
 float maxDistance,

 // Pixel coordinates of the first intersection with the scene
 out vec2 hitPixel,

 // Camera space location of the ray hit
 out vec3 hitPoint) {

    // Clip to the near plane
    float rayLength = ((csOrig.z + csDir.z * maxDistance) > nearPlaneZ) ?
        (nearPlaneZ - csOrig.z) / csDir.z : maxDistance;
    vec3 csEndPoint = csOrig + csDir * rayLength;

    // Project into homogeneous clip space
    vec4 H0 = proj * vec4(csOrig, 1.0);
    vec4 H1 = proj * vec4(csEndPoint, 1.0);
    float k0 = 1.0 / H0.w, k1 = 1.0 / H1.w;

    // The interpolated homogeneous version of the camera-space points
    vec3 Q0 = csOrig * k0, Q1 = csEndPoint * k1;

    // Screen-space endpoints
    vec2 P0 = H0.xy * k0, P1 = H1.xy * k1;

    // If the line is degenerate, make it cover at least one pixel
    // to avoid handling zero-pixel extent as a special case later
    P1 += vec2((distanceSquared(P0, P1) < 0.0001) ? 0.01 : 0.0);
    vec2 delta = P1 - P0;

    // Permute so that the primary iteration is in x to collapse
    // all quadrant-specific DDA cases later
    bool permute = false;
    if (abs(delta.x) < abs(delta.y)) {
        // This is a more-vertical line
        permute = true; delta = delta.yx; P0 = P0.yx; P1 = P1.yx;
    }

    float stepDir = sign(delta.x);
    float invdx = stepDir / delta.x;

    // Track the derivatives of Q and k
    vec3  dQ = (Q1 - Q0) * invdx;
    float dk = (k1 - k0) * invdx;
    vec2  dP = vec2(stepDir, delta.y * invdx);

    // Scale derivatives by the desired pixel stride and then
    // offset the starting values by the jitter fraction
    dP *= stride; dQ *= stride; dk *= stride;
    P0 += dP * jitter; Q0 += dQ * jitter; k0 += dk * jitter;

    // Slide P from P0 to P1, (now-homogeneous) Q from Q0 to Q1, k from k0 to k1
    vec3 Q = Q0;

    // Adjust end condition for iteration direction
    float  end = P1.x * stepDir;

    float k = k0, stepCount = 0.0, prevZMaxEstimate = csOrig.z;
    float rayZMin = prevZMaxEstimate, rayZMax = prevZMaxEstimate;
    float sceneZMax = rayZMax + 100;
    for (vec2 P = P0;
         ((P.x * stepDir) <= end) && (stepCount < maxSteps) &&
         ((rayZMax < sceneZMax - zThickness) || (rayZMin > sceneZMax)) &&
          (sceneZMax != 0);
         P += dP, Q.z += dQ.z, k += dk, ++stepCount) {

        rayZMin = prevZMaxEstimate;
        rayZMax = (dQ.z * 0.5 + Q.z) / (dk * 0.5 + k);
        prevZMaxEstimate = rayZMax;
        if (rayZMin > rayZMax) {
           float t = rayZMin; rayZMin = rayZMax; rayZMax = t;
        }

        hitPixel = permute ? P.yx : P;
        // You may need hitPixel.y = csZBufferSize.y - hitPixel.y; here if your vertical axis
        // is different than ours in screen space

        vec4 depthData = texelFetch(csZBuffer, ivec2(hitPixel), 0).zw;

        sceneZMax = depthData.z;
        zThickness = depthData.w;
    }

    // Advance Q based on the number of steps
    Q.xy += dQ.xy * stepCount;
    hitPoint = Q * (1.0 / k);
    return (rayZMax >= sceneZMax - zThickness) && (rayZMin < sceneZMax);
}


void main() {
    vec2 hitPixel = vec2(0.0, 0.0);
    vec3 hitPoint = vec3(0.0, 0.0, 0.0);

    vec2 jitter = abs(hash22(v_texCoord0))*1.0;
    float reflectivity = texture(normals, v_texCoord0).a;

    vec2 ts = vec2(textureSize(normals, 0).xy);
    vec3 viewNormal = normalize(texture(normals, v_texCoord0).xyz);// + (texture(noise, v_texCoord0*0.1).xyz - 0.5) * 0.0;
    vec3 viewPos = texture(positions, v_texCoord0).xyz;
    vec3 reflected = normalize(reflect(normalize(viewPos), normalize(viewNormal)));

    float frontalFade = clamp(-reflected.z,0, 1);
    if ( reflectivity > 0 ) {
        bool hit = traceScreenSpaceRay1(
            viewPos + reflected*(1.0+jitter.y * jitterOriginGain),
            reflected,
            projection,
            positions,
            ts,
            1.0,
            0.0,
            1.0,
            jitter.x,
            distanceLimit,
            iterationLimit,
            hitPixel,
            hitPoint);

        float distanceFade = max( 0.0, (maxDistance -length(hitPoint-viewPos))/ distanceLimit);
        vec4 p = projection * vec4(hitPoint, 1.0);
        float k = 1.0 / p.w;

        vec2 pos = vec2(p.xy*k);
        vec2 ad = vec2(ts/2- abs(pos - ts/2));
        float borderFade = smoothstep(0, borderWidth, min(ad.x, ad.y));

        vec4 reflectedColor = texelFetch(colors, ivec2(p.xy*k), 0);
        float hitFade = hit? 1.0: 0.0;
        o_color = (reflectivity * reflectedColor * hitFade * frontalFade * distanceFade * borderFade * gain);
        o_color.a = 1.0;
    } else {
        o_color = vec4(0.0);
    }
}
