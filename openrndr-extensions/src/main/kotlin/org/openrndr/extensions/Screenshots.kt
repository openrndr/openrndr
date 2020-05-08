package org.openrndr.extensions

import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.draw.*
import org.openrndr.extensions.CreateScreenshot.*
import java.io.File
import java.time.LocalDateTime
import mu.KotlinLogging
import org.openrndr.events.Event
import kotlin.math.max

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
        createScreenshot = Named(outputFile)
    }
}

data class ScreenshotEvent(val basename: String)

/**
 * an extension that takes screenshots when [key] (default is spacebar) is pressed
 */
open class Screenshots : Extension {

    /**
     * Event that is triggered just before drawing the contents for the screenshot
     */
    val beforeScreenshot = Event<ScreenshotEvent>()

    /**
     * Event that is triggered after contents have been drawn and the screenshot has been committed to file
     */
    val afterScreenshot = Event<ScreenshotEvent>()

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
     * delays the screenshot for a number of frames.
     * useful to let visuals build up in automated screenshots.
     */

    var delayFrames = 0

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

    private var programRef: Program? = null

    override fun setup(program: Program) {
        programRef = program
        program.keyboard.keyDown.listen {
            if (!it.propagationCancelled) {
                if (it.name == key) {
                    trigger()

                }
            }
        }
    }

    /**
     * Trigger screenshot creation
     */
    fun trigger() {
        createScreenshot = AutoNamed
        programRef?.window?.requestDraw()
    }

    private var filename: String? = null
    override fun beforeDraw(drawer: Drawer, program: Program) {
        fun Int.z(zeroes: Int = 2): String {
            val sv = this.toString()
            var prefix = ""
            for (i in 0 until max(zeroes - sv.length, 0)) {
                prefix += "0"
            }
            return "$prefix$sv"
        }

        if (createScreenshot != None && delayFrames-- <= 0) {
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

            val dt = LocalDateTime.now()
            val basename = program.name.ifBlank { program.window.title.ifBlank { "untitled" } }

            filename = when (val cs = createScreenshot) {
                None -> throw IllegalStateException("")
                AutoNamed -> "${if (folder == null) "" else "$folder/"}$basename-${dt.year.z(4)}-${dt.month.value.z()}-${dt.dayOfMonth.z()}-${dt.hour.z()}.${dt.minute.z()}.${dt.second.z()}.png"
                is Named -> cs.name
            }

            filename?.let {
                beforeScreenshot.trigger(ScreenshotEvent(it.dropLast(4)))
            }

            program.backgroundColor?.let {
                drawer.clear(it)
            }
        }
    }

    override fun afterDraw(drawer: Drawer, program: Program) {
        filename?.let { fn ->
            filename = null
            val targetFile = File(fn)

            drawer.shadeStyle = null
            target?.unbind()

            target?.let {
                drawer.defaults()
                targetFile.parentFile?.let { file ->
                    if (!file.exists()) {
                        file.mkdirs()
                    }
                }

                val resolved = resolved
                if (resolved == null) {
                    it.colorBuffer(0).saveToFile(targetFile, async = async)
                    drawer.image(it.colorBuffer(0), it.colorBuffer(0).bounds, drawer.bounds)
                } else {
                    target?.let { rt ->
                        rt.colorBuffer(0).resolveTo(resolved)
                        resolved.saveToFile(targetFile, async = async)
                        drawer.image(resolved, resolved.bounds, drawer.bounds)
                    }
                }
                logger.info("[Screenshots] saved to: ${targetFile.relativeTo(File("."))}")
                afterScreenshot.trigger(ScreenshotEvent(fn.dropLast(4)))
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