import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.draw.ImageFileDetails
import org.openrndr.draw.ImageFileFormat
import org.openrndr.internal.CubemapImageData
import org.openrndr.internal.ImageData
import org.openrndr.internal.ImageDriver
import org.openrndr.internal.ImageSaveConfiguration
import org.openrndr.utils.buffer.MPPBuffer

class ImageDriverBrowser: ImageDriver {
    override fun probeImage(fileOrUrl: String): ImageFileDetails? {
        TODO("Not yet implemented")
    }

    override fun probeImage(
        buffer: MPPBuffer,
        formatHint: ImageFileFormat?,
        name: String?
    ): ImageFileDetails? {
        TODO("Not yet implemented")
    }

    override fun loadImage(
        fileOrUrl: String,
        formatHint: ImageFileFormat?,
        allowSRGB: Boolean,
        details: ImageFileDetails?
    ): ImageData {
        TODO("Not yet implemented")
    }

    override fun loadImage(
        buffer: MPPBuffer,
        name: String?,
        formatHint: ImageFileFormat?,
        allowSRGB: Boolean,
        details: ImageFileDetails?
    ): ImageData {
        TODO("Not yet implemented")
    }

    override fun saveImage(
        imageData: ImageData,
        filename: String,
        configuration: ImageSaveConfiguration
    ) {
        TODO("Not yet implemented")
    }

    override fun imageToDataUrl(
        imageData: ImageData,
        formatHint: ImageFileFormat?
    ): String {
        TODO("Not yet implemented")
    }

    override fun loadCubemapImage(
        fileOrUrl: String,
        formatHint: ImageFileFormat?
    ): CubemapImageData {
        TODO("Not yet implemented")
    }

    override fun loadCubemapImage(
        buffer: MPPBuffer,
        name: String?,
        formatHint: ImageFileFormat?
    ): CubemapImageData {
        TODO("Not yet implemented")
    }

    override fun createImageData(
        width: Int,
        height: Int,
        format: ColorFormat,
        type: ColorType,
        flipV: Boolean,
        buffer: MPPBuffer?
    ): ImageData {
        TODO("Not yet implemented")
    }
}