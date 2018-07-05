package org.openrndr.draw

import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchKey
import kotlin.concurrent.thread


internal val watching = mutableMapOf<Path, MutableList<ShaderWatcher>>()
internal val pathKeys = mutableMapOf<Path, WatchKey>()
internal val keyPaths = mutableMapOf<WatchKey, Path>()

internal val watchService by lazy {
    FileSystems.getDefault().newWatchService()
}

internal val watchThread by lazy {
    thread {
        println("starting service")
        while (true) {
            val key = watchService.take()
            val path = keyPaths[key]
            key.pollEvents().forEach {
                val contextPath = it.context() as Path
                val fullPath = path?.resolve(contextPath)
                watching[fullPath]?.forEach {
                    println("informing $it of change in $fullPath")
                    it.changed = true
                }
            }
            key.reset()
        }
    }
}


class ShaderWatcher private constructor(val vsUrlString: String, val fsUrlString: String, var currentShader: Shader) {

    internal var changed = false
    companion object {
        fun fromFiles(vsUrlString: String, fsUrlString: String): ShaderWatcher {
            val vsFile = File(vsUrlString)
            val fsFile = File(fsUrlString)
            val vsPath = vsFile.toPath()
            val fsPath = fsFile.toPath()
            val vsParent = vsPath.parent
            val fsParent = fsPath.parent

            val watcher = ShaderWatcher(vsFile.toURL().toExternalForm(), fsFile.toURL().toExternalForm(), Shader.createFromUrls(vsFile.toURL().toExternalForm(), fsFile.toURL().toExternalForm()))
            listOf(vsParent, fsParent).forEach {
                val key = pathKeys.getOrPut(it) {
                    it.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY)
                }
                keyPaths.getOrPut(key) { it }

            }
            watching.getOrPut(vsPath) { mutableListOf() }.add(watcher)
            watching.getOrPut(fsPath) { mutableListOf() }.add(watcher)
            watchThread
            return watcher
        }
    }
    val shader: Shader?
        get() {
            if (changed) {
                try {
                    currentShader = Shader.createFromUrls(vsUrlString, fsUrlString)
                } catch (exception: Throwable) {
                    exception.printStackTrace()
                }
                changed = false
            }
            return currentShader
        }
}