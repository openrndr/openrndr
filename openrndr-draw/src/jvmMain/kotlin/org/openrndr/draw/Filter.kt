@file:JvmName("FilterFunctions")
package org.openrndr.draw

import java.net.URL


fun filterShaderFromUrl(url: String): Shader {
    return filterShaderFromCode(URL(url).readText(), "filter-shader: $url")
}

fun filterWatcherFromUrl(url: String): ShaderWatcher {
    return shaderWatcher {
        vertexShaderCode = Filter.filterVertexCode
        fragmentShaderUrl = url
    }
}