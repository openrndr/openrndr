import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.openrndr.awt.programPanel
import org.openrndr.color.ColorRGBa
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import javax.swing.SwingUtilities
import kotlin.math.cos
import kotlin.math.sin

/**
 * The state that the Compose controls share with the program. The program reads it on the render thread of the AWT
 * back-end, so the values are volatile.
 */
object Shared {
    @Volatile
    var speed = 1.0

    @Volatile
    var count = 12

    @Volatile
    var hue = 20.0

    @Volatile
    var clearRequested = false
}

/**
 * An OPENRNDR program in a Compose Desktop window, in a SwingPanel next to Compose controls. The controls change the
 * program, clicks in the program are counted in Compose.
 */
fun main() = application {
    var clicks by remember { mutableStateOf(0) }
    var speed by remember { mutableStateOf(1f) }
    var count by remember { mutableStateOf(12f) }
    var hue by remember { mutableStateOf(20f) }

    Window(
        onCloseRequest = ::exitApplication,
        title = "OPENRNDR in Compose",
        state = rememberWindowState(width = 1100.dp, height = 700.dp)
    ) {
        MaterialTheme {
            Row(Modifier.fillMaxSize()) {
                SwingPanel(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    factory = {
                        programPanel {
                            var angle = 0.0
                            val stamps = mutableListOf<Circle>()
                            mouse.buttonDown.listen {
                                stamps.add(Circle(it.position, 24.0))
                                // the program runs on the render thread, Compose state is changed on the UI thread
                                SwingUtilities.invokeLater { clicks++ }
                            }
                            extend {
                                if (Shared.clearRequested) {
                                    Shared.clearRequested = false
                                    stamps.clear()
                                }
                                angle += Shared.speed / 60.0
                                drawer.clear(ColorRGBa.fromHex("#1c2026"))
                                val color = ColorRGBa.fromHex("#ff6040").toHSVa().shiftHue(Shared.hue).toRGBa()
                                drawer.fill = color.opacify(0.4)
                                drawer.stroke = color
                                drawer.circles(stamps)
                                drawer.stroke = null
                                drawer.fill = color
                                val n = Shared.count
                                val center = Vector2(width / 2.0, height / 2.0)
                                for (i in 0 until n) {
                                    val a = angle + i * 2 * Math.PI / n
                                    drawer.circle(center + Vector2(cos(a), sin(a * 2.0) * 0.5) * (minOf(width, height) * 0.35), 14.0)
                                }
                                drawer.fill = ColorRGBa.WHITE
                                drawer.circle(mouse.position, 6.0)
                            }
                        }
                    }
                )
                Column(Modifier.width(280.dp).fillMaxHeight().padding(16.dp)) {
                    Text("Drawn by OPENRNDR, controlled by Compose", style = MaterialTheme.typography.subtitle1)
                    Spacer(Modifier.height(16.dp))
                    Text("speed ${"%.2f".format(speed)}")
                    Slider(speed, onValueChange = { speed = it; Shared.speed = it.toDouble() }, valueRange = 0f..4f)
                    Text("count ${count.toInt()}")
                    Slider(count, onValueChange = { count = it; Shared.count = it.toInt() }, valueRange = 1f..48f)
                    Text("hue ${hue.toInt()}")
                    Slider(hue, onValueChange = { hue = it; Shared.hue = it.toDouble() }, valueRange = 0f..360f)
                    Spacer(Modifier.height(16.dp))
                    Text("clicks in the program: $clicks")
                    Button(onClick = { Shared.clearRequested = true; clicks = 0 }) {
                        Text("clear")
                    }
                }
            }
        }
    }
}
