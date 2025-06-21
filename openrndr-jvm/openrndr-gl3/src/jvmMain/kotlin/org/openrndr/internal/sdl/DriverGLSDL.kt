package org.openrndr.internal.sdl

import org.lwjgl.sdl.SDLVideo.SDL_GL_GetCurrentContext
import org.openrndr.draw.Session
import org.openrndr.internal.ResourceThread
import org.openrndr.internal.gl3.DriverGL3
import org.openrndr.internal.gl3.DriverVersionGL

class DriverGLSDL(versionGL: DriverVersionGL) : DriverGL3(versionGL) {
    override val contextID: Long
        get() {
            return SDL_GL_GetCurrentContext()

        }

    override fun createResourceThread(session: Session?, f: () -> Unit): ResourceThread {
        return ResourceThreadSDL.create(f)
    }

}