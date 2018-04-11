package org.openrndr.filter

import org.openrndr.draw.Filter
import org.openrndr.draw.Shader
import org.openrndr.resourceUrl
import java.net.URL

internal class FilterTools

internal fun filterFragmentCode(resourceId: String): String {
    val urlString = resourceUrl("gl3/$resourceId", FilterTools::class.java)
    return URL(urlString).readText()
}

fun filterShaderFromUrl(url: String): Shader {
    return filterShaderFromCode(URL(url).readText())
}

fun filterShaderFromCode(fragmentShaderCode: String): Shader {
    return Shader.createFromCode(Filter.filterVertexCode, fragmentShaderCode)
}