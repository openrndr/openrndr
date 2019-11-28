// openrndr - gl3 - filter-triangle.vert

#version 330

in vec2 a_position;

out vec2 v_texCoord0;

void main() {
    v_texCoord0 = a_position * 0.5 + 0.5;
    gl_Position = vec4(a_position, 0.0, 1.0);
}