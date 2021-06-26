#ifdef OR_IN_OUT
in vec2 v_texCoord0;
#else
varying vec2 v_texCoord0;
#endif

uniform sampler2D tex0;

#ifndef OR_GL_FRAGCOLOR
out vec4 o_color;
#endif

void main() {
    #ifndef OR_GL_TEXTURE2D
    vec4 e = texture(tex0, v_texCoord0) / 12.0;
    #else
    vec4 e = texture2D(tex0, v_texCoord0) / 12.0;
    #endif
    vec3 hlg = vec3(0.0);
    e.rgb = max(vec3(0.0), e.rgb);

    float a = 0.17883277;
    float b = 0.28466892;
    float c = 0.55991073;
    float r = 0.5;

    hlg.r = e.r <= 1.0 ? sqrt(e.r) * r : a * log(e.r - b) + c;
    hlg.g = e.g <= 1.0 ? sqrt(e.g) * r : a * log(e.g - b) + c;
    hlg.b = e.b <= 1.0 ? sqrt(e.b) * r : a * log(e.b - b) + c;

    vec4 result = vec4(hlg, e.a);

    #ifndef OR_GL_FRACOLOR
    o_color = result;
    #else
    gl_FragColor = result;
    #endif
}