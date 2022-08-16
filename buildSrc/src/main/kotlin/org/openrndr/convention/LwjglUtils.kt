package org.openrndr.convention

fun JvmNativeVariant.mapToLwjglTargetName(): String = when (targetName) {
    "natives-linux-arm64" -> "natives-linux-arm64"
    "natives-linux-x64" -> "natives-linux"
    "natives-macos-arm64" -> "natives-macos-arm64"
    "natives-macos" -> "natives-macos"
    "natives-windows" -> "natives-windows"
    else -> throw IllegalArgumentException("No match for target: $targetName")
}

object LwjglModules {
    /**
     * Excluding `lwjgl-egl` for not having any variants.
     */
    val all: List<String>
        get() = listOf(
            "lwjgl",
            "lwjgl-assimp",
            "lwjgl-bgfx",
            "lwjgl-bom",
            "lwjgl-cuda",
            "lwjgl-glfw",
            "lwjgl-jawt",
            "lwjgl-jemalloc",
            "lwjgl-libdivide",
            "lwjgl-llvm",
            "lwjgl-lmdb",
            "lwjgl-lz4",
            "lwjgl-meow",
            "lwjgl-meshoptimizer",
            "lwjgl-nanovg",
            "lwjgl-nfd",
            "lwjgl-nuklear",
            "lwjgl-odbc",
            "lwjgl-openal",
            "lwjgl-opencl",
            "lwjgl-opengl",
            "lwjgl-opengles",
            "lwjgl-openvr",
            "lwjgl-openxr",
            "lwjgl-opus",
            "lwjgl-ovr",
            "lwjgl-par",
            "lwjgl-platform",
            "lwjgl-remotery",
            "lwjgl-rpmalloc",
            "lwjgl-shaderc",
            "lwjgl-spvc",
            "lwjgl-sse",
            "lwjgl-stb",
            "lwjgl-tinyexr",
            "lwjgl-tinyfd",
            "lwjgl-tootle",
            "lwjgl-vma",
            "lwjgl-vulkan",
            "lwjgl-xxhash",
            "lwjgl-yoga",
            "lwjgl-zstd"
        )
}