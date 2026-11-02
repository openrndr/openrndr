package org.openrndr.internal.gl3

import org.openrndr.draw.DmaBufImporter

/**
 * Creates a [DmaBufImporter] for the current context
 * @return the importer, or null when DMA-BUFs can not be imported without copying them
 */
internal expect fun createDmaBufImporterGL3(): DmaBufImporter?
