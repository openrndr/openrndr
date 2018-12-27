package org.openrndr.extensions.drawrate

import kotlinx.coroutines.*
import org.openrndr.*

/**
 * Special case of delay duration i.e. minimum value
 */
const val MIN_DELAY = 1L
/**
 * Special case of delay duration i.e. maximum value
 */
const val MAX_DELAY = Long.MAX_VALUE

/**
 * Configuration data
 *
 * @isEnable feature is enabled(ON) if true, disabled(OFF) if false
 * @duration ranges from [MIN_DELAY] to [MAX_DELAY]
 */
sealed class DrawRateConfig(var isEnable: Boolean = false, var duration: Long = MIN_DELAY)

/**
 * default configuration for `DRAWRATE`
 */
object DrawConfig : DrawRateConfig()

/**
 * default configuration for `MINIMISE`
 */
object MinimiseConfig : DrawRateConfig()

/**
 * default configuration for `UNFOCUS`
 */
object UnfocusConfig : DrawRateConfig()

/**
 * This configurable feature supports creative use of the Draw() loop.
 * It also has a utilitarian purpose in that it enables a high-level means of managing consumption
 * of system resources (CPU, GPU, Memory).
 *
 * Blurb on `Presentation Control` from the Guide:
 * _The default mode is automatic presentation, the draw method is called as often as possible.
 * The other mode is manual presentation, in which it is the developer's responsibility to request draw to be called._
 *
 * This feature supports finer control between [PresentationMode.AUTOMATIC] and [PresentationMode.MANUAL].
 * It has a base [DrawConfig] that applies to the draw loop generally.
 * Additionally, special cases [MinimiseConfig] and [UnfocusConfig] are currently supported.
 *
 * NB! Refrain from using presentation control modes `AUTOMATIC` and `MANUAL` within your app
 * when this feature is `enabled` because it may cause contra-indications.
 *
 * @see [PresentationMode]
 */
class DrawRate : Extension {
    override var enabled = true

    // Create a local coroutine scope
    private val job = Job()
    private val cs = CoroutineScope(Dispatchers.Default + job)

    /**
     * Initialise the DrawRate() Extension for the supported cases
     * 1. The general draw loop frequency i.e. `DRAWRATE` [DrawConfig]
     * 2. The special case window event i.e. `MINIMISE` [MinimiseConfig]
     * 3. The special case window event i.e. `UNFOCUSED` [UnfocusConfig]
     */
    override fun setup(program: Program) {
        // 1.a setup the general DRAWRATE feature
        if (DrawConfig.isEnable) {
            program.window.presentationMode = PresentationMode.MANUAL
            cs.launch {
                pulse(program, DrawConfig)
            }
        }

        // 1.b setup keyboard ON/OFF toggle hook for DRAWRATE
        program.keyboard.keyDown.listen {
            if (it.key == KEY_SPACEBAR && KeyboardModifier.CTRL in it.modifiers) {
                if (DrawConfig.isEnable) {
                    job.cancelChildren()
                    program.window.presentationMode = PresentationMode.AUTOMATIC
                } else {
                    program.window.presentationMode = PresentationMode.MANUAL
                    cs.launch {
                        pulse(program, DrawConfig)
                    }
                }
                DrawConfig.isEnable = !DrawConfig.isEnable
            }
        }

        // 2.a setup special case #1 'MINIMISE'
        program.window.minimized.listen {
            if (MinimiseConfig.isEnable) {
                job.cancelChildren()
                program.window.presentationMode = PresentationMode.MANUAL
                cs.launch {
                    pulse(program, MinimiseConfig)
                }
            }
        }

        // 2.b setup special case #1 'MINIMISE' - `RESTORE` state prior to 'MINIMISE'
        program.window.restored.listen {
            if (MinimiseConfig.isEnable) {
                job.cancelChildren()
                program.window.presentationMode = PresentationMode.AUTOMATIC
                if (DrawConfig.isEnable) {
                    program.window.presentationMode = PresentationMode.MANUAL
                    cs.launch {
                        pulse(program, DrawConfig)
                    }
                }
            }
        }

        // 3.a setup special case #2  'UNFOCUSED'
        program.window.unfocused.listen {
            if (UnfocusConfig.isEnable) {
                job.cancelChildren()
                program.window.presentationMode = PresentationMode.MANUAL
                cs.launch {
                    pulse(program, UnfocusConfig)
                }
            }
        }

        // 3.b setup special case #2  'UNFOCUSED' - Restore state prior to 'UNFOCUS'
        program.window.focused.listen {
            if (UnfocusConfig.isEnable) {
                job.cancelChildren()
                program.window.presentationMode = PresentationMode.AUTOMATIC
                if (DrawConfig.isEnable) {
                    program.window.presentationMode = PresentationMode.MANUAL
                    cs.launch {
                        pulse(program, DrawConfig)
                    }
                }
            }
        }
    }

    /**
     * Sends a regular [requestDraw()][Program.Window.requestDraw] in `MANUAL` presentation mode based on a configured delay duration
     *
     * @program core [Program] class
     * @drc [DrawRateConfig] is either [DrawConfig], [MinimiseConfig], or [UnfocusConfig]
     */
    @UseExperimental(InternalCoroutinesApi::class)
    suspend fun pulse(program: Program, drc: DrawRateConfig) {
        while (cs.isActive) {
            //println("${program.seconds} request draw() for $drc after ${drc.duration}L")
            delay(drc.duration)
            program.window.requestDraw()
        }
    }
}
