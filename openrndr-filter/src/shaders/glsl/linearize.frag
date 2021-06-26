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
    const float t = 0.0404482362771082;
    #ifndef OR_GL_TEXTURE2D
    vec4 c = texture(tex0, v_texCoord0);
    #else
    vec4 c = texture2D(tex0, v_texCoord0);
    #endif
    if (c.a > 0.0) {
        c.rgb /= c.a;
    }
    vec3 lin =
        vec3(
        c.r <= t ? c.r / 12.92 : pow((c.r + 0.055) / 1.055, 2.4),
        c.g <= t ? c.g / 12.92 : pow((c.g + 0.055) / 1.055, 2.4),
        c.b <= t ? c.b / 12.92 : pow((c.b + 0.055) / 1.055, 2.4));

    vec4 result = vec4(lin * c.a, c.a);
    #ifndef OR_GL_FRACOLOR
    o_color = result;
    #else
    gl_FragColor = result;
    #endif
}
