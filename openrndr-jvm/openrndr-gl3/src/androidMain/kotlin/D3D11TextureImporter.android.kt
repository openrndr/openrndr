package org.openrndr.internal.gl3

import org.openrndr.draw.D3D11TextureImporter

internal actual fun supportsD3D11TextureImportGL3(): Boolean = false

internal actual fun createD3D11TextureImporterGL3(device: Long): D3D11TextureImporter? = null
