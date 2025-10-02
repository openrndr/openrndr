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