package org.openrndr.internal.gl3

import org.openrndr.WindowMultisample

/**
 * The largest number of samples the renderer of the current context supports, null when there is no current context
 * or it can't be queried
 */
fun currentMaxSamples(): Int? = try {
    glGetInteger(GL_MAX_SAMPLES).takeIf { it > 0 }
} catch (e: IllegalStateException) {
    // there are no capabilities for the current thread
    null
}

/**
 * The message for a window or its context that could not be created. It holds [reason], the error of the windowing
 * library, and when the window asks for more samples than [maxSamples], the largest number of samples the renderer
 * supports, a sample count to use instead. Windows are created with multisampled framebuffers, too many samples is a
 * common reason for failing to create them.
 */
fun windowCreationFailedMessage(
    what: String,
    reason: String?,
    multisample: WindowMultisample,
    maxSamples: Int?,
    configuration: Any
): String {
    val message = StringBuilder("Failed to create $what")
    if (!reason.isNullOrBlank()) {
        message.append(": ").append(reason.trim().removeSuffix("."))
    }
    message.append('.')
    if (multisample is WindowMultisample.SampleCount && multisample.count > 1) {
        val count = multisample.count
        if (maxSamples != null && count > maxSamples) {
            message.append(
                " The window asks for $count samples for multisampling, but the renderer supports at most $maxSamples:" +
                        " use WindowMultisample.SampleCount($maxSamples) or fewer, or WindowMultisample.Disabled."
            )
        } else {
            message.append(
                " The window asks for $count samples for multisampling, which the display may not support:" +
                        " try fewer samples, or WindowMultisample.Disabled."
            )
        }
    }
    message.append(" Window configuration: ").append(configuration)
    return message.toString()
}
