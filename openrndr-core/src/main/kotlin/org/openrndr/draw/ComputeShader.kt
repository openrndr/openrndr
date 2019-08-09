package org.openrndr.draw

import org.openrndr.internal.Driver
import java.io.File

interface ComputeShader {

    companion object {
        fun createFromCode(code: String): ComputeShader = Driver.instance.createComputeShader(code)
        fun fromFile(file: File) : ComputeShader {
            return createFromCode(file.readText())
        }
    }

    fun image(name:String, image:Int, colorBuffer: ColorBuffer)

    fun execute(width: Int, height: Int, depth:Int)
}