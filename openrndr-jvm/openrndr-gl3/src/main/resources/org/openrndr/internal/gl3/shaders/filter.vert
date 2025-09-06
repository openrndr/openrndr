// openrndr - gl3 - filter.vert

#version 330

in vec2 a_texCoord0;
in vec2 a_position;

uniform vec2 targetSize;
uniform vec2 padding;
uniform mat4 projectionMatrix;

out vec2 v_texCoord0;

void main() {
    v_texCoord0 = a_texCoord0;
    vec2 transformed = a_position * (targetSize - 2*padding) + padding;
    gl_Position = projectionMatrix * vec4(transformed, 0.0, 1.0);
}