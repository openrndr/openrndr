import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.internal.Driver

/**
 * Demonstration of `drawThread`
 */
fun main() = application {
    program {
        // create a draw thread
        val dt = drawThread()

        val cb = colorBuffer(width, height)

        // naive guard
        var finished = false


        dt.launch {
            // note that render targets should be made on the thread that uses it as active target
            val rt = renderTarget(width, height) {
                // here we attach the color buffer that we have made on the main draw thread
                colorBuffer(cb)
            }

            // note that we use dt.drawer here and not the drawer on the main thread
            // using the main thread's drawer will result in undefined behavior
            dt.drawer.isolatedWithTarget(rt) {
                dt.drawer.ortho(rt)
                dt.drawer.clear(ColorRGBa.PINK)
                dt.drawer.fill = ColorRGBa.WHITE
                dt.drawer.stroke = ColorRGBa.BLACK
                dt.drawer.circle(320.0, 240.0, 200.0)
            }

            // here we use low-level functionality to flush out the GPU
            Driver.instance.finish()

            // clean up the render target we created
            rt.detachColorAttachments()
            rt.destroy()

            // unlock the result
            finished = true
        }

        extend {
            if (finished) {
                drawer.image(cb)
            }
        }
    }
}