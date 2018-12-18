package org.openrndr.extensions

import kotlinx.coroutines.*
import kotlinx.coroutines.NonCancellable.isActive
import org.openrndr.*

/**
 * Special cases of delay duration
 */
const val MIN_DELAY = 1L
const val MAX_DELAY = Long.MAX_VALUE

/**
 * Configuration Data for DrawRate
 *
 * @id any string identifier
 * @isEnable feature is enabled if true(ON), disabled if false(OFF)
 * @duration ranges from MIN_DELAY to MAX_DELAY
 */
data class DrawConfig(val id: String, var isEnable: Boolean = false, var duration: Long = MIN_DELAY)

/**
 * This configurable feature supports creative use of the Draw() loop.
 * It also has a utilitarian purpose in that it enables a high-level means of managing consumption of system resources(CPU, Memory).
 *
 * Blurb on `Presentation Control` from the Guide
 * > The default mode is automatic presentation, the draw method is called as often as possible.
 * The other mode is manual presentation, in which it is the developer's responsibility to request draw to be called.
 *
 * This feature supports finer control between `PresentationMode.AUTOMATIC` and `PresentationMode.MANUAL`.
 * It has a base `DrawRate` config that applies to the draw loop generally.
 * Additionally, further special cases may be configured (currently, special case 'Minimise' is supported).
 *
 * NB! Refrain from using presentation control modes 'AUTOMATIC' and 'MANUAL' within your app when this feature is `enabled` because it may cause contra-indications.
 */
class DrawRate : Extension {
    override var enabled = true

    /**
     * initial setup configuration of 'DrawRate'
     */
    @Volatile
    var drawRate = DrawConfig("DrawRate")
    /**
     * initial setup configuration of 'Minimise'
     */
    @Volatile
    var minimiseRate = DrawConfig("MinimiseRate")

    private lateinit var drawRateJob: Job
    private lateinit var minRateJob: Job
    private var restoreDrawRate = false
    private val cs = CoroutineScope(Dispatchers.Default)

    /**
     * Initialise the DrawRate() Extension - 3 cases are supported
     * 1. The general draw loop frequency
     * 2. The special case window event 'MINIMISE'
     * 3. The special case window event 'UNFOCUS'
     */
    override fun setup(program: Program) {
        // 1.a setup the general drawrate feature
        if (drawRate.isEnable)
            drawRateJob = cs.launch {
                pulse(program, drawRate)
            }

        // 1.b setup keyboard ON/OFF toggle hook for DRAWRATE
        program.keyboard.keyDown.listen {
            if (it.key == KEY_SPACEBAR && KeyboardModifier.CTRL in it.modifiers) {
                if (drawRate.isEnable) {
                    if (::drawRateJob.isInitialized && drawRateJob.isActive)
                        runBlocking {
                            drawRateJob.cancelAndJoin()
                        }
                } else
                    drawRateJob = cs.launch {
                        pulse(program, drawRate)
                    }
                drawRate.isEnable = !drawRate.isEnable
            }
        }

        // 2.a setup special case #1 'MINIMISE'
        program.window.minimized.listen {
            if (minimiseRate.isEnable) {
                if (::drawRateJob.isInitialized && drawRateJob.isActive) {
                    runBlocking {
                        drawRateJob.cancelAndJoin()
                    }
                    restoreDrawRate = true
                }
                minRateJob = cs.launch {
                    pulse(program, minimiseRate)
                }
            }
        }

        // 2.b setup special case #1 'MINIMISE' - Restore state prior to 'Minimise'
        program.window.restored.listen {
            if (minimiseRate.isEnable) {
                if (::minRateJob.isInitialized && minRateJob.isActive)
                    runBlocking {
                        minRateJob.cancelAndJoin()
                    }
                if (restoreDrawRate) {
                    restoreDrawRate = false
                    drawRateJob = cs.launch {
                        pulse(program, drawRate)
                    }
                }
            }
        }

        // 3. setup special case #2  'UNFOCUSED'
        /* under discussion - deferred to next iteration
        program.window.unfocused.listen {
            println("UNFOCUSED:")
        }
        program.window.focused.listen {
            println("FOCUSED:")
        }
        */
    }

    /**
     * Sends a regular requestDraw() in 'MANUAL' presentation mode based on a configured delay duration
     *
     * @program OPENRNDR core
     * @dc draw configuration[DrawConfig]]
     */
    @UseExperimental(InternalCoroutinesApi::class)
    suspend fun pulse(program: Program, dc: DrawConfig) {
        program.window.presentationMode = PresentationMode.MANUAL
        try { // check for cancel exception
            while (isActive) {
                delay(dc.duration)
                program.window.requestDraw()
                //println("request draw() for ${dc.id} after ${dc.duration}L")
            }
        } finally {
            program.window.presentationMode = PresentationMode.AUTOMATIC
        }
    }
}
