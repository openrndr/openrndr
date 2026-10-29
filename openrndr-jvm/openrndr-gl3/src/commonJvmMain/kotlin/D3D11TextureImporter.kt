package org.openrndr.internal.gl3

import org.openrndr.draw.D3D11TextureImporter

/**
 * Can the current context import Direct3D 11 textures without copying them?
 */
internal expect fun supportsD3D11TextureImportGL3(): Boolean

/**
 * Creates a [D3D11TextureImporter] for [device], an `ID3D11Device`, in the current context
 * @return the importer, or null when textures of [device] can not be imported
 */
internal expect fun createD3D11TextureImporterGL3(device: Long): D3D11TextureImporter?
