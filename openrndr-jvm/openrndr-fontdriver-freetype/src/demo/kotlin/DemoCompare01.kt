import org.openrndr.fontdriver.stb.FontDriverStbTt
import org.openrndr.utils.buffer.MPPBuffer

fun main() {

    val fontStb = FontDriverStbTt()
    val fontFt = FontDriverFreetype()

    val faceStb = fontStb.loadFace("data/fonts/Platypi-Regular.ttf", 24.0, 1.0)
    val faceFt = fontFt.loadFace("data/fonts/Platypi-Regular.ttf", 24.0, 1.0)



    println("ascent: ${faceStb.ascent}")
    println("descent: ${faceStb.descent}")
    println("linegap: ${faceStb.lineGap}")
    println("height: ${faceStb.height}")

    println("ascent: ${faceFt.ascent}")
    println("descent: ${faceFt.descent}")
    println("linegap: ${faceFt.lineGap}")
    println("height: ${faceFt.height}")

    val glyphStb = faceStb.glyphForCharacter('A')
    val glyphFt = faceFt.glyphForCharacter('A')

    println("stb advance width: ${glyphStb.advanceWidth()}")
    println("ft advance width: ${glyphFt.advanceWidth()}")

    println("stb left side bearing: ${glyphStb.leftSideBearing()}")
    println("ft left side bearing: ${glyphFt.leftSideBearing()}")

    println("stb top side bearing: ${glyphStb.topSideBearing()}")
    println("ft top side bearing: ${glyphFt.topSideBearing()}")

    println("stb bounding box: ${glyphStb.bounds()}")
    println("ft bounding box: ${glyphFt.bounds()}")

    val buffer = MPPBuffer.allocate(256*256)
    glyphStb.rasterize(buffer, 256, true)
    glyphFt.rasterize(buffer, 256, true)

}