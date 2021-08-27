package org.openrndr.draw

/**
 * File format used while saving to file
 */
enum class ImageFileFormat(val mimeType: String, val extensions: List<String>) {
    JPG("image/jpeg", listOf("jpg", "jpeg")),
    PNG("image/png", listOf("png")),
    DDS("image/vnd.ms-dds", listOf("dds")),
    EXR("image/x-exr", listOf("exr"));

    companion object {
        fun guessFromExtension(extension: String): ImageFileFormat {
            return when (val candidate = extension.lowercase()) {
                "jpg", "jpeg" -> JPG
                "png" -> PNG
                "dds" -> DDS
                "exr" -> EXR
                else -> throw IllegalArgumentException("unsupported format: \"$candidate\"")
            }
        }
    }
}