package org.openrndr

import kotlinx.coroutines.*
import org.openrndr.draw.Writer
import org.openrndr.draw.writer as writerFunc
import org.openrndr.shape.*
import java.time.LocalDateTime
import kotlin.coroutines.CoroutineContext

/**
 * launch a coroutine in the [Program] context
 */
@Suppress("EXPERIMENTAL_API_USAGE")
fun Program.launch(
    context: CoroutineContext = dispatcher,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
): Job = GlobalScope.launch(context, start, block)

// Derives Composition dimensions from current Drawer
fun Program.drawComposition(
    documentBounds: CompositionDimensions = CompositionDimensions(0.0.pixels, 0.0.pixels, this.drawer.width.toDouble().pixels, this.drawer.height.toDouble().pixels),
    composition: Composition? = null,
    cursor: GroupNode? = composition?.root as? GroupNode,
    drawFunction: CompositionDrawer.() -> Unit
): Composition = CompositionDrawer(documentBounds, composition, cursor).apply { drawFunction() }.composition

fun Program.drawComposition(
    documentBounds: Rectangle,
    composition: Composition? = null,
    cursor: GroupNode? = composition?.root as? GroupNode,
    drawFunction: CompositionDrawer.() -> Unit
): Composition = CompositionDrawer(CompositionDimensions(documentBounds), composition, cursor).apply { drawFunction() }.composition

actual fun Program.namedTimestamp(extension: String, path: String?):
        String {
    val now = LocalDateTime.now()
    val basename = this.name.ifBlank { this.window.title.ifBlank { "untitled" } }
    val computedPath = when {
        path.isNullOrBlank() -> ""
        path.endsWith("/") -> path
        else -> "$path/"
    }
    val ext = when {
        extension.isEmpty() -> ""
        extension.startsWith(".") -> extension
        else -> ".$extension"
    }

    return "$computedPath$basename-%04d-%02d-%02d-%02d.%02d.%02d$ext".format(
        now.year, now.month.value, now.dayOfMonth,
        now.hour, now.minute, now.second)
}

fun <T> Program.writer(f: Writer.() -> T): T {
    return writerFunc(drawer, f)
}
