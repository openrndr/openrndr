@file:JvmName("FilterFunctions")
package org.openrndr.draw

import java.net.URL


/**
 * Creates a Shader object by loading and compiling shader code from the provided URL.
 *
 * @param url the URL pointing to the shader code to be loaded and compiled.
 * @return the compiled Shader object created from the provided shader code.
 */
fun filterShaderFromUrl(url: String): Shader {
    return filterShaderFromCode(URL(url).readText(), "filter-shader: $url")
}

/**
 * Creates and returns a ShaderWatcher configured for the given fragment shader URL.
 * The returned ShaderWatcher uses a predefined vertex shader code and the provided URL for the fragment shader.
 *
 * @param url The URL pointing to the fragment shader source code.
 * @return A ShaderWatcher instance configured with the provided fragment shader URL and predefined vertex shader code.
 */
fun filterWatcherFromUrl(url: String): ShaderWatcher {
    return shaderWatcher {
        vertexShaderCode = Filter.filterVertexCode
        fragmentShaderUrl = url
    }
}