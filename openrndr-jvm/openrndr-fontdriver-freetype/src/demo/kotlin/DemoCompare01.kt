import org.openrndr.fontdriver.stb.FontDriverStbTt
import org.openrndr.utils.buffer.MPPBuffer

fun main() {

    val fontStb = FontDriverStbTt()
    val fontFt = FontDriverFreetype()

    val faceStb = fontStb.loadFace("data/fonts/Platypi-Regular.ttf", 24.0, 1.0)
    val faceFt = fontFt.loadFace("data/fonts/Platypi-Regular.ttf", 24.0, 1.0)



    println("stb ascent: ${faceStb.ascent}")
    println("stb descent: ${faceStb.descent}")
    println("stb linegap: ${faceStb.lineGap}")
    println("stb height: ${faceStb.height}")
    println("stb x height: ${faceStb.xHeight}")

    println("--")
    println("ft ascent: ${faceFt.ascent}")
    println("ft descent: ${faceFt.descent}")
    println("ft linegap: ${faceFt.lineGap}")
    println("ft height: ${faceFt.height}")
    println("ft x height: ${faceFt.xHeight}")


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