package org.openrndr.internal.gl3

import org.openrndr.draw.IOSurfaceImporter

/**
 * Creates an [IOSurfaceImporter] for the current context
 * @return the importer, or null when IOSurfaces can not be imported without copying them
 */
internal expect fun createIOSurfaceImporterGL3(): IOSurfaceImporter?
