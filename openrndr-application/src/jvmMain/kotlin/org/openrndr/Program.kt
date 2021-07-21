package org.openrndr

import kotlinx.coroutines.*
import org.openrndr.shape.*
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
