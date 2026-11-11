package org.openrndr.math

import java.lang.management.ManagementFactory
import java.lang.Math.fma as fma_

/**
 * Does the processor have fused multiply-add instructions? Without them `Math.fma` is computed in software, about a
 * thousand times slower than a multiplication and an addition. HotSpot enables its `UseFMA` option only when the
 * processor has the instructions, other virtual machines are assumed to have them.
 */
@PublishedApi
internal object HardwareFma {
    @JvmField
    val available: Boolean = try {
        ManagementFactory.getPlatformMXBean(com.sun.management.HotSpotDiagnosticMXBean::class.java)
            .getVMOption("UseFMA").value == "true"
    } catch (e: Throwable) {
        true
    }
}

actual inline fun fma(a: Double, b: Double, c: Double): Double =
    if (HardwareFma.available) fma_(a, b, c) else a * b + c
