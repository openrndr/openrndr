package org.openrndr.filter

import org.openrndr.draw.Shader
import org.openrndr.draw.filterShaderFromCode
import org.openrndr.internal.Driver

internal class FilterTools

fun mppFilterShader(code: String, name: String) : Shader =
    filterShaderFromCode("${Driver.instance.shaderConfiguration()}\n${code}", name)
