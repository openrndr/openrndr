#version 330

in vec2 v_texCoord0;
uniform sampler2D tex0;
uniform sampler2D tex1;



out vec4 o_color;
void main() {
    vec4 a = texture(tex0, v_texCoord0);
    vec4 b = texture(tex1, v_texCoord0);



    //float ai = dot(vec3(1.0), a.rgb)/3.0;
    //float bi = dot(vec3(1.0), b.rgb)/3.0;


    float ai = max(a.z, max(a.x, a.y));
    float bi = max(b.z, max(b.x, b.y));

    //vec3 f = bi < 0.5? vec3(0.0) : a.rgb;

//    vec3 f = smoothstep(0.5, 0.9, bi) * a.rgb;

    vec3 f = a.rgb - (1.0-b.rgb)*2.0*b.a;

    o_color.rgb = max(vec3(0.0), f) * (1.0) + b.rgb * (1.0-a.a);;

    o_color.a = 1.0; //min(1.0, a.a + b.a);
}