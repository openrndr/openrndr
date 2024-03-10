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
    vec4 c = texture(tex0, v_texCoord0);
#else
    vec4 c = texture2D(tex0, v_texCoord0);
#endif

    vec4 result = c;
#ifndef OR_GL_FRACOLOR
    o_color = result;
#else
    gl_FragColor = result;
#endif
}