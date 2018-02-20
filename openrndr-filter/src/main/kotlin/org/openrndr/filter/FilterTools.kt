package org.openrndr.filter

import org.openrndr.draw.codeFromURL
internal class FilterTools

internal fun filterFragmentCode(resourceId:String):String {
    val resource = FilterTools::class.java.getResource("gl3/$resourceId")

    if (resource != null) {
        val url = resource.toExternalForm()
        return codeFromURL(url)
    } else {
        throw RuntimeException("failed to find filter fragment code for $resourceId")
    }

}