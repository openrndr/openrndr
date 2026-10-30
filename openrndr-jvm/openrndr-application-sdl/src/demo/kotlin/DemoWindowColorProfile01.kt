import org.openrndr.application
import org.openrndr.color.ColorRGBa

/**
 * Shows the color profile of the display the window is on, and its HDR headroom. Move the window to another display
 * to see the color profile change. Run with `-Dhdr=true` for an HDR window.
 */
fun main() {
    application {
        configure {
            width = 720
            height = 240
            hdr = System.getProperty("hdr") == "true"
        }
        program {
            fun describe() = listOf(
                "color profile: ${window.colorProfile.name}",
                "linearity: ${window.colorProfile.linearity}",
                "transfer: ${window.colorProfile.transfer}",
                "red ${window.colorProfile.red}, green ${window.colorProfile.green}",
                "blue ${window.colorProfile.blue}, white ${window.colorProfile.white}",
                "HDR headroom: ${window.hdrHeadroom}, SDR white level: ${window.sdrWhiteLevel}"
            )
            describe().forEach { println(it) }

            window.colorProfileChanged.listen {
                println("color profile changed")
                describe().forEach { println(it) }
            }
            window.hdrChanged.listen {
                println("HDR changed: headroom ${window.hdrHeadroom}, SDR white level ${window.sdrWhiteLevel}")
            }

            extend {
                drawer.clear(ColorRGBa.BLACK)
                drawer.fill = ColorRGBa.WHITE
                for ((index, line) in describe().withIndex()) {
                    drawer.text(line, 20.0, 40.0 + index * 30.0)
                }
            }
        }
    }
}
