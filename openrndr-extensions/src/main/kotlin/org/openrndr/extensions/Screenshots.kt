package org.openrndr.extensions

import org.openrndr.Extension
import org.openrndr.KEY_SPACEBAR
import org.openrndr.Program
import org.openrndr.draw.*
import org.openrndr.extensions.CreateScreenshot.*
import org.openrndr.math.Matrix44
import java.io.File
import java.time.LocalDateTime
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

internal sealed class CreateScreenshot {
    object None : CreateScreenshot()
    object AutoNamed : CreateScreenshot()
    data class Named(val name: String) : CreateScreenshot()
}

/**
 * specialized version of the [Screenshots] extension that takes a single screenshot and exits
 */
class SingleScreenshot : Screenshots() {
    init {
        quitAfterScreenshot = true
        async = false
        folder = null
    }

    /**
     * filename of the screenshot output
     */
    var outputFile = "screenshot.png"

    override fun setup(program: Program) {
        createScreenshot = CreateScreenshot.Named(outputFile)
    }
}

/**
 * an extension that takes screenshots when [key] (default is spacebar) is pressed
 */
open class Screenshots : Extension {
    override var enabled: Boolean = true

    /**
     * scale can be se to be greater than 1.0 for higher resolution screenshots
     */
    var scale = 1.0

    /**
     * should saving be performed asynchronously?
     */
    var async: Boolean = true

    /**
     * multisample settings
     */
    var multisample: BufferMultisample = BufferMultisample.Disabled

    /**
     * should the program quit after taking a screenshot?
     */
    var quitAfterScreenshot = false

    /**
     * the key that should be pressed to take a screenshot
     */
    var key: String = "space"

    /**
     * the folder where the screenshot will be saved to. Default value is "screenshots", saves in current working
     * directory when set to null.
     */
    var folder: String? = "screenshots"

    internal var createScreenshot: CreateScreenshot = None

    private var target: RenderTarget? = null
    private var resolved: ColorBuffer? = null

    override fun setup(program: Program) {
        program.keyboard.keyDown.listen {
            if (it.name == key) {
                createScreenshot = AutoNamed
                program.window.requestDraw()
            }
        }
    }

    override fun beforeDraw(drawer: Drawer, program: Program) {
        if (createScreenshot != None) {
            val targetWidth = (program.width * scale).toInt()
            val targetHeight = (program.height * scale).toInt()

            target = renderTarget(targetWidth, targetHeight, multisample = multisample) {
                colorBuffer()
                depthBuffer()
            }
            resolved = when (multisample) {
                BufferMultisample.Disabled -> null
                is BufferMultisample.SampleCount -> colorBuffer(targetWidth, targetHeight)
            }
            target?.bind()

            program.backgroundColor?.let {
                drawer.background(it)
            }
        }
    }

    override fun afterDraw(drawer: Drawer, program: Program) {
        fun Int.z(zeroes: Int = 2): String {
            val sv = this.toString()
            var prefix = ""
            for (i in 0 until Math.max(zeroes - sv.length, 0)) {
                prefix += "0"
            }
            return "$prefix$sv"
        }

        val createScreenshot = createScreenshot

        if (createScreenshot != None) {
            drawer.shadeStyle = null
            target?.unbind()

            target?.let {
                drawer.defaults()

                val dt = LocalDateTime.now()
                val basename = program.javaClass.simpleName.ifBlank { program.window.title.ifBlank { "untitled" } }

                val filename = when (createScreenshot) {
                    None -> throw IllegalStateException("")
                    AutoNamed -> "${if(folder==null)"" else "$folder/"}$basename-${dt.year.z(4)}-${dt.month.value.z()}-${dt.dayOfMonth.z()}-${dt.hour.z()}.${dt.minute.z()}.${dt.second.z()}.png"
                    is Named -> createScreenshot.name
                }

                File(filename).parentFile.let { file ->
                    if (!file.exists()) {
                        file.mkdirs()
                    }
                }

                val resolved = resolved

                if (resolved == null) {
                    it.colorBuffer(0).saveToFile(File(filename))

                    drawer.image(it.colorBuffer(0), it.colorBuffer(0).bounds, drawer.bounds)
                } else {
                    target?.let { rt ->
                        rt.colorBuffer(0).resolveTo(resolved)

                        resolved.saveToFile(File(filename))

                        drawer.image(resolved, resolved.bounds, drawer.bounds)
                    }
                }

                logger.info("[Screenshots] saved to: $filename")
            }

            target?.destroy()
            resolved?.destroy()
            this.createScreenshot = None

            if (quitAfterScreenshot) {
                program.application.exit()
            }
        }
    }
}