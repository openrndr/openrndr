package org.openrndr.internal.gl3

import org.lwjgl.opengl.ARBTextureCompressionBPTC
import org.lwjgl.opengl.EXTTextureCompressionS3TC
import org.lwjgl.opengl.GL33C.*
import org.openrndr.draw.DepthFormat
import org.openrndr.internal.Driver

class GL3Exception(message: String) : Exception(message)

fun glEnumName(enum: Int): String {
    return when (enum) {
        GL_TEXTURE_2D -> "GL_TEXTURE_2D"
        GL_TEXTURE_3D -> "GL_TEXTURE_3D"
        GL_TEXTURE_CUBE_MAP -> "GL_TEXTURE_CUBE_MAP"
        GL_TEXTURE_CUBE_MAP_POSITIVE_X -> "GL_TEXTURE_CUBE_MAP_POSITIVE_X"
        GL_TEXTURE_CUBE_MAP_POSITIVE_Y -> "GL_TEXTURE_CUBE_MAP_POSITIVE_Y"
        GL_TEXTURE_CUBE_MAP_POSITIVE_Z -> "GL_TEXTURE_CUBE_MAP_POSITIVE_Z"
        GL_TEXTURE_CUBE_MAP_NEGATIVE_X -> "GL_TEXTURE_CUBE_MAP_NEGATIVE_X"
        GL_TEXTURE_CUBE_MAP_NEGATIVE_Y -> "GL_TEXTURE_CUBE_MAP_NEGATIVE_Y"
        GL_TEXTURE_CUBE_MAP_NEGATIVE_Z -> "GL_TEXTURE_CUBE_MAP_NEGATIVE_Z"
        GL_RGB -> "GL_RGB"
        GL_RGBA -> "GL_RGBA"
        GL_RGB8 -> "GL_RGB8"
        GL_BGR -> "GL_BGR"
        GL_UNSIGNED_BYTE -> "GL_UNSIGNED_BYTE"
        GL_BYTE -> "GL_BYTE"
        GL_UNSIGNED_SHORT -> "GL_UNSIGNED_SHORT"
        GL_SHORT ->  "GL_SHORT"
        GL_UNSIGNED_INT -> "GL_UNSIGNED_INT"
        GL_INT -> "GL_INT"
        GL_HALF_FLOAT -> "GL_HALF_FLOAT"
        GL_FLOAT -> "GL_FLOAT"
        EXTTextureCompressionS3TC.GL_COMPRESSED_RGBA_S3TC_DXT1_EXT -> "GL_COMPRESSED_RGBA_S3TC_DXT1_EXT"
        EXTTextureCompressionS3TC.GL_COMPRESSED_RGBA_S3TC_DXT3_EXT -> "GL_COMPRESSED_RGBA_S3TC_DXT3_EXT"
        EXTTextureCompressionS3TC.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT -> "GL_COMPRESSED_RGBA_S3TC_DXT5_EXT"
        ARBTextureCompressionBPTC.GL_COMPRESSED_RGBA_BPTC_UNORM_ARB -> "GL_COMPRESSED_RGBA_BPTC_UNORM_ARB"
        GL_DEPTH_COMPONENT16 -> "GL_DEPTH_COMPONENT16"
        GL_DEPTH_COMPONENT24 -> "GL_DEPTH_COMPONENT24"
        GL_DEPTH_COMPONENT32 -> "GL_DEPTH_COMPONENT32"
        GL_DEPTH_COMPONENT32F -> "GL_DEPTH_COMPONENT32F"
        GL_DEPTH32F_STENCIL8 -> "GL_DEPTH32F_STENCIL8"
        GL_DEPTH_STENCIL -> "GL_DEPTH_STENCIL"
        GL_STENCIL_INDEX -> "GL_STENCIL_INDEX"
        GL_STENCIL_INDEX8 -> "GL_STENCIL_INDEX8"
        else -> "[$enum]"
    }
}


inline fun checkGLErrors(crossinline errorFunction: ((Int) -> String?) = { null }) {
    val error = glGetError()
    if (error != GL_NO_ERROR) {
        val message = when (error) {
            GL_INVALID_OPERATION -> "GL_INVALID_OPERATION"
            GL_INVALID_VALUE -> "GL_INVALID_VALUE"
            GL_INVALID_ENUM -> "GL_INVALID_ENUM"
            GL_INVALID_FRAMEBUFFER_OPERATION -> "GL_INVALID_FRAMEBUFFER_OPERATION"
            GL_OUT_OF_MEMORY -> "GL_OUT_OF_MEMORY"
            GL_STACK_UNDERFLOW -> "GL_STACK_UNDERFLOW"
            GL_STACK_OVERFLOW -> "GL_STACK_OVERFLOW"
            else -> "<untranslated: $error>"
        }
        throw GL3Exception("[context=${Driver.instance.contextID}] GL ERROR: $message ${errorFunction.invoke(error)}")
    }
}

inline fun debugGLErrors(crossinline errorFunction: ((Int) -> String?) = { null }) {
    if (DriverGL3Configuration.useDebugContext) {
        checkGLErrors(errorFunction)
    }
}