#version 330

in vec2 v_texCoord0;
uniform sampler2D tex0;
uniform sampler2D tex1;

float blendColorBurn(float base, float blend) {
	return (blend==0.0)?blend:max((1.0-((1.0-base)/blend)),0.0);
}


out vec4 o_color;
void main() {
    vec4 a = texture(tex0, v_texCoord0);
    vec4 b = texture(tex1, v_texCoord0);

    vec3 na = a.a == 0.0 ? vec3(0.0): a.rgb / a.a;
    vec3 nb = b.a == 0.0 ? b.rgb: b.rgb / b.a;

    float minAlpha = min(a.a, b.a);
    float maxAlpha = max(a.a, b.a);

    vec3 ka = mix(vec3(0.0), na.rgb, a.a);
    vec3 kb = mix(vec3(1.0), nb.rgb, b.a);

    vec4 m = vec4(
        blendColorBurn(ka.r, kb.r),
        blendColorBurn(ka.g, kb.g),
        blendColorBurn(ka.b, kb.b),

        1.0
        ) * maxAlpha;

    vec4 l = a;
    l = l * (1.0 - b.a) + b;
    l = l * (1.0 - m.a) + m;
    o_color = l;
    o_color.a = maxAlpha;
}