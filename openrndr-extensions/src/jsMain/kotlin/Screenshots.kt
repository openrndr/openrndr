package org.openrndr.extensions

import kotlinx.browser.document
import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.events.Event
import org.openrndr.webgl.ApplicationWebGL
import org.w3c.dom.HTMLAnchorElement


data class ScreenshotEvent(val basename: String)

/**
 * an extension that takes screenshots when [key] (default is spacebar) is pressed
 */
open class Screenshots : Extension {
    /**
     * Event that is triggered just before drawing the contents for the screenshot
     */
    val beforeScreenshot = Event<ScreenshotEvent>("before-screenshot", postpone = false)

    /**
     * Event that is triggered after contents have been drawn and the screenshot has been committed to file
     */
    val afterScreenshot = Event<ScreenshotEvent>("after-screenshot", postpone = false)

    override var enabled: Boolean = true


    /**
     * the key that should be pressed to take a screenshot
     */
    var key: String = " "


    /**
     * override automatic naming for screenshot
     */
    var name: String? = "screenshot"


    private var programRef: Program? = null

    var listenToKeyDownEvent = true


    override fun setup(program: Program) {
        programRef = program

        if (listenToKeyDownEvent) {
            program.keyboard.keyDown.listen {
                if (!it.propagationCancelled) {
                    if (it.name == key) {
                        trigger()
                    }
                }
            }
        }
    }


    /**
     * Trigger screenshot creation
     */
    fun trigger() {
        beforeScreenshot.trigger(ScreenshotEvent("before-screenshot"))
        val link = document.createElement("a") as HTMLAnchorElement
        link.download = "$name.png"
        // TODO: Fix screenshots
//        link.href = (programRef!!.application as ApplicationWebGL).canvas.toDataURL()
        link.click()
        afterScreenshot.trigger(ScreenshotEvent("after-screenshot"))
    }
}