package org.openrndr.draw

/**
 * File format used while saving to file
 */
enum class ImageFileFormat(val mimeType: String, val extensions: List<String>) {
    JPG("image/jpeg", listOf("jpg", "jpeg")),
    PNG("image/png", listOf("png")),
    DDS("image/vnd.ms-dds", listOf("dds")),
    EXR("image/x-exr", listOf("exr")),
    HDR("image/vnd.radiance", listOf("hdr"))
    ;

    companion object {
        /**
         * Guess the image file format from extension
         */
        fun guessFromExtension(extension: String?): ImageFileFormat? {
            return when (extension?.lowercase()) {
                "jpg", "jpeg" -> JPG
                "png" -> PNG
                "dds" -> DDS
                "exr" -> EXR
                "hdr" -> HDR
                else -> null
            }
        }
    }
}